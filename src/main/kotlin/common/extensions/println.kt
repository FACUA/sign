package common.extensions

fun <T> T.println(): T {
	println(this)
	return this
}