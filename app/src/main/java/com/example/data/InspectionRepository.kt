package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.example.util.DocxReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InspectionRepository(
    private val dao: ReportDao,
    private val context: Context
) {
    val allReports: Flow<List<ReportEntity>> = dao.getAllReports()

    fun getReportFlow(reportId: String): Flow<ReportEntity?> = dao.getReportByIdFlow(reportId)

    suspend fun getReport(reportId: String): ReportEntity? = dao.getReportById(reportId)

    fun getIssuesForReport(reportId: String): Flow<List<IssueEntity>> = dao.getIssuesForReport(reportId)

    fun getIssueFlow(issueId: String): Flow<IssueEntity?> = dao.getIssueByIdFlow(issueId)

    suspend fun getIssue(issueId: String): IssueEntity? = dao.getIssueById(issueId)

    fun getPhotosForIssue(issueId: String): Flow<List<PhotoEntity>> = dao.getPhotosForIssue(issueId)

    fun getPhotosForReport(reportId: String): Flow<List<PhotoEntity>> = dao.getPhotosForReport(reportId)

    fun getPhotoFlow(photoId: String): Flow<PhotoEntity?> = dao.getPhotoByIdFlow(photoId)

    /**
     * Create a new Report with prefilled jobReference 'BBC-[YY]-'
     */
    suspend fun createReport(
        jobReference: String,
        reportName: String,
        clientName: String,
        address: String,
        reportDate: String,
        status: String,
        inspectionDate: String
    ): ReportEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val report = ReportEntity(
            id = UUID.randomUUID().toString(),
            jobReference = jobReference.trim(),
            reportName = reportName.trim(),
            clientName = clientName.trim(),
            address = address.trim(),
            reportDate = reportDate.trim(),
            status = status,
            inspectionDate = inspectionDate.trim(),
            inspectionDateHistory = "[]",
            nextIssueNumber = 1,
            createdAt = now,
            updatedAt = now
        )
        dao.insertReport(report)
        report
    }

    /**
     * Updates report metadata and enforces append-only inspection date history.
     * "if inspectionDate changed, push the old value + timestamp into inspectionDateHistory before writing the new value."
     */
    suspend fun updateReport(
        reportId: String,
        jobReference: String,
        reportName: String,
        clientName: String,
        address: String,
        reportDate: String,
        status: String,
        newInspectionDate: String
    ): Unit = withContext(Dispatchers.IO) {
        val existing = dao.getReportById(reportId) ?: return@withContext
        val now = System.currentTimeMillis()

        var updatedHistoryJson = existing.inspectionDateHistory
        val trimmedNewDate = newInspectionDate.trim()
        val trimmedOldDate = existing.inspectionDate.trim()

        if (trimmedNewDate.isNotBlank() && trimmedNewDate != trimmedOldDate && trimmedOldDate.isNotBlank()) {
            val historyList = HistoryJsonConverter.fromJson(existing.inspectionDateHistory).toMutableList()
            historyList.add(
                InspectionDateHistoryEntry(
                    date = trimmedOldDate,
                    changedAt = now
                )
            )
            updatedHistoryJson = HistoryJsonConverter.toJson(historyList)
        }

        val updated = existing.copy(
            jobReference = jobReference.trim(),
            reportName = reportName.trim(),
            clientName = clientName.trim(),
            address = address.trim(),
            reportDate = reportDate.trim(),
            status = status,
            inspectionDate = trimmedNewDate,
            inspectionDateHistory = updatedHistoryJson,
            updatedAt = now
        )
        dao.updateReport(updated)
    }

    /**
     * Sequential issue numbering: assign issueNumber once per issue at creation,
     * never reuse or shift on delete (leaves gaps e.g. Issue 1, Issue 3).
     */
    suspend fun addIssue(
        reportId: String,
        issueName: String,
        issueDescription: String
    ): IssueEntity = withContext(Dispatchers.IO) {
        val report = dao.getReportById(reportId)
            ?: throw IllegalStateException("Parent report not found")

        val assignedNumber = report.nextIssueNumber
        val newIssue = IssueEntity(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            issueNumber = assignedNumber,
            issueName = issueName.trim(),
            issueDescription = issueDescription.trim(),
            createdAt = System.currentTimeMillis()
        )

        dao.insertIssue(newIssue)
        dao.updateReport(
            report.copy(
                nextIssueNumber = assignedNumber + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
        newIssue
    }

    suspend fun updateIssue(
        issueId: String,
        issueName: String,
        issueDescription: String
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getIssueById(issueId) ?: return@withContext
        dao.updateIssue(
            existing.copy(
                issueName = issueName.trim(),
                issueDescription = issueDescription.trim()
            )
        )
    }

    /**
     * Multi-image reliable upload/storage with progress callback per image.
     * Denormalizes issueNumber and jobReference from parent chain.
     */
    suspend fun saveMultipleGalleryImages(
        reportId: String,
        issueId: String,
        uris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val report = dao.getReportById(reportId) ?: return@withContext emptyList()
        val issue = dao.getIssueById(issueId) ?: return@withContext emptyList()

        val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
        val createdPhotos = mutableListOf<PhotoEntity>()
        val total = uris.size

        for ((index, uri) in uris.withIndex()) {
            try {
                val photoId = UUID.randomUUID().toString()
                val targetFile = File(photosDir, "img_${photoId}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val photo = PhotoEntity(
                    id = photoId,
                    issueId = issueId,
                    reportId = reportId,
                    imagePath = targetFile.absolutePath,
                    description = "",
                    isReferenceImage = false,
                    issueNumber = issue.issueNumber,
                    jobReference = report.jobReference,
                    createdAt = System.currentTimeMillis() + index
                )
                dao.insertPhoto(photo)
                createdPhotos.add(photo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onProgress(index + 1, total)
        }

        dao.updateReport(report.copy(updatedAt = System.currentTimeMillis()))
        createdPhotos
    }

    /**
     * In-app camera photo capture saving with denormalized fields.
     */
    suspend fun saveCapturedPhoto(
        reportId: String,
        issueId: String,
        tempFile: File,
        description: String = "",
        isReferenceImage: Boolean = false
    ): PhotoEntity = withContext(Dispatchers.IO) {
        val report = dao.getReportById(reportId)
            ?: throw IllegalStateException("Parent report not found")
        val issue = dao.getIssueById(issueId)
            ?: throw IllegalStateException("Parent issue not found")

        val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
        val photoId = UUID.randomUUID().toString()
        val targetFile = File(photosDir, "img_${photoId}.jpg")

        if (tempFile.exists()) {
            tempFile.copyTo(targetFile, overwrite = true)
        }

        val photo = PhotoEntity(
            id = photoId,
            issueId = issueId,
            reportId = reportId,
            imagePath = targetFile.absolutePath,
            description = description.trim(),
            isReferenceImage = isReferenceImage,
            issueNumber = issue.issueNumber,
            jobReference = report.jobReference,
            createdAt = System.currentTimeMillis()
        )
        dao.insertPhoto(photo)
        dao.updateReport(report.copy(updatedAt = System.currentTimeMillis()))
        photo
    }

    suspend fun updatePhoto(
        photoId: String,
        description: String,
        isReferenceImage: Boolean
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getPhotoById(photoId) ?: return@withContext
        dao.updatePhoto(
            existing.copy(
                description = description.trim(),
                isReferenceImage = isReferenceImage
            )
        )
    }

    // Universal delete methods (all invoked after confirmation dialog)
    suspend fun deleteReport(reportId: String) = withContext(Dispatchers.IO) {
        dao.deleteReportById(reportId)
    }

    suspend fun deleteIssue(issueId: String) = withContext(Dispatchers.IO) {
        dao.deleteIssueById(issueId)
    }

    suspend fun deletePhoto(photoId: String) = withContext(Dispatchers.IO) {
        val photo = dao.getPhotoById(photoId)
        if (photo != null) {
            try {
                val f = File(photo.imagePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                // ignore
            }
            dao.deletePhotoById(photoId)
        }
    }

    /**
     * Exports the entire report including metadata, issues, and photos into a .docx document.
     */
    suspend fun exportReportDocx(reportId: String): File? = withContext(Dispatchers.IO) {
        val report = dao.getReportById(reportId) ?: return@withContext null
        val issues = dao.getIssuesForReportSync(reportId)
        val photos = dao.getPhotosForReportSync(reportId)
        DocxReportExporter.generateDocx(context, report, issues, photos)
    }

    /**
     * Seeds initial inspection data for Nic on first launch if the database is empty.
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllReports()
        // Check synchronously
        val report = dao.getReportById("sample-report-1")
        if (report != null) return@withContext

        val now = System.currentTimeMillis()
        val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

        // Create sample placeholder images with high-contrast inspection labels
        val sample1File = File(photosDir, "sample_facade_crack.jpg")
        val sample2File = File(photosDir, "sample_window_seal.jpg")
        val sample3File = File(photosDir, "sample_substation.jpg")

        createPlaceholderInspectionImage(sample1File, "BBC-26-90 | Issue 1", "External Masonry Stress Fracture", Color.rgb(30, 41, 59))
        createPlaceholderInspectionImage(sample2File, "BBC-26-90 | Issue 2", "Level 3 Sealant Weathering", Color.rgb(51, 65, 85))
        createPlaceholderInspectionImage(sample3File, "BBC-26-88 | Issue 1", "Substation Grounding Pad", Color.rgb(15, 23, 42))

        val r1History = listOf(
            InspectionDateHistoryEntry(date = "2026-08-28", changedAt = now - 86400000L * 12)
        )

        val report1 = ReportEntity(
            id = "sample-report-1",
            jobReference = "BBC-26-90",
            reportName = "Commercial Facade Assessment",
            clientName = "Skyline Holdings Ltd",
            address = "742 High Street, District 4",
            reportDate = "2026-09-08",
            status = ReportStatus.IN_PROGRESS.label,
            inspectionDate = "2026-09-10",
            inspectionDateHistory = HistoryJsonConverter.toJson(r1History),
            nextIssueNumber = 3,
            createdAt = now - 86400000L * 3,
            updatedAt = now
        )
        dao.insertReport(report1)

        val issue1 = IssueEntity(
            id = "sample-issue-1",
            reportId = report1.id,
            issueNumber = 1,
            issueName = "Structural Masonry Cracks",
            issueDescription = "Vertical hairline cracking observed extending from second-floor lintel to roofline. Possible thermal expansion.",
            createdAt = now - 86400000L * 3
        )
        val issue2 = IssueEntity(
            id = "sample-issue-2",
            reportId = report1.id,
            issueNumber = 2,
            issueName = "Sealant Degradation on Level 3 Windows",
            issueDescription = "Silicone perimeter bead severely weathered on south-facing facade. Water ingress risk under driving rain.",
            createdAt = now - 86400000L * 2
        )
        dao.insertIssue(issue1)
        dao.insertIssue(issue2)

        val photo1 = PhotoEntity(
            id = "sample-photo-1",
            issueId = issue1.id,
            reportId = report1.id,
            imagePath = sample1File.absolutePath,
            description = "Close up of stepped crack along mortar line above window head.",
            isReferenceImage = true,
            issueNumber = 1,
            jobReference = "BBC-26-90",
            createdAt = now - 86400000L * 3
        )
        val photo2 = PhotoEntity(
            id = "sample-photo-2",
            issueId = issue2.id,
            reportId = report1.id,
            imagePath = sample2File.absolutePath,
            description = "Perimeter seal separation along south elevation frame.",
            isReferenceImage = false,
            issueNumber = 2,
            jobReference = "BBC-26-90",
            createdAt = now - 86400000L * 2
        )
        dao.insertPhoto(photo1)
        dao.insertPhoto(photo2)

        val report2 = ReportEntity(
            id = "sample-report-2",
            jobReference = "BBC-26-88",
            reportName = "Substation Foundation Audit",
            clientName = "Northern Power Grid",
            address = "Unit 12 Grid Sector North",
            reportDate = "2026-09-02",
            status = ReportStatus.AWAITING_REVIEW.label,
            inspectionDate = "2026-09-04",
            inspectionDateHistory = "[]",
            nextIssueNumber = 2,
            createdAt = now - 86400000L * 6,
            updatedAt = now - 86400000L * 2
        )
        dao.insertReport(report2)

        val issue3 = IssueEntity(
            id = "sample-issue-3",
            reportId = report2.id,
            issueNumber = 1,
            issueName = "Grounding Strap Corrosion",
            issueDescription = "Bimetallic oxidation noted at copper-to-steel junction on earth pit.",
            createdAt = now - 86400000L * 5
        )
        dao.insertIssue(issue3)

        val photo3 = PhotoEntity(
            id = "sample-photo-3",
            issueId = issue3.id,
            reportId = report2.id,
            imagePath = sample3File.absolutePath,
            description = "Earth tape bonding lug showing surface oxidation.",
            isReferenceImage = true,
            issueNumber = 1,
            jobReference = "BBC-26-88",
            createdAt = now - 86400000L * 5
        )
        dao.insertPhoto(photo3)
    }

    private fun createPlaceholderInspectionImage(
        file: File,
        badge: String,
        title: String,
        bgColor: Int
    ) {
        try {
            val width = 800
            val height = 600
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(bgColor)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                isFakeBoldText = true
            }

            // Grid lines to look like survey/inspection capture
            val gridPaint = Paint().apply {
                color = Color.argb(40, 255, 255, 255)
                strokeWidth = 2f
            }
            for (x in 0 until width step 100) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
            }
            for (y in 0 until height step 100) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
            }

            // Badge box
            val badgePaint = Paint().apply {
                color = Color.rgb(217, 119, 6) // Safety Amber
            }
            canvas.drawRoundRect(40f, 40f, 380f, 100f, 12f, 12f, badgePaint)
            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                isFakeBoldText = true
            }
            canvas.drawText(badge, 60f, 82f, badgeTextPaint)

            // Title
            canvas.drawText(title, 40f, 320f, paint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                textSize = 24f
            }
            canvas.drawText("Assured Capture • Field Inspection Photo Record", 40f, 370f, subPaint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
