package ui.view

import i18n.I18n
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Pane
import tornadofx.*
import ui.controller.MainController
import ui.util.SmartCardPoller

class MainView : View(I18n.ui["app-name"]) {
	private val c: MainController by inject()
	private val str = I18n.ui.mainView

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

			fieldset(str["file-to-sign"]) {
				vbox {
					hbox {
						hbox {
							style {
								paddingRight = 10
							}
							alignment = Pos.CENTER
							fileLabel = label()
						}

						selectFileButton = button(str["select-file"])
					}
					label(str["select-or-drag-file"]) {
						style {
							paddingTop = 5
						}
					}
				}
			}

			fieldset(str["smart-card"]) {
				searchingSmartCardsBox = hbox {
					label(str["searching-cards"]) {
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

			fieldset(str["authentication"]) {
				field(str["card-pin"]) {
					pinField = passwordfield()
				}
			}
		}

		right = borderpane {
			style {
				padding = box(10.px)
			}

			center = button(str["sign"]) {
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
			height = 275.0
			isResizable = false

			icons.add(resources.image("/icon.png"))

			setOnCloseRequest { SmartCardPoller.stop() }
		}

		c.init(this)
	}
}
