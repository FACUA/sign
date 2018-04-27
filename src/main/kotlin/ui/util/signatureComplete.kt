package ui.util

import i18n.I18n
import tornadofx.information

fun signatureCompleteDialog() {
	information(
		I18n.ui.common["signature-complete.title"],
		I18n.ui.common["signature-complete.text"]
	)
}
