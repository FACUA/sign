package pdf

import core.model.AcaSmartCard
import core.model.DniE
import core.model.SmartCard
import i18n.I18n

fun getSignatureText(card: SmartCard) = when (card) {
	is DniE -> "${I18n.pdf.signatureAppearance["signed-by"]}\n" +
		"${card.lastName}, ${card.firstName}\n" +
		"${I18n.pdf.signatureAppearance["with-dni"]} ${card.dniNumber}"
	is AcaSmartCard -> "${I18n.pdf.signatureAppearance["signed-by"]}\n" +
		"${card.lastName}, ${card.firstName}\n" +
		"${I18n.pdf.signatureAppearance["with-dni"]} ${card.dniNumber}\n" +
		"${I18n.pdf.signatureAppearance["with-collegiate-number"]} " +
		card.collegiateNumber
	else -> I18n.pdf.signatureAppearance["signed"]
}