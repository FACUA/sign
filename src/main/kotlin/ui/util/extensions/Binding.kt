package ui.util.extensions

import com.github.thomasnield.rxkotlinfx.toBinding
import com.github.thomasnield.rxkotlinfx.toObservable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javafx.beans.binding.Binding

fun Binding<Boolean>.not() = this.toObservable().map { !it }.toBinding()
fun Binding<Boolean>.and(aBinding: Binding<Boolean>) = combine(
	this,
	aBinding
) { a, b -> a && b }
fun Binding<Boolean>.or(aBinding: Binding<Boolean>) = combine(
	this,
	aBinding
) { a, b -> a || b }

internal fun combine(
	a: Binding<Boolean>,
	b: Binding<Boolean>,
	fn: (a: Boolean, b: Boolean) -> Boolean
): Binding<Boolean> = Observable
	.combineLatest<Boolean, Boolean, Boolean>(
		a.toObservable(),
		b.toObservable(),
		BiFunction { aValue, bValue -> fn(aValue, bValue) }
	)
	.toBinding()