package common.extensions

import java.util.Optional

val <T> Optional<T>.value get(): T? = this.orElse(null)
val <T> T?.optional get() = Optional.ofNullable(this)