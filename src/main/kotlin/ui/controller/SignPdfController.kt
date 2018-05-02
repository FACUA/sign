package ui.controller

import common.util.async
import com.github.thomasnield.rxkotlinfx.doOnNextFx
import com.github.thomasnield.rxkotlinfx.toBinding
import com.github.thomasnield.rxkotlinfx.toObservable
import common.extensions.optional
import common.extensions.presentValues
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.stage.StageStyle
import pdf.PDFPageExtractor
import pdf.signPdf
import tornadofx.Controller
import tornadofx.action
import ui.util.extensions.not
import ui.util.extensions.or
import ui.util.extensions.split
import ui.util.signatureCompleteDialog
import ui.util.subscribeWithErrorHandler
import ui.view.SignPdfView
import ui.view.SigningProgressView
import java.util.concurrent.TimeUnit

class SignPdfController : Controller() {
	private lateinit var v: SignPdfView
	private lateinit var extractor: PDFPageExtractor

	/*
	 * These coordinates represent the position of the top left corner of
	 * the signature, relative to the PDF preview image. The starting
	 * position is on the bottom right corner.
	 */
	private val signatureRelativeX = SimpleDoubleProperty(0.60)
	private val signatureRelativeY = SimpleDoubleProperty(0.85)

	fun init(v: SignPdfView) {
		this.v = v

		extractor = PDFPageExtractor(v.pdf)

		setupBindings()
		setupEventListeners()
		setupDefaultValues()
	}

	private fun setupBindings() {
		val currentPageTextStream = v.navigationCurrentPageTextField
			.textProperty()
			.toObservable()

		val currentPageStream = currentPageTextStream
			.flatMap {
				it.toIntOrNull()
					?.let { Observable.just(it) }
					?: Observable.empty()
			}

		//<editor-fold desc="Preview bindings" defaultstate="collapsed">

		val actualPageStream = currentPageStream
			// If the user clicks too quickly, this results in too much
			// parallel processing and the JVM might crash. To solve this,
			// we only render the page when the user "calms down".
			.debounce(500, TimeUnit.MILLISECONDS)

		v.preview.imageProperty()
			.bind(
				actualPageStream
					.flatMap {
						async { extractor.getPageAsImage(it - 1).optional }
					}
					.presentValues()
					.toBinding()
			)

		//</editor-fold>

		//<editor-fold desc="Navigator bindings" defaultstate="collapsed">

		val currentPageFieldValid = currentPageTextStream
			.map {
				it.toIntOrNull()
					?.let {
						it >= 1 && it <= extractor.pageCount
					}
					?: false
			}
			.toBinding()

		val currentPageIsFirst = currentPageStream
			.map { it <= 1 }
			.toBinding()

		val currentPageIsLast = currentPageStream
			.map { it >= extractor.pageCount }
			.toBinding()

		// Whenever an invalid value is entered on the current page field,
		// mark it red.
		v.navigationCurrentPageTextField
			.styleProperty()
			.bind(
				currentPageFieldValid
					.not()
					.toObservable()
					.map {
						"-fx-control-inner-background: ${
							if (it) {
								"#D32F2F"
							} else {
								"white"
							}
						};"
					}
					.toBinding()
			)

		// Disable the "Next page" button when the value of the current page is
		// invalid, or when we're on the last page.
		v.navigationNextPageButton.disableProperty()
			.bind(
				currentPageFieldValid.not()
					.or(currentPageIsLast)
			)

		// Disable the "Previous page" button when the value of the current page
		// is invalid, or when we're on the first page.
		v.navigationPreviousPageButton.disableProperty()
			.bind(
				currentPageFieldValid.not()
					.or(currentPageIsFirst)
			)

		// Disable the "First page" button when we're already on the first page.
		v.navigationFirstPageButton.disableProperty()
			.bind(currentPageIsFirst)

		// Disable the "Last page" button when we're already on the last page.
		v.navigationLastPageButton.disableProperty()
			.bind(currentPageIsLast)

		//</editor-fold>

		//<editor-fold desc="Signature preview bindings"
		//  defaultstate="collapsed">

		// When the relative position of the signature is modified,  modify it
		// on the signature image and the signature text too
		val (absoluteXStream, absoluteYStream) = Observable.combineLatest<
			Double,
			Double,
			Pair<Double, Double>
		>(
			signatureRelativeX.toObservable().map { it as Double },
			signatureRelativeY.toObservable().map { it as Double },
			BiFunction { x, y ->
				signaturePositionsRelativeToAbsolute(x to y)
			}
		)
			.split()

