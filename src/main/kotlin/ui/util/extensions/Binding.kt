package ui.util.extensions

import com.github.thomasnield.rxkotlinfx.toBinding
import com.github.thomasnield.rxkotlinfx.toObservable
import javafx.beans.binding.Binding

fun Binding<Boolean>.not() = this.toObservable().map { !it }.toBinding()
