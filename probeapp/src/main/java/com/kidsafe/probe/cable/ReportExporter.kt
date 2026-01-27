package com.kidsafe.probe.cable

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {
    fun writeTextReport(context: Context, fileBaseName: String, content: String): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "${sanitizeFileName(fileBaseName)}.txt")
        file.writeBytes(content.toByteArray(StandardCharsets.UTF_8))
        return file
    }

    fun writePdfReport(context: Context, fileBaseName: String, content: String): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "${sanitizeFileName(fileBaseName)}.pdf")
        val doc = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 18f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }

        val lines = content.replace("\r\n", "\n").split('\n')
        val wrapped = buildList {
            lines.forEach { line ->
                addAll(wrapLine(line, paint, pageWidth - margin * 2))
            }
        }

        var pageIndex = 1
        var y = margin
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        var canvas = page.canvas

        wrapped.forEach { line ->
            if (y + lineHeight > pageHeight - margin) {
                doc.finishPage(page)
                pageIndex += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
                canvas = page.canvas
                y = margin
            }
            canvas.drawText(line, margin, y, paint)
            y += lineHeight
        }

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }

    fun shareFileIntent(context: Context, file: File, mimeType: String, chooserTitle: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, chooserTitle) }
    }

    fun defaultFileBaseName(prefix: String): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "${prefix}_${sdf.format(Date())}"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[^\w\u4e00-\u9fa5\-_\.]+"""), "_").take(80)
    }

    private fun wrapLine(line: String, paint: Paint, maxWidthPx: Float): List<String> {
        if (line.isEmpty()) return listOf("")
        if (paint.measureText(line) <= maxWidthPx) return listOf(line)

        val out = mutableListOf<String>()
        var start = 0
        while (start < line.length) {
            var end = line.length
            while (end > start) {
                val slice = line.substring(start, end)
                if (paint.measureText(slice) <= maxWidthPx) {
                    out.add(slice)
                    start = end
                    break
                }
                end -= 1
            }
            if (end == start) {
                out.add(line.substring(start, start + 1))
                start += 1
            }
        }
        return out
    }
}

