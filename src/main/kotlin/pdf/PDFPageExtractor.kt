package pdf

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.ghost4j.document.PDFDocument
import org.ghost4j.renderer.SimpleRenderer
import java.awt.image.BufferedImage
import java.io.File

class PDFPageExtractor(file: File) {
	private val pdf = PDFDocument()

	init {
		pdf.load(file)
	}
	
	val pageCount get() = pdf.pageCount

	/**
	 * Renders the page of a PDF file to a bitmap.
	 *
	 * @param page The index of the page to render
	 * @return the rendered Image object
	 */
	fun getPageAsImage(page: Int): Image? {
		val renderer = SimpleRenderer()
		renderer.resolution = 300 // DPI

		val awtImage = renderer.render(pdf, page, page).firstOrNull()

		return if (awtImage is BufferedImage) {
			SwingFXUtils.toFXImage(
				awtImage,
				null
			)
		} else {
			null
		}
	}
}