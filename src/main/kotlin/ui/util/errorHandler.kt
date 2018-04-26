package ui.util

import com.github.thomasnield.rxkotlinfx.doOnErrorFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

fun <T> Observable<T>.subscribeWithErrorHandler(
	dialogMessage: String? = null
): Disposable = this
	.doOnErrorFx {
		tornadofx.error(
			"Ha ocurrido un error",
			dialogMessage
		) {
			System.exit(1)
		}
	}
	.subscribe({}, Throwable::printStackTrace)
