package core.util.extensions

fun CharArray.toUTF8() = String(this)
	.toByteArray(Charsets.ISO_8859_1)
	.toString(Charsets.UTF_8)
