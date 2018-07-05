package common.util

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlin.concurrent.thread

/**
 * Runs a code block in a separate thread.
 *
 * @param body The code block to run asynchronously.
 * @return An observable that emits the return value of the code block, or any
 * error that it might throw. After the code block returns or throws, the
 * observable completes.
 */
fun <T> async(
	body: () -> T
): Observable<T> {
	val subject = PublishSubject.create<T>()

	thread {
		try {
			subject.onNext(body())
		} catch (e: Throwable) {
			subject.onError(e)
		}

		subject.onComplete()
	}

	return subject.share()
}