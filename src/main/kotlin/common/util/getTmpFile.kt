package common.util

import java.io.File
import java.util.*

fun getNewTmpFile() = File(
	"${System.getProperty("java.io.tmpdir")}/" +
		"facua-sign-${UUID.randomUUID()}"
)