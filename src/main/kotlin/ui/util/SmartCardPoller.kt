package ui.util

import common.extensions.optional
import core.model.SmartCard
import core.util.SmartCards
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.Optional
import kotlin.concurrent.thread

/**
 * SmartCardPoller provides a way to subscribe to new smart cards
 * being added, or existing ones removed. It will broadcast the
 * current smart cards every second.
 */
object SmartCardPoller {
	private val subject =
		PublishSubject.create<Optional<SmartCard>>()
	private var isActive = false

	private fun poll() {
		try {
			subject.onNext(
				SmartCards
					.get()
					.take(1)
					.blockingIterable()
					.firstOrNull()
					.optional
			)
		} catch (e: Exception) {
			subject.onError(e)
		}

		if (isActive) {
			Thread.sleep(500L)
			poll()
		}
	}

	val stream get():
		Observable<Optional<SmartCard>> = subject.share()

	/**
	 * Pauses the SmartCardPoller. The stream will still be open, and the poller
	 * may be resumed at any time.
	 */
	fun pause() {
		isActive = false
	}

	/**
	 * Resumes a paused SmartCardPoller.
	 */
	fun resume() {
		if (!isActive) {
			isActive = true
			thread(name = "SmartCardPoller") {
				poll()
			}
		}
	}

	/**
	 * Stops the SmartCardPoller. It may not be restarted again.
	 */
	fun stop() {
		isActive = false
		subject.onComplete()
	}

	init {
		resume()
	}
}