package core.util

import core.model.DigestAlgorithm
import core.model.EncryptionAlgorithm
import core.model.SmartCardObject
import sun.security.pkcs11.wrapper.CK_ATTRIBUTE
import sun.security.pkcs11.wrapper.CK_MECHANISM
import sun.security.pkcs11.wrapper.PKCS11
import sun.security.pkcs11.wrapper.PKCS11Constants
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class SmartCardOperator(private val pkcs11: PKCS11, private val cardSlot: Long) {
	fun getObjectHandles(
		flags: Array<CK_ATTRIBUTE>,
		pin: String? = null
	): List<SmartCardObject> = session(pin = pin) { handle ->
		pkcs11.C_FindObjectsInit(handle, flags)

		val objs = pkcs11.C_FindObjects(handle, 1000)
		pkcs11.C_FindObjectsFinal(handle)

		objs
			.map {
				SmartCardObject(
					it,
					getObjectLabel(handle, it)
				)
			}
	}

	fun getCert(
		scObject: SmartCardObject
	): X509Certificate = session { sessionHandle ->
		val attrs = arrayOf(CK_ATTRIBUTE(PKCS11Constants.CKA_VALUE))
		pkcs11.C_GetAttributeValue(sessionHandle, scObject.handle, attrs)

		CertificateFactory
			.getInstance("X.509")
			.generateCertificate(
				ByteArrayInputStream(attrs[0].byteArray)
			) as X509Certificate
	}

	fun sign(
		scObject: SmartCardObject,
		bytes: ByteArray,
		digestAlgorithm: DigestAlgorithm,
		encryptionAlgorithm: EncryptionAlgorithm,
		pin: String
	): ByteArray = session(pin = pin) {
		pkcs11.C_SignInit(
			it,
			CK_MECHANISM(
				when (encryptionAlgorithm) {
					EncryptionAlgorithm.RSA -> when (digestAlgorithm) {
						DigestAlgorithm.SHA_1 ->
							PKCS11Constants.CKM_SHA1_RSA_PKCS
						DigestAlgorithm.SHA_256 ->
							PKCS11Constants.CKM_SHA256_RSA_PKCS
						DigestAlgorithm.SHA_512 ->
							PKCS11Constants.CKM_SHA512_RSA_PKCS
					}
				}
			),
			scObject.handle
		)

		pkcs11.C_Sign(it, bytes)
	}

	private fun getObjectLabel(sessionHandle: Long, objectHandle: Long): String? {
		val attrs = arrayOf(
			CK_ATTRIBUTE(PKCS11Constants.CKA_LABEL)
		)

		pkcs11.C_GetAttributeValue(
			sessionHandle,
			objectHandle,
			attrs
		)

		return try {
			attrs[0].charArray?.let { String(it) }
		} catch (e: RuntimeException) {
			null
		}
	}

	private fun <T> session(
		flags: List<Long> = emptyList(),
		pin: String? = null,
		body: (handle: Long) -> T
	): T {
		val handle = pkcs11.C_OpenSession(
			cardSlot,
			listOf(
				// For legacy reasons, the CKF_SERIAL_SESSION bit MUST always be set
				// Source: http://docs.oasis-open.org/pkcs11/pkcs11-base/v2.40/os/pkcs11-base-v2.40-os.html#_Toc72656119
				PKCS11Constants.CKF_SERIAL_SESSION,
				*flags.toTypedArray()
			)
				.reduce { a, b -> (a or b) },
			null,
			null
		)

		pin?.let {
			pkcs11.C_Login(handle, PKCS11Constants.CKU_USER, pin.toCharArray())
		}

		val result = body(handle)

		pin?.let {
			pkcs11.C_Logout(handle)
		}

		pkcs11.C_CloseSession(handle)

		return result
	}
}