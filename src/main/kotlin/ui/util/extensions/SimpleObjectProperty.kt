package ui.util.extensions

import com.github.thomasnield.rxkotlinfx.toBinding
import com.github.thomasnield.rxkotlinfx.toObservable
import javafx.beans.property.SimpleObjectProperty
import java.util.Optional

fun <T> SimpleObjectProperty<Optional<T>>.present() = this
	.toObservable()
	.map { it.isPresent }
	.toBinding()