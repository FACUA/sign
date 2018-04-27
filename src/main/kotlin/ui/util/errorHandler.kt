package ui.util

import com.github.thomasnield.rxkotlinfx.doOnErrorFx
import i18n.I18n
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

fun <T> Observable<T>.subscribeWithErrorHandler(
	dialogMessage: String? = null
): Disposable = this
	.doOnErrorFx {
		tornadofx.error(
			I18n.ui.error["generic"],
			dialogMessage?.let { I18n.ui.error[it] }
		) {
			System.exit(1)
		}
	}
	.subscribe({}, Throwable::printStackTrace)
