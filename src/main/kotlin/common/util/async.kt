package common.util

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlin.concurrent.thread

fun <T> async(
	body: () -> T
): Observable<T> {
	val subject = PublishSubject.create<T>()

	thread {
		try {
			subject.onNext(body())
		} catch (e: Exception) {
			subject.onError(e)
		}

		subject.onComplete()
	}

	return subject.share()
}