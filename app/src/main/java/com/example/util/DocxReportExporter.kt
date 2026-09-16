package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.HistoryJsonConverter
import com.example.data.IssueEntity
import com.example.data.PhotoEntity
import com.example.data.ReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxReportExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /**
     * Generates a fully compliant Microsoft Word (.docx) document containing
     * the complete report, metadata, issues, audit history, and embedded inspection photos.
     */
    suspend fun generateDocx(
        context: Context,
        report: ReportEntity,
        issues: List<IssueEntity>,
        photos: List<PhotoEntity>
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val safeJobRef = report.jobReference.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "Report_${safeJobRef}_${System.currentTimeMillis()}.docx"
        val docxFile = File(exportsDir, fileName)

        // Map existing photos to internal media entries
        val mediaMap = mutableListOf<Triple<String, File, LongPair>>()
        var mediaIndex = 1
        for (photo in photos) {
            val imgFile = File(photo.imagePath)
            if (imgFile.exists() && imgFile.canRead() && imgFile.length() > 0) {
                val dimensions = getImageDimensionsEmu(imgFile)
                val mediaTarget = "image_${mediaIndex}.jpg"
                mediaMap.add(Triple(mediaTarget, imgFile, dimensions))
                mediaIndex++
            }
        }

        // Build XML contents
        val contentTypesXml = buildContentTypesXml(mediaMap.isNotEmpty())
        val packageRelsXml = buildPackageRelsXml()
        val documentRelsXml = buildDocumentRelsXml(mediaMap)
        val stylesXml = buildStylesXml()
        val documentXml = buildDocumentXml(report, issues, photos, mediaMap)

        // Write as Zip package
        ZipOutputStream(FileOutputStream(docxFile)).use { zos ->
            // 1. [Content_Types].xml
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            // 2. _rels/.rels
            writeZipEntry(zos, "_rels/.rels", packageRelsXml)

            // 3. word/_rels/document.xml.rels
            writeZipEntry(zos, "word/_rels/document.xml.rels", documentRelsXml)

            // 4. word/styles.xml
            writeZipEntry(zos, "word/styles.xml", stylesXml)

            // 5. word/document.xml
            writeZipEntry(zos, "word/document.xml", documentXml)

            // 6. word/media/image_*.jpg
            for (item in mediaMap) {
                val entryPath = "word/media/${item.first}"
                zos.putNextEntry(ZipEntry(entryPath))
                FileInputStream(item.second).use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        docxFile
    }

    /**
     * Saves the exported docx file to the device's public Downloads directory.
     */
    fun saveToDownloads(context: Context, sourceFile: File, displayName: String): Uri? {
        val mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, displayName)
                sourceFile.copyTo(targetFile, overwrite = true)
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens or shares the exported docx via an Android intent.
     */
    fun shareDocx(context: Context, file: File, title: String = "Share Inspection Report") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        zos.putNextEntry(ZipEntry(entryName))
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun buildContentTypesXml(hasImages: Boolean): String {
        val imageTypes = if (hasImages) {
            """  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Default Extension="png" ContentType="image/png"/>"""
        } else ""

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
$imageTypes
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
    }

    private fun buildPackageRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun buildDocumentRelsXml(mediaMap: List<Triple<String, File, LongPair>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
""")
        for ((index, item) in mediaMap.withIndex()) {
            val relId = "rIdImg${index + 1}"
            val target = "media/${item.first}"
            sb.append("""  <Relationship Id="$relId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="$target"/>
""")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="22"/>
        <w:color w:val="1E293B"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:qFormat/>
    <w:pPr>
      <w:spacing w:before="360" w:after="160"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:sz w:val="36"/>
      <w:color w:val="0F172A"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:qFormat/>
    <w:pPr>
      <w:spacing w:before="240" w:after="120"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:sz w:val="28"/>
      <w:color w:val="16A34A"/>
    </w:rPr>
  </w:style>
</w:styles>"""
    }

    private fun buildDocumentXml(
        report: ReportEntity,
        issues: List<IssueEntity>,
        photos: List<PhotoEntity>,
        mediaMap: List<Triple<String, File, LongPair>>
    ): String {
        // Map photo ID or file to its media index
        val photoRelIdMap = mutableMapOf<String, Pair<String, LongPair>>()
        for ((index, item) in mediaMap.withIndex()) {
            val relId = "rIdImg${index + 1}"
            photoRelIdMap[item.second.absolutePath] = Pair(relId, item.third)
        }

        val photosByIssue = photos.groupBy { it.issueId }
        val auditHistory = HistoryJsonConverter.fromJson(report.inspectionDateHistory)
        val exportTimestamp = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
""")

        // Header Title
        sb.append(paragraph("ASSURED CAPTURE • FIELD INSPECTION REPORT", bold = true, sizePt = 11, colorHex = "16A34A", spaceBefore = 100, spaceAfter = 60))
        sb.append(paragraph(report.reportName.ifBlank { "Inspection Report" }, bold = true, sizePt = 24, colorHex = "0F172A", spaceBefore = 60, spaceAfter = 120))
        sb.append(paragraph("Job Reference: ${report.jobReference}", bold = true, sizePt = 16, colorHex = "D97706", spaceBefore = 0, spaceAfter = 240))

        // Metadata Table
        sb.append("""    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="9200" w:type="dxa"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:left w:val="none"/>
          <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:right w:val="none"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:insideV w:val="none"/>
        </w:tblBorders>
      </w:tblPr>
""")
        sb.append(tableRow("Job Reference", report.jobReference))
        sb.append(tableRow("Report Title", report.reportName.ifBlank { "Untitled" }))
        sb.append(tableRow("Client Name", report.clientName.ifBlank { "Unassigned" }))
        sb.append(tableRow("Site Address", report.address.ifBlank { "Not specified" }))
        sb.append(tableRow("Report Date", report.reportDate.ifBlank { "Not set" }))
        sb.append(tableRow("Inspection Date", report.inspectionDate.ifBlank { "Not set" }))
        sb.append(tableRow("Status", report.status))
        sb.append(tableRow("Total Issues", "${issues.size} recorded"))
        sb.append(tableRow("Total Photos", "${photos.size} attached"))
        sb.append("    </w:tbl>\n")

        // Inspection Date Audit History (if any)
        if (auditHistory.isNotEmpty()) {
            sb.append(paragraph("Inspection Date Audit History", bold = true, sizePt = 14, colorHex = "0F172A", spaceBefore = 280, spaceAfter = 80))
            sb.append(paragraph("Previous inspection dates recorded in append-only system:", italic = true, sizePt = 10, colorHex = "64748B", spaceBefore = 0, spaceAfter = 80))
            for (entry in auditHistory) {
                val changedDateStr = dateFormat.format(Date(entry.changedAt))
                sb.append(bulletItem("Previous Date: ${entry.date} (Replaced on $changedDateStr)"))
            }
        }

        // Divider
        sb.append(paragraph("", spaceBefore = 180, spaceAfter = 180))

        // Issues Section
        sb.append(paragraph("ISSUES & INSPECTION FINDINGS (${issues.size})", bold = true, sizePt = 18, colorHex = "0F172A", spaceBefore = 300, spaceAfter = 160))

        if (issues.isEmpty()) {
            sb.append(paragraph("No issues recorded for this inspection report.", italic = true, sizePt = 11, colorHex = "64748B", spaceBefore = 60, spaceAfter = 120))
        } else {
            for (issue in issues) {
                // Issue Header Box / Title
                sb.append(paragraph("Issue ${issue.issueNumber}: ${issue.issueName.ifBlank { "Untitled Issue" }}", bold = true, sizePt = 14, colorHex = "1E3A8A", spaceBefore = 240, spaceAfter = 60))
                val issueDate = dateFormat.format(Date(issue.createdAt))
                sb.append(paragraph("Logged: $issueDate", sizePt = 9, colorHex = "64748B", spaceBefore = 0, spaceAfter = 60))

                // Issue Description
                val desc = if (issue.issueDescription.isNotBlank()) issue.issueDescription else "No description provided."
                sb.append(paragraph(desc, sizePt = 11, colorHex = "334155", spaceBefore = 40, spaceAfter = 120))

                // Photos for this issue
                val issuePhotos = photosByIssue[issue.id] ?: emptyList()
                if (issuePhotos.isNotEmpty()) {
                    sb.append(paragraph("Photographic Evidence (${issuePhotos.size})", bold = true, sizePt = 11, colorHex = "475569", spaceBefore = 80, spaceAfter = 60))

                    for ((pIdx, photo) in issuePhotos.withIndex()) {
                        val relInfo = photoRelIdMap[photo.imagePath]

                        val refLabel = if (photo.isReferenceImage) "[REFERENCE IMAGE] " else ""
                        val pTitle = "${refLabel}Photo ${pIdx + 1}: ${photo.jobReference} | Issue #${photo.issueNumber}"
                        sb.append(paragraph(pTitle, bold = true, sizePt = 10, colorHex = if (photo.isReferenceImage) "D97706" else "334155", spaceBefore = 100, spaceAfter = 40))

                        // Drawing element if image file exists
                        if (relInfo != null) {
                            val (relId, dim) = relInfo
                            sb.append(drawingImage(relId, dim.first, dim.second, "Photo ${pIdx + 1}"))
                        }

                        // Photo Caption
                        if (photo.description.isNotBlank()) {
                            sb.append(paragraph("Notes: ${photo.description}", italic = true, sizePt = 10, colorHex = "475569", spaceBefore = 40, spaceAfter = 80))
                        }
                    }
                } else {
                    sb.append(paragraph("No photos attached for this issue.", italic = true, sizePt = 10, colorHex = "94A3B8", spaceBefore = 40, spaceAfter = 80))
                }

                sb.append(paragraph("", spaceBefore = 80, spaceAfter = 80))
            }
        }

        // Footer / Verification Block
        sb.append(paragraph("", spaceBefore = 240, spaceAfter = 120))
        sb.append(paragraph("VERIFICATION & SIGN-OFF", bold = true, sizePt = 12, colorHex = "0F172A", spaceBefore = 180, spaceAfter = 60))
        sb.append(paragraph("Report generated by Assured Capture Field Inspection System", sizePt = 10, colorHex = "64748B", spaceBefore = 0, spaceAfter = 40))
        sb.append(paragraph("Export Date: $exportTimestamp", sizePt = 10, colorHex = "64748B", spaceBefore = 0, spaceAfter = 160))

        // Page Layout & Margins (1 inch margins = 1440 dxa)
        sb.append("""    <w:sectPr>
      <w:pgSz w:w="12240" w:h="15840"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>""")

        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun paragraph(
        text: String,
        bold: Boolean = false,
        italic: Boolean = false,
        sizePt: Int = 11,
        colorHex: String = "1E293B",
        spaceBefore: Int = 60,
        spaceAfter: Int = 60
    ): String {
        val bTag = if (bold) "<w:b/>" else ""
        val iTag = if (italic) "<w:i/>" else ""
        val szVal = sizePt * 2
        return """    <w:p>
      <w:pPr><w:spacing w:before="$spaceBefore" w:after="$spaceAfter"/></w:pPr>
      <w:r>
        <w:rPr>$bTag$iTag<w:sz w:val="$szVal"/><w:color w:val="$colorHex"/></w:rPr>
        <w:t xml:space="preserve">${escapeXml(text)}</w:t>
      </w:r>
    </w:p>
"""
    }

    private fun bulletItem(text: String): String {
        return """    <w:p>
      <w:pPr>
        <w:ind w:left="720"/>
        <w:spacing w:before="40" w:after="40"/>
      </w:pPr>
      <w:r>
        <w:rPr><w:sz w:val="20"/><w:color w:val="475569"/></w:rPr>
        <w:t xml:space="preserve">• ${escapeXml(text)}</w:t>
      </w:r>
    </w:p>
"""
    }

    private fun tableRow(label: String, value: String): String {
        return """      <w:tr>
        <w:tc>
          <w:tcPr>
            <w:tcW w:w="2800" w:type="dxa"/>
            <w:shd w:val="clear" w:color="auto" w:fill="F8FAFC"/>
          </w:tcPr>
          <w:p>
            <w:pPr><w:spacing w:before="80" w:after="80"/></w:pPr>
            <w:r>
              <w:rPr><w:b/><w:sz w:val="20"/><w:color w:val="475569"/></w:rPr>
              <w:t xml:space="preserve">${escapeXml(label)}</w:t>
            </w:r>
          </w:p>
        </w:tc>
        <w:tc>
          <w:tcPr>
            <w:tcW w:w="6400" w:type="dxa"/>
            <w:shd w:val="clear" w:color="auto" w:fill="FFFFFF"/>
          </w:tcPr>
          <w:p>
            <w:pPr><w:spacing w:before="80" w:after="80"/></w:pPr>
            <w:r>
              <w:rPr><w:sz w:val="20"/><w:color w:val="0F172A"/></w:rPr>
              <w:t xml:space="preserve">${escapeXml(value)}</w:t>
            </w:r>
          </w:p>
        </w:tc>
      </w:tr>
"""
    }

    private fun drawingImage(relId: String, cx: Long, cy: Long, desc: String): String {
        val uniqueId = (1000..9999).random()
        return """    <w:p>
      <w:pPr><w:spacing w:before="60" w:after="60"/></w:pPr>
      <w:r>
        <w:drawing>
          <wp:inline distT="0" distB="0" distL="0" distR="0">
            <wp:extent cx="$cx" cy="$cy"/>
            <wp:effectExtent l="0" t="0" r="0" b="0"/>
            <wp:docPr id="$uniqueId" name="${escapeXml(desc)}"/>
            <wp:cNvGraphicFramePr>
              <a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/>
            </wp:cNvGraphicFramePr>
            <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                  <pic:nvPicPr>
                    <pic:cNvPr id="$uniqueId" name="${escapeXml(desc)}"/>
                    <pic:cNvPicPr/>
                  </pic:nvPicPr>
                  <pic:blipFill>
                    <a:blip r:embed="$relId" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                    <a:stretch>
                      <a:fillRect/>
                    </a:stretch>
                  </pic:blipFill>
                  <pic:spPr>
                    <a:xfrm>
                      <a:off x="0" y="0"/>
                      <a:ext cx="$cx" cy="$cy"/>
                    </a:xfrm>
                    <a:prstGeom prst="rect">
                      <a:avLst/>
                    </a:prstGeom>
                  </pic:spPr>
                </pic:pic>
              </a:graphicData>
            </a:graphic>
          </wp:inline>
        </w:drawing>
      </w:r>
    </w:p>
"""
    }

    private fun getImageDimensionsEmu(imageFile: File): LongPair {
        val maxWidthEmu = 4572000L // ~5.0 inches
        val maxHeightEmu = 3429000L // ~3.75 inches
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, opts)
            val w = opts.outWidth.toLong()
            val h = opts.outHeight.toLong()
            if (w > 0 && h > 0) {
                if (w * maxHeightEmu > h * maxWidthEmu) {
                    val finalW = maxWidthEmu
                    val finalH = (h * maxWidthEmu) / w
                    LongPair(finalW, finalH)
                } else {
                    val finalH = maxHeightEmu
                    val finalW = (w * maxHeightEmu) / h
                    LongPair(finalW, finalH)
                }
            } else {
                LongPair(maxWidthEmu, maxHeightEmu)
            }
        } catch (e: Exception) {
            LongPair(maxWidthEmu, maxHeightEmu)
        }
    }
}

data class LongPair(val first: Long, val second: Long)
