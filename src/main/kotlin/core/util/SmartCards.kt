package core.util

import common.extensions.optional
import common.extensions.presentValues
import common.util.async
import core.model.AcaSmartCard
import core.model.DniE
import core.model.SmartCard
import core.util.extensions.toUTF8
import io.reactivex.Observable
import sun.security.pkcs11.wrapper.PKCS11

object SmartCards {
	private val pkcs11libs = SmartCardDrivers
		.installedDrivers
		.map { PKCS11.getInstance(it, "C_GetFunctionList", null, false) }

	/**
	 * Looks for smart cards using all installed drivers, in order, until
	 * it finds one supported card. These operations cannot be processed
	 * in parallel, as attempting two PKCS#11 operations at once leads to
	 * problems.
	 */
	private fun findCard(): SmartCard? {
		for (pkcs11 in pkcs11libs) {
			val slots = pkcs11.C_GetSlotList(true)

			// If we performed the next operation immediately, it might fail
			Thread.sleep(200)

			if (slots.isEmpty()) {
				break
			}

			for (slot in slots) {
				try {
					val info = pkcs11.C_GetTokenInfo(slot)

					val smartCard = buildSmartCard(
						pkcs11,
						slot,
						info.label.toUTF8().trim()
					)

					if (smartCard != null) {
						return smartCard
					}
				} catch (e: Exception) {
					// The exception can most likely be safely ignored
				}
			}
		}

		return null
	}

	/**
	 * @return An Observable that emits the first present Smart Card as soon
	 * as it is detected.
	 */
	fun get(): Observable<SmartCard> = async {
		findCard().optional
	}
		.presentValues()

	private fun buildSmartCard(
		pkcs11: PKCS11,
		slot: Long,
		label: String
	): SmartCard? {
		val op = SmartCardOperator(pkcs11, slot)
		return when (label) {
			"DNI electrónico" -> DniE(op)
			"DNI electrónico (PIN1)" -> DniE(op)
			"PIN1 (DNI electrónico)" -> DniE(op)
			"DS Crypto Smart Card" -> AcaSmartCard(op)
			"DSD" -> AcaSmartCard(op)
			else -> null
		}
	}
}