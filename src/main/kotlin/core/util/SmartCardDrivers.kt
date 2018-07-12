package core.util

import java.io.File

object SmartCardDrivers {
	private val rootDir = File("/usr/local/facua-sign")
	private val driversDir = File("${rootDir.absolutePath}/drivers")

	private val ldLibraryPaths get() = driversDir
		.listFiles()
		.flatMap {
			val lib = File("${it.absolutePath}/usr/lib")

			if (lib.exists()) {
				listOf(lib.absolutePath)
			} else {
				emptyList()
			}
		}

	// The following list contains the location, relative to the drivers
	// folder, of all drivers supported by Facua Sign, in the order they should
	// be tested (the app will try the first, and if it doesn't find a Smart
	// Card, it will try the next one).
	// The first driver should be the most common one, or the fastest one.
	val installedDrivers get() = listOf(
		"bit4id/usr/lib/libbit4ipki.so",
		"opensc/usr/lib/opensc-pkcs11.so"
	)
		.map { File("${driversDir.absolutePath}/$it") }
		.filter { it.exists() }
		.map { it.absolutePath }

	init {
		if (!driversDir.exists()) {
			throw Exception("Can't find /usr/local/facua-sign/drivers!")
		}

		val currentPaths = System.getenv("LD_LIBRARY_PATH")
			.split(":")

		if (!currentPaths.containsAll(ldLibraryPaths)) {
			throw Exception(
				"The LD_LIBRARY_PATH is not correct! it should contain: " +
				ldLibraryPaths.joinToString(":")
			)
		}
	}
}