		fun bindToRelativePosition(node: Node) {
			node.layoutXProperty().bind(absoluteXStream.toBinding())
			node.layoutYProperty().bind(absoluteYStream.toBinding())
		}

		bindToRelativePosition(v.signatureImagePreview)
		bindToRelativePosition(v.signatureTextPreview)

		//</editor-fold>
	}

	private fun setupEventListeners() {
		v.previewPane
			.setOnMouseClicked { handlePreviewClicked(it) }
		v.navigationNextPageButton
			.setOnMouseClicked { handleNextPageClicked() }
		v.navigationPreviousPageButton
			.setOnMouseClicked { handlePreviousPageClicked() }
		v.navigationFirstPageButton
			.setOnMouseClicked { handleFirstPageClicked() }
		v.navigationLastPageButton
			.setOnMouseClicked { handleLastPageClicked() }
		v.signButton
			.action { handleSign() }
	}

	private fun setupDefaultValues() {
		// Simulate a lick on "First page"
		handleFirstPageClicked()
	}

	private fun handleNextPageClicked() {
		val currentPage = v.navigationCurrentPageTextField
			.textProperty()
			.get()
			.toIntOrNull() ?: return

		if (currentPage >= extractor.pageCount) {
			return
		}

		v.navigationCurrentPageTextField
			.textProperty()
			.set("${currentPage + 1}")
	}

	private fun handlePreviousPageClicked() {
		val currentPage = v.navigationCurrentPageTextField
			.textProperty()
			.get()
			.toIntOrNull() ?: return

		if (currentPage <= 0) {
			return
		}

		v.navigationCurrentPageTextField
			.textProperty()
			.set("${currentPage - 1}")
	}

	private fun handleFirstPageClicked() {
		v.navigationCurrentPageTextField.textProperty().set("1")
	}

	private fun handleLastPageClicked() {
		v.navigationCurrentPageTextField.textProperty()
			.set("${extractor.pageCount}")
	}

	private fun handlePreviewClicked(event: MouseEvent) {
		val (x, y) = signaturePositionsAbsoluteToRelative(
			event.x to event.y
		)

		signatureRelativeX.set(x)
		signatureRelativeY.set(y)
	}

	private fun handleSign() {
		val modal = find<SigningProgressView>()
			.openModal(StageStyle.UNDECORATED) ?: return

		async {
			v.smartCard.signPdf(
				v.pdf,
				v.pin,
				v.signatureReasonField.text,
				v.signatureLocationField.text,
				v.navigationCurrentPageTextField.text.toInt(),
				signatureRelativeX.value to
					signatureRelativeY.value
			)
		}
			.doOnNextFx {
				v.onSignedCallback()

				modal.close()
				v.close()

				signatureCompleteDialog()
			}
			.subscribeWithErrorHandler("could-not-sign")
	}

	/**
	 * Transforms a pair of coordinates representing the absolute position of
	 * the signature on the screen, to a pair of coordinates representing the
	 * position relative to the PDF size.
	 *
	 * @param positions The absolute positions; doubles ranging from 0 to the
	 *                  width/height of the PDF image preview.
	 * @return The relative positions; doubles ranging from 0 (the signature
	 * is a the very top for X, and at the very left for Y) to 1 (the
	 * signature is at the very bottom for X, and at the very right for Y).
	 */
	private fun signaturePositionsAbsoluteToRelative(
		positions: Pair<Double, Double>
	): Pair<Double, Double> {
		val (absoluteX, absoluteY) = positions

		val relativeX = absoluteX / v.preview.fitWidth
		val relativeY = absoluteY / v.preview.fitHeight

		return relativeX to relativeY
	}

	/**
	 * Transforms a pair of coordinates representing the position relative to
	 * the PDF size, to a pair of coordinates representing the absolute
	 * position of the signature on the screen.
	 *
	 * @param positions The relative positions; doubles ranging from 0 (the
	 *                  signature is a the very top for X, and at the very
	 *                  left for Y) to 1 (the signature is at the very
	 *                  bottom for X, and at the very right for Y).
	 * @return The absolute positions; doubles ranging from 0 to the
	 * width/height of the PDF image preview.
	 */
	private fun signaturePositionsRelativeToAbsolute(
		positions: Pair<Double, Double>
	): Pair<Double, Double> {
		val (relativeX, relativeY) = positions

		val absoluteX = relativeX * v.preview.fitWidth
		val absoluteY = relativeY * v.preview.fitHeight

		return absoluteX to absoluteY
	}
}