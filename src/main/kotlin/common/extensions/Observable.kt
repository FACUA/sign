package common.extensions

import io.reactivex.Observable
import java.util.Optional

fun <T> Observable<Optional<T>>.presentValues(): Observable<T> = this
	.flatMap {
		if (it.isPresent) {
			Observable.just(it.value)
		} else {
			Observable.empty()
		}
	}