package pdf

import core.model.AcaSmartCard
import core.model.DniE
import core.model.SmartCard

fun getSignatureText(card: SmartCard) = when (card) {
	is DniE -> "Firmado digitalmente por\n" +
		"${card.lastName}, ${card.firstName}\n" +
		"con DNI ${card.dniNumber}"
	is AcaSmartCard -> "Firmado digitalmente por\n" +
		"${card.lastName}, ${card.firstName}\n" +
		"con DNI ${card.dniNumber}\n" +
		"y nº colegiado ${card.colegiateNumber}"
	else -> "Firmado digitalmente"
}