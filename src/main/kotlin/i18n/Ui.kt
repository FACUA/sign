package i18n

import java.util.*

class Ui(bundle: ResourceBundle) : BundleAccessor("ui", bundle) {
	val mainView = object : BundleAccessor(
		"ui.main-view",
		bundle
	) {}
	val signPdfView = object : BundleAccessor(
		"ui.sign-pdf-view",
		bundle
	) {}
	val signingProgressView = object : BundleAccessor(
		"ui.signing-progress-view",
		bundle
	) {}
	val common = object : BundleAccessor(
		"ui.common",
		bundle
	) {}
	val error = object : BundleAccessor(
		"ui.error",
		bundle
	) {}
}