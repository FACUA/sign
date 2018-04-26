package pdf

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.ghost4j.document.PDFDocument
import org.ghost4j.renderer.SimpleRenderer
import java.awt.image.BufferedImage
import java.io.File
import kotlin.concurrent.thread

/**
 * Renders a PDF file, page by page, retrieving their corresponding images.
 *
 * @param file The PDF file to render
 * @return An observable that emits the images as they are processed. If the
 * observable is disposed, the rendering will stop, even if not all images have
 * been rendered.
 */
fun getPdfImages(file: File): Observable<Image> {
	val subject = PublishSubject.create<Image>()

	var isThreadCancelled = false

	thread {
		val pdf = PDFDocument()
		pdf.load(file)

		val renderer = SimpleRenderer()
		renderer.resolution = 300 // DPI

		// Instead of rendering all pages together, we render them one by one,
		// so we have the first pages as soon as possible
		(0 until pdf.pageCount).forEach {
			if (isThreadCancelled) {
				return@thread
			}

			try {
				val awtImage = renderer.render(pdf, it, it).firstOrNull()

				if (awtImage is BufferedImage) {
					val javaFxImage = SwingFXUtils.toFXImage(
						awtImage,
						null
					)
					subject.onNext(javaFxImage)
				}
			} catch (e: Exception) {
				subject.onError(e)
			}
		}

		subject.onComplete()
	}

	return subject
		.doOnDispose { isThreadCancelled = true }
		.share()
}