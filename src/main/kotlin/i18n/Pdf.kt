package i18n

import java.util.*

class Pdf(bundle: ResourceBundle) : BundleAccessor("pdf", bundle) {
	fun signatureField(id: String) = params("signature-field", id)

	val signatureAppearance = object : BundleAccessor(
		"pdf.signature-appearance",
		bundle
	) {}
}