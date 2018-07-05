package ui.controller

import common.util.async
import com.github.thomasnield.rxkotlinfx.*
import common.extensions.optional
import common.extensions.value
import core.model.SmartCard
import i18n.I18n
import io.reactivex.Observable
import io.reactivex.functions.Function4
import io.reactivex.subjects.PublishSubject
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.DragEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.TransferMode
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
	private val str = I18n.ui.mainView

	private lateinit var v: MainView

	private val smartCard = SimpleObjectProperty<Optional<SmartCard>>()
	private val file = SimpleObjectProperty<Optional<File>>(Optional.empty())
	// Will be true when the user clicks "Sign", while the application is
	// validating the PIN.
	private val backgroundWorking = SimpleObjectProperty<Boolean>(false)

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

		// Make the file label display the selected file name
		v.fileLabel.bind(
			file
				.toObservable()
				.map { it.value?.name ?: str["no-file-selected"] }
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
		// - The application is not working in the background
		v.signButton.disableProperty().bind(
			Observable.combineLatest<
				Boolean,
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
				backgroundWorking
					.toObservable()
					.map { !it },
				Function4 { a, b, c, d -> a && b && c && d }
			)
				.toBinding()
				.not()
		)

		// Make the sign button display "Working" when the application is
		// working in the background, or "Sign" otherwise.
		v.signButton.textProperty().bind(
			backgroundWorking
				.toObservable()
				.map { str[if (it) "working" else "sign"] }
		)
	}

	private fun setupEventHandlers() {
		v.selectFileButton.action { handleSelectFile() }
		v.signButton.action { handleSign() }
		v.pinField
			.events(KeyEvent.KEY_PRESSED)
			.filter { it.code == KeyCode.ENTER }
			.subscribe { handleSign() }
		v.root.setOnDragOver { handleFileDragOver(it) }
		v.root.setOnDragDropped { handleFileDragDropped(it) }
	}

	private fun handleSelectFile() {
		chooseFile(str["choose-file"], emptyArray())
			.firstOrNull()
			?.let { file.set(it.optional) }
	}

	private fun handleFileDragOver(event: DragEvent) {
		event.acceptTransferModes(TransferMode.MOVE)
		event.consume()
	}

	private fun handleFileDragDropped(event: DragEvent) {
		val db = event.dragboard

		val (draggedFile, isEventSuccessful) = if (db.hasFiles()) {
			db.files.firstOrNull() to true
		} else {
			null to false
		}

		file.set(draggedFile.optional)

		event.isDropCompleted = isEventSuccessful
		event.consume()
	}

	/**
	 * Checks that the entered PIN is correct, and launches a "Sign file" dialog
	 * corresponding to the file's content type. While the PIN is being tested,
	 * and the dialogs are open, the application is set to be working, which
	 * will prevent this method from being called again before finishing.
	 */
	private fun handleSign() {
		val pin = v.pinField.text
		val fileToSign = file.value.value
		val smartCard = smartCard.value.value

		if (pin.isEmpty() ||
			fileToSign == null ||
			smartCard == null) { return }

		setApplicationBackgroundWorking()

		async { smartCard.isPinValid(pin) }
			.doOnNextFx {
				if (!it) {
					tornadofx.error(
						I18n.ui.error["invalid-pin"]
					) {
						v.pinField.clear()
						setApplicationNotBackgroundWorking()
					}
				}
			}
			// Only continue if the PIN is correct
			.filter { it }
			.flatMap {
				async { Files.probeContentType(fileToSign.toPath()) }
			}
			.observeOnFx()
			.flatMap {
				when (it) {
					"application/pdf" -> handleSignPdf(
						fileToSign,
						smartCard,
						pin
					)
					else -> handleSignGenericFile(
						fileToSign,
						smartCard,
						pin
					)
				}
			}
			.doOnNextFx { setApplicationNotBackgroundWorking() }
			.doOnErrorFx { setApplicationNotBackgroundWorking() }
			.doOnNextFx {
				if (it) {
					// When the file is signed, remove so it is not
					// accidentally signed again
					file.set(Optional.empty())
				}
			}
			.subscribeWithErrorHandler()
	}

	/**
	 * Launches the "Sign PDF" dialog.
	 *
	 * @param pdf The PDF to sign
	 * @param smartCard The SmartCard which will sign the PDF
	 * @param pin The PIN of the SmartCard
	 *
	 * @return An observable that emits true once the PDF has been signed, or
	 * emits false if the signing operation is aborted.
	 */
	private fun handleSignPdf(
		pdf: File,
		smartCard: SmartCard,
		pin: String
	): Observable<Boolean> {
		val subject = PublishSubject.create<Boolean>()

		find<SignPdfView>(
			mapOf(
				SignPdfView::pdf to pdf,
				SignPdfView::smartCard to smartCard,
				SignPdfView::pin to pin,
				SignPdfView::onSignedCallback to {
					subject.onNext(true)
				},
				SignPdfView::onClosedCallback to {
					subject.onNext(false)
				}
			)
		)
			.openModal(modality = Modality.WINDOW_MODAL)

		return subject.share()
	}

	/**
	 * Launches the "Sign generic file" dialog. This dialog will let the user
	 * know that the signature will be stored in a separate file, since the
	 * signature can't be embedded in the file itself because the file format
	 * is unknown to the application.
	 *
	 * @param file The file to sign
	 * @param smartCard The SmartCard which will sign the PDF
	 * @param pin The PIN of the SmartCard
	 *
	 * @return An observable that emits true once the file has been signed, or
	 * emits false if the signing operation is aborted.
	 */
	private fun handleSignGenericFile(
		file: File,
		smartCard: SmartCard,
		pin: String
	): Observable<Boolean> {
		var observable: Observable<Boolean>? = null

		confirm(
			str["confirm-generic-file.title"],
			str["confirm-generic-file.text"]
		) {
			val signatureDestination = chooseFile(
				title = str["save-signature"],
				filters = arrayOf(
					FileChooser.ExtensionFilter(
						str["signature-file"],
						"*.sig"
					)
				),
				mode = FileChooserMode.Save
			)
				.singleOrNull() ?: return@confirm

			val modal = find<SigningProgressView>()
				.openModal(StageStyle.UNDECORATED) ?: return@confirm

			observable = async {
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
				.map { true }
		}

		return observable ?: Observable.just(false)
	}

	/**
	 * Changes the status of the application to "Background working", which will
	 * pause the Smart Card Poller, and disable the "Sign" button.
	 */
	private fun setApplicationBackgroundWorking() {
		SmartCardPoller.pause()
		backgroundWorking.set(true)
	}

	/**
	 * Changes the status of the application no "Not background working", which
	 * will resume the Smart Card Poller, and enable the "Sign" button.
	 */
	private fun setApplicationNotBackgroundWorking() {
		SmartCardPoller.resume()
		backgroundWorking.set(false)
	}
}