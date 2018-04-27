package i18n

import java.nio.charset.Charset
import java.util.*

abstract class BundleAccessor(
	private val prefix: String,
	private val bundle: ResourceBundle
) {
	private fun str(key: String): String = String(
		bundle
			.getString("$prefix.$key")
			.toByteArray(Charset.forName("ISO-8859-1")),
		Charset.forName("UTF-8")
	)
	protected fun params(key: String, vararg params: String) = String
		.format(str(key), *params)
	operator fun get(key: String) = str(key)
}