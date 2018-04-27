package i18n

import java.util.*

object I18n {
	private val bundle = ResourceBundle.getBundle("i18n")

	val ui = Ui(bundle)
	val pdf = Pdf(bundle)
}
