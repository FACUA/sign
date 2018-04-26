package core.model

import core.util.SmartCardOperator
import java.security.cert.X509Certificate

class DniE(operator: SmartCardOperator) : SmartCard(operator) {
	override val digestAlgorithm = DigestAlgorithm.SHA_1
	override val encryptionAlgorithm = EncryptionAlgorithm.RSA

	override val publicCertRegex = """CN=".+", GIVENNAME=(.+), SURNAME=(.+), SERIALNUMBER=([0-9]{8}[A-Z]), C=ES""".toRegex()
	override val signingCertLabel = "KprivFirmaDigital"
	override fun publicCertIdentifier(cert: X509Certificate) =
		cert.subjectDN.name.contains("(FIRMA)")
	override fun caCertIdentifier(cert: X509Certificate) =
		cert.subjectDN.name.contains("DIRECCION GENERAL DE LA POLICIA")

	val firstName get() = personalData[1]
	val lastName get() = personalData[2]
	val dniNumber get() = personalData[3]

	override fun toString() = "DNI: $firstName $lastName ($dniNumber)"
}
