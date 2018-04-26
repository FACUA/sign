package ui.controller

import common.util.async
import com.github.thomasnield.rxkotlinfx.*
import common.extensions.optional
import common.extensions.value
import core.model.SmartCard
import io.reactivex.Observable
import io.reactivex.functions.Function3
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import ui.util.SmartCardPoller
import ui.util.extensions.enableTrueHidden
import ui.util.extensions.not
import ui.util.extensions.present
import ui.util.signatureCompleteDialog
import ui.util.subscribeWithErrorHandler
import ui.view.SignPdfView
import ui.view.MainView
import ui.view.SigningProgressView
import java.io.File
import java.nio.file.Files
import java.util.Optional

class MainController : Controller() {
	private lateinit var v: MainView

	private val smartCard = SimpleObjectProperty<Optional<SmartCard>>()
	private val file = SimpleObjectProperty<Optional<File>>(Optional.empty())

	fun init(v: MainView) {
		this.v = v

		setupBindings()
		setupEventHandlers()
	}

	private fun setupBindings() {
		// Make the smart card have the last value of the polling
		smartCard.bind(
			SmartCardPoller
				.stream
				.map { it }
				.startWith(Optional.empty())
				.observeOnFx()
				.toBinding()
		)

		// Make the smart card label display the smart card value
		v.smartCardLabel.bind(
			smartCard
				.toObservable()
				.map { it.value?.toString() ?: "" }
				.toBinding()
		)

		v.fileLabel.bind(
			file
				.toObservable()
				.map {
					it.value?.name
						?: "Ningún archivo seleccionado"
				}
				.toBinding()
		)

		// Make the "searching smart card" box invisible when the
		// smart card is present
		v.searchingSmartCardsBox.hiddenWhen(smartCard.present())
		// Make the "smart card found" box invisible when the smart
		// card is present
		v.smartCardFoundBox.hiddenWhen(smartCard.present().not())

		v.searchingSmartCardsBox.enableTrueHidden()
		v.smartCardFoundBox.enableTrueHidden()

		// Make the pin field be enabled when the smart card is present
		v.pinField.disableProperty().bind(smartCard.present().not())

		// Make the sign button be only enabled when
		// - The smart card is present
		// - There is a pin in the pin field
		// - The file is present
		v.signButton.disableProperty().bind(
			Observable.combineLatest<
				Boolean,
				Boolean,
				Boolean,
				Boolean
			>(
				smartCard
					.present()
					.toObservable(),
				v.pinField
					.textProperty()
					.toObservable()
					.map { !it.isEmpty() },
				file
					.present()
					.toObservable(),
				Function3 { a, b, c -> a && b && c }
			)
				.toBinding()
				.not()
		)
	}

	private fun setupEventHandlers() {
		v.selectFileButton.action { handleSelectFile() }
		v.signButton.action { handleSign() }
		v.pinField
			.events(KeyEvent.KEY_PRESSED)
			.filter { it.code == KeyCode.ENTER }
			.subscribe { handleSign() }
	}

	private fun handleSelectFile() {
		chooseFile("Elige un archivo para firmar", emptyArray())
			.firstOrNull()
			?.let { file.set(it.optional) }
	}

	private fun handleSign() {
		val pin = v.pinField.text
		val file = file.value.value
		val smartCard = smartCard.value.value

		if (pin.isEmpty() ||
			file == null ||
			smartCard == null) { return }

		async { Files.probeContentType(file.toPath()) }
			.doOnNextFx {
				when (it) {
					"application/pdf" -> handleSignPdf(file, smartCard, pin)
					else -> handleSignGenericFile(file, smartCard, pin)
				}
			}
			.subscribeWithErrorHandler()
	}

	private fun handleSignPdf(pdf: File, smartCard: SmartCard, pin: String) {
		SmartCardPoller.pause()

		find<SignPdfView>(
			mapOf(
				SignPdfView::pdf to pdf,
				SignPdfView::smartCard to smartCard,
				SignPdfView::pin to pin,
				SignPdfView::onSignedCallback to {
					// When the file is signed, remove so it is not
					// accidentally signed again
					file.set(Optional.empty())
				}
			)
		)
			.openModal(modality = Modality.WINDOW_MODAL)
	}

	private fun handleSignGenericFile(
		file: File,
		smartCard: SmartCard,
		pin: String
	) {
		SmartCardPoller.pause()

		confirm(
			"Confirmar firma de archivo genérico",
			"""El archivo que vas a firmar no es un archivo PDF,
				|por lo que la aplicación lo firmará de forma
				|genérica, guardando la firma en un archivo
				|separado. ¿Continuar?
			""".trimMargin()
		) {
			val signatureDestination = chooseFile(
				title = "Guardar firma",
				filters = arrayOf(
					FileChooser.ExtensionFilter(
						"Archivo de firma",
						"*.sig"
					)
				),
				mode = FileChooserMode.Save
			)
				.singleOrNull() ?: return@confirm

			val modal = find<SigningProgressView>()
				.openModal(StageStyle.UNDECORATED) ?: return@confirm

			async {
				val signature = smartCard.signBytes(
					file.readBytes(),
					pin
				) ?: throw Exception("Error when signing")

				signatureDestination.writeBytes(signature)
			}
				.doOnNextFx {
					modal.close()
					signatureCompleteDialog()
				}
		}

		SmartCardPoller.resume()
	}
}