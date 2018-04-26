package ui.controller

import common.util.async
import com.github.thomasnield.rxkotlinfx.doOnNextFx
import com.github.thomasnield.rxkotlinfx.observeOnFx
import com.github.thomasnield.rxkotlinfx.toBinding
import com.github.thomasnield.rxkotlinfx.toObservable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.stage.StageStyle
import pdf.getPdfImages
import pdf.signPdf
import tornadofx.Controller
import tornadofx.action
import ui.util.extensions.split
import ui.util.signatureCompleteDialog
import ui.util.subscribeWithErrorHandler
import ui.view.SignPdfView
import ui.view.SigningProgressView

class SignPdfController : Controller() {
	private lateinit var v: SignPdfView
	private lateinit var imageStreamSubscription: Disposable

	/*
	 * These coordinates represent the position of the top left corner of
	 * the signature, relative to the PDF preview image. The starting
	 * position is on the bottom right corner.
	 */
	private val signatureRelativeX = SimpleDoubleProperty(0.60)
	private val signatureRelativeY = SimpleDoubleProperty(0.85)

	fun init(v: SignPdfView) {
		this.v = v

		setupBindings()
		setupEventListeners()
	}

	fun dispose() {
		if (!imageStreamSubscription.isDisposed) {
			imageStreamSubscription.dispose()
		}
	}

	private fun setupBindings() {
		val imageStream = getPdfImages(v.pdf)

		// Add the images to the image list as they come.
		// They will be automatically added to the thumbnail panel as it
		imageStreamSubscription = imageStream
			.doOnNextFx {
				v.images.add(it)
			}
			.subscribeWithErrorHandler(
				"No se ha podido leer el PDF."
			)

		// When the first image comes, select the first item on the list.
		imageStream
			.take(1)
			.observeOnFx()
			.subscribe {
				v.thumbnailsPane
					.selectionModel
					.selectFirst()
			}

		// When another item of the list is selected, change the image.
		v.preview.imageProperty()
			.bind(
				v.thumbnailsPane
					.selectionModel
					.selectedItemProperty()
			)

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
	}

	private fun setupEventListeners() {
		v.previewPane
			.setOnMouseClicked { handlePreviewClicked(it) }
		v.signButton
			.action { handleSign() }
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
			val page = v.thumbnailsPane.selectionModel.selectedIndex + 1
			v.smartCard.signPdf(
				v.pdf,
				v.pin,
				v.signatureReasonField.text,
				v.signatureLocationField.text,
				page,
				signatureRelativeX.value to
					signatureRelativeY.value
			)
		}
			.doOnNext { dispose() }
			.doOnNextFx {
				v.onSignedCallback()

				modal.close()
				v.close()

				signatureCompleteDialog()
			}
			.subscribeWithErrorHandler(
				"No se ha podido firmar el archivo"
			)
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