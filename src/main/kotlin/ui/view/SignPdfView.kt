package ui.view

import core.model.SmartCard
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Cursor
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import pdf.getSignatureText
import tornadofx.*
import ui.*
import ui.controller.SignPdfController
import ui.util.SmartCardPoller
import java.io.File

class SignPdfView : Fragment("Firmar PDF") {
	private val c: SignPdfController by inject()

	val pdf: File by param()
	val pin: String by param()
	val smartCard: SmartCard by param()
	val onSignedCallback: () -> Unit by param()

	val images: ObservableList<Image> = FXCollections
		.observableList<Image>(mutableListOf())

	lateinit var thumbnailsPane: DataGrid<Image>
	lateinit var preview: ImageView
	lateinit var previewPane: Pane
	lateinit var signatureImagePreview: ImageView
	lateinit var signatureTextPreview: Label
	lateinit var signButton: Button
	lateinit var signatureReasonField: TextField
	lateinit var signatureLocationField: TextField

	override val root = hbox {
		thumbnailsPane = datagrid(images) {
			maxCellsInRow = 1
			maxWidth = 185.0

			cellCache {
				imageview(it) {
					// 20% of full image
					fitWidth = 109.2
					fitHeight = 154.44
				}
			}
		}

		vbox {
			style {
				paddingLeft = 10
				paddingRight = 10
				paddingBottom = 10
			}

			label("Haz click en la previsualización para seleccionar dónde firmar el PDF") {
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
						signatureImagePreview = imageview(
							resources.image("/signature_logo.png")
						) {
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
		}

		vbox {
			form {
				fieldset("Opciones de firma") {
					label("Estos campos son opcionales")

					field("Motivo") {
						signatureReasonField = textfield()
					}
					field("Lugar") {
						signatureLocationField = textfield()
					}

					signButton = button("Firmar")
				}
			}
		}
	}

	override fun onUndock() {
		super.onUndock()
		c.dispose()
		SmartCardPoller.resume()
	}

	init {
		c.init(this)
	}
}