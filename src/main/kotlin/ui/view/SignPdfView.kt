package ui.view

import core.model.SmartCard
import i18n.I18n
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import pdf.getSignatureText
import tornadofx.*
import ui.*
import ui.controller.SignPdfController
import ui.util.SVGIcons
import ui.util.SmartCardPoller
import java.io.File

class SignPdfView : Fragment(I18n.ui.signPdfView["name"]) {
	private val c: SignPdfController by inject()
	private val str = I18n.ui.signPdfView

	val pdf: File by param()
	val pin: String by param()
	val smartCard: SmartCard by param()
	val onSignedCallback: () -> Unit by param()

	/*
	 * These coordinates represent the position of the top left corner of
	 * the signature, relative to the PDF preview image. The starting
	 * position is on the bottom right corner.
	 */
	val signatureRelativeX = SimpleDoubleProperty(0.60)
	val signatureRelativeY = SimpleDoubleProperty(0.85)

	val signatureLogos = mutableListOf(
		I18n.pdf.signatureAppearance["no-logo"]
	).observable()

	lateinit var preview: ImageView
	lateinit var previewPane: Pane
	lateinit var navigationCurrentPageTextField: TextField
	lateinit var navigationNextPageButton: Button
	lateinit var navigationPreviousPageButton: Button
	lateinit var navigationFirstPageButton: Button
	lateinit var navigationLastPageButton: Button
	lateinit var signatureImagePreview: ImageView
	lateinit var signatureTextPreview: Label
	lateinit var signButton: Button
	lateinit var signatureReasonField: TextField
	lateinit var signatureLocationField: TextField
	lateinit var signatureLogoComboBox: ComboBox<String>

	override val root = hbox {
		vbox {
			style {
				paddingLeft = 10
				paddingRight = 10
				paddingBottom = 10
			}

			label(str["preview-tip"]) {
				style {
					padding = box(10.px)
				}
			}

			scrollpane {
				previewPane = pane {
					cursor = Cursor.CROSSHAIR

					preview = imageview(null) {
						fitWidth = A4_WIDTH_MM * PDF_PREVIEW_SIZE_FACTOR
						fitHeight = A4_HEIGHT_MM * PDF_PREVIEW_SIZE_FACTOR
					}
					pane {
						signatureImagePreview = imageview {
							fitWidth =
								SIGNATURE_WIDTH_MM * PDF_PREVIEW_SIZE_FACTOR
							fitHeight =
								SIGNATURE_HEIGHT_MM * PDF_PREVIEW_SIZE_FACTOR
						}

						signatureTextPreview = label(
							getSignatureText(smartCard)
						) {
							style {
								fontSize = 11.px
							}
						}
					}
				}
			}

			hbox {
				style {
					paddingTop = 5
				}

				alignment = Pos.CENTER

				navigationFirstPageButton = button {
					svgpath(SVGIcons.firstPage)
				}
				navigationPreviousPageButton = button {
					svgpath(SVGIcons.previousPage)
				}
				navigationCurrentPageTextField = textfield {
					maxWidth = 50.0
				}
				navigationNextPageButton = button {
					svgpath(SVGIcons.nextPage)
				}
				navigationLastPageButton = button {
					svgpath(SVGIcons.lastPage)
				}
			}
		}

		vbox {
			form {
				fieldset(str["signature-options.name"]) {
					label(str["signature-options.optional-fields"])

					field(str["signature-options.reason"]) {
						signatureReasonField = textfield()
					}
					field(str["signature-options.location"]) {
						signatureLocationField = textfield()
					}
					field("Logo") {
						signatureLogoComboBox = combobox()
					}

					signButton = button(str["sign"])
				}
			}
		}
	}

	override fun onUndock() {
		super.onUndock()
		SmartCardPoller.resume()
	}

	init {
		c.init(this)
	}
}