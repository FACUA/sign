package ui.view

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Pane
import tornadofx.*
import ui.controller.MainController
import ui.util.SmartCardPoller

class MainView : View("Facua Sign") {
	private val c: MainController by inject()

	lateinit var smartCardLabel: Label
	lateinit var fileLabel: Label
	lateinit var searchingSmartCardsBox: Pane
	lateinit var smartCardFoundBox: Pane
	lateinit var pinField: PasswordField
	lateinit var selectFileButton: Button
	lateinit var signButton: Button

	override val root = borderpane {
		left = form {
			alignment = Pos.CENTER

			fieldset("Archivo a firmar") {
				hbox {
					hbox {
						style {
							paddingRight = 10
						}
						alignment = Pos.CENTER
						fileLabel = label()
					}

					selectFileButton = button("Seleccionar")
				}
			}

			fieldset("Tarjeta inteligente") {
				searchingSmartCardsBox = hbox {
					label("Buscando tarjetas...") {
						style {
							paddingRight = 10
						}
					}
					progressbar(ProgressBar.INDETERMINATE_PROGRESS)
				}

				smartCardFoundBox = vbox {
					style {
						paddingBottom = 4
					}

					smartCardLabel = label()
				}
			}

			fieldset("Autenticación") {
				field("PIN de la tarjeta") {
					pinField = passwordfield()
				}
			}
		}

		right = borderpane {
			style {
				padding = box(10.px)
			}

			center = button("Firmar") {
				signButton = this
			}

			bottom = imageview(resources.image("/logo.png")) {
				fitWidth = 114.0
				fitHeight = 40.0
			}
		}
	}

	init {
		with(primaryStage) {
			width = 600.0
			height = 250.0
			isResizable = false

			icons.add(resources.image("/icon.png"))

			setOnCloseRequest { SmartCardPoller.stop() }
		}

		c.init(this)
	}
}
