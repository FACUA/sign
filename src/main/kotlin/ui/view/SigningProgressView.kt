package ui.view

import i18n.I18n
import javafx.geometry.Pos
import javafx.scene.control.ProgressBar
import tornadofx.*

class SigningProgressView : Fragment() {
	private val str = I18n.ui.signingProgressView

	override val root = vbox {
		alignment = Pos.CENTER

		style {
			padding = box(50.px)
		}

		label(str["signing"]) {
			style {
				fontSize = 14.px
			}
		}

		progressbar(ProgressBar.INDETERMINATE_PROGRESS)
	}
}