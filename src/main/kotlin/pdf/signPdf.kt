package pdf

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.signatures.*
import common.util.getNewTmpFile
import core.model.DigestAlgorithm
import core.model.EncryptionAlgorithm
import core.model.SmartCard
import ui.SIGNATURE_HEIGHT_MM
import ui.SIGNATURE_WIDTH_MM
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * Digitally signs a PDF, and adds a visual representation of the signature to
 * it.
 *
 * @param file The file to sign. Must be a valid PDF. It will be overwritten
 * with the signed document.
 * @param pin The pin of the SmartCard.
 * @param signatureReason The signature "reason" metadata. May be empty.
 * @param signatureLocation The signature "location" metadata. May be empty.
 * @param signaturePage The page where to add the visual representation of the
 * signature.
 * @param signatureRelativePosition The position of top left corner of the
 * visual representation of the signature, relative to the PDF size. For the X
 * axis, 0 being the left border, and 1 being the right border.
 */
fun SmartCard.signPdf(
	file: File,
	pin: String,
	signatureReason: String,
	signatureLocation: String,
	signaturePage: Int,
	signatureRelativePosition: Pair<Double, Double>
) {
	/*
	 * In this function we need to perform two read operations:
	 *
	 * - One will read the contents of the PDF in order to calculate its digest
	 * and produce the signature.
	 * - Another one will read the contents of the PDF to retrieve the page
	 * width and height, in order to determine the placement of the signature
	 * visual representation.
	 *
	 * Unfortunately, performing both operations within the same PdfReader
	 * produces some obscure iText error when signing:
	 *
	 * "There is no associate PdfWriter for making indirects".
	 *
	 * In order to work around this, the PDF file is copied first to a temporary
	 * file (readTmp), and then read. It is used to calculate the width and
	 * height of the PDF, then removed.
	 *
	 * The second read file (reader) is just the original file. This read
	 * operation is used to calculate the digest of the PDF.
	 *
	 * The writeTmp file is yet another temporary file (distinct from readTmp),
	 * which is where the signed PDF will be written to. This is because reading
	 * and writing to the same location isn't supported by iText apparently.
	 *
	 * At the end of the function, the writeTmp contents are written to the
	 * original file, provided the signature operation was successful, and then
	 * writeTmp is deleted.
	 */
	val readTmp = getNewTmpFile()
	val writeTmp = getNewTmpFile()

	file.copyTo(readTmp)

	val reader = PdfReader(file.absolutePath)
	val pdf = PdfDocument(PdfReader(readTmp))
	val destination = FileOutputStream(writeTmp)

	val signer = PdfSigner(reader, destination, false)

	val appearance = signer.signatureAppearance
	appearance.image = ImageDataFactory.create(
		ClassLoader
			.getSystemResourceAsStream("signature_logo.png")
			.readBytes()
	)
	appearance.layer2Text = getSignatureText(card = this)
	appearance.reason = signatureReason
	appearance.location = signatureLocation
	appearance.pageRect = calculateRectangle(pdf, signatureRelativePosition)
	appearance.pageNumber = signaturePage
	appearance.setReuseAppearance(false)

	signer.fieldName = "Firma"

	signer.signDetached(
		BouncyCastleDigest(),
		object : IExternalSignature {
			override fun getHashAlgorithm() =
				when(this@signPdf.digestAlgorithm) {
					DigestAlgorithm.SHA_1 -> "SHA-1"
					DigestAlgorithm.SHA_256 -> "SHA-256"
					DigestAlgorithm.SHA_512 -> "SHA-512"
				}
			override fun getEncryptionAlgorithm() =
				when(this@signPdf.encryptionAlgorithm) {
					EncryptionAlgorithm.RSA -> "RSA"
				}
			override fun sign(message: ByteArray) =
				this@signPdf.signBytes(message, pin)
					?: throw Exception("Error when signing")
		},
		arrayOf(
			this.publicCert,
			*(this.caCert?.let { arrayOf(it) } ?: emptyArray())
		),
		emptyList<ICrlClient>(),
		OcspClientBouncyCastle(null),
		null,
		0,
		PdfSigner.CryptoStandard.CMS
	)

	writeTmp.copyTo(file, overwrite = true)

	pdf.close()

	// Run in another thread so we don't make the user wait for these
	thread {
		readTmp.delete()
		writeTmp.delete()
	}
}

internal fun calculateRectangle(
	pdf: PdfDocument,
	signatureRelativePosition: Pair<Double, Double>
): Rectangle {
	val maxX = pdf.defaultPageSize.width
	val maxY = pdf.defaultPageSize.height

	val (relativeX, relativeY) = signatureRelativePosition

	val x = maxX * relativeX
	// The Y is inverted (i.e Y = 0 means bottom, Y = maxY means top)
	val y = maxY - (maxY * relativeY)

	val width = SIGNATURE_WIDTH_MM * 3
	val height = SIGNATURE_HEIGHT_MM * 3

	return Rectangle(
		x.toFloat(),
		// We add the height because iText expects the XY coordinates to
		// represent the bottom left corner of the signature, but so far we
		// have been using the top left corner instead.
		y.toFloat() - height.toFloat(),
		width.toFloat(),
		height.toFloat()
	)
}