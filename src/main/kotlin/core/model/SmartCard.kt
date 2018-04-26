package core.model

import sun.security.pkcs11.wrapper.CK_ATTRIBUTE
import sun.security.pkcs11.wrapper.PKCS11Constants
import core.util.SmartCardOperator
import java.security.cert.X509Certificate

abstract class SmartCard(
	private val operator: SmartCardOperator
) {
	abstract val digestAlgorithm: DigestAlgorithm
	abstract val encryptionAlgorithm: EncryptionAlgorithm

	protected abstract val publicCertRegex: Regex
	protected abstract val signingCertLabel: String
	protected abstract fun publicCertIdentifier(cert: X509Certificate): Boolean
	protected abstract fun caCertIdentifier(cert: X509Certificate): Boolean

	private val certHandles = operator
		.getObjectHandles(
			arrayOf(
				CK_ATTRIBUTE(
					PKCS11Constants.CKA_CLASS,
					PKCS11Constants.CKC_X_509_ATTR_CERT
				)
			)
		)
	private val certs = certHandles
		.map { operator.getCert(it) }

	val publicCert get() = certs
		.find { publicCertIdentifier(it) }
		?: throw Exception("No se ha encontrado el certificado")

	val caCert get() = certs
		.find { caCertIdentifier(it) }

	protected val personalData get() = publicCertRegex
		.matchEntire(publicCert.subjectDN.name)
		?.groupValues
		?: throw Exception("No se reconoce el formato del certificado")

	fun signBytes(bytes: ByteArray, pin: String) = operator.getObjectHandles(
			arrayOf(
				CK_ATTRIBUTE(
					PKCS11Constants.CKA_CLASS,
					PKCS11Constants.CKO_PRIVATE_KEY
				)
			),
			pin
		)
		.find { it.label == signingCertLabel }
		?.let {
			operator.sign(
				it,
				bytes,
				digestAlgorithm,
				encryptionAlgorithm,
				pin
			)
		}
}
