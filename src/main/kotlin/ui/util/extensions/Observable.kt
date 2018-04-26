package ui.util.extensions

import io.reactivex.Observable

fun <T1, T2> Observable<Pair<T1, T2>>.split():
	Pair<Observable<T1>, Observable<T2>> {
	val first = this.map { it.first }
	val second = this.map { it.second }

	return first to second
}
