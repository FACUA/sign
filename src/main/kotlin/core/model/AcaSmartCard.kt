package core.model

import core.util.SmartCardOperator
import java.security.cert.X509Certificate

class AcaSmartCard(operator: SmartCardOperator) : SmartCard(operator) {
	override val digestAlgorithm = DigestAlgorithm.SHA_512
	override val encryptionAlgorithm = EncryptionAlgorithm.RSA

	override val publicCertRegex = """EMAILADDRESS=.+, CN=NOMBRE .+ - NIF ([0-9]{8}[A-Z]), OU=.+ / ([0-9]+), O=.+, C=ES, ST=.+, T=.+, SERIALNUMBER=.+, GIVENNAME=(.+), SURNAME=(.+), OID.+""".toRegex()
	override val signingCertLabel = "DS User Private Key 3"
	override fun publicCertIdentifier(cert: X509Certificate) =
		publicCertRegex.matches(cert.subjectDN.name)
	// Las tarjetas ACA TS 2048 JS no tienen el certificado CA intermedio
	override fun caCertIdentifier(cert: X509Certificate) =
		cert.subjectDN.name.contains("Consejo General de la Abogacia NIF:Q-2863006I")

	val firstName get() = personalData[3]
	val lastName get() = personalData[4]
	val dniNumber get() = personalData[1]
	val colegiateNumber get() = personalData[2]

	override fun toString() = "ACA: $firstName $lastName (Colegiado Nº $colegiateNumber)"
}