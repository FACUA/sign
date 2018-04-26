package ui.view

import javafx.geometry.Pos
import javafx.scene.control.ProgressBar
import tornadofx.*

class SigningProgressView() : Fragment() {
	override val root = vbox {
		alignment = Pos.CENTER

		style {
			padding = box(50.px)
		}

		label("Firmando... Por favor, espera.") {
			style {
				fontSize = 14.px
			}
		}

		progressbar(ProgressBar.INDETERMINATE_PROGRESS)
	}
}