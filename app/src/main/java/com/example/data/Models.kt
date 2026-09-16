package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class ReportStatus(val label: String) {
    DRAFT("Draft"),
    IN_PROGRESS("In Progress"),
    AWAITING_REVIEW("Awaiting Review"),
    COMPLETED("Completed"),
    SENT_TO_CLIENT("Sent to Client");

    companion object {
        fun fromString(value: String): ReportStatus {
            return entries.find { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: DRAFT
        }
    }
}

data class InspectionDateHistoryEntry(
    val date: String,
    val changedAt: Long
)

object HistoryJsonConverter {
    fun fromJson(jsonStr: String): List<InspectionDateHistoryEntry> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<InspectionDateHistoryEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InspectionDateHistoryEntry(
                        date = obj.optString("date", ""),
                        changedAt = obj.optLong("changedAt", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(list: List<InspectionDateHistoryEntry>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("date", item.date)
            obj.put("changedAt", item.changedAt)
            array.put(obj)
        }
        return array.toString()
    }
}

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val jobReference: String, // e.g. BBC-26-90
    val reportName: String,
    val clientName: String,
    val address: String,
    val reportDate: String, // YYYY-MM-DD
    val status: String, // Draft / In Progress / Awaiting Review / Completed / Sent to Client
    val inspectionDate: String, // Current value, editable
    val inspectionDateHistory: String, // Append-only JSON array of {date, changedAt}
    val nextIssueNumber: Int = 1, // Sequential counter never reused or shifted
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "issues",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reportId")]
)
data class IssueEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val issueNumber: Int, // Auto-assigned sequentially per report ("Issue 1", "Issue 2"...) - never reused or shifted
    val issueName: String,
    val issueDescription: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = IssueEntity::class,
            parentColumns = ["id"],
            childColumns = ["issueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("issueId"), Index("reportId")]
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val issueId: String,
    val reportId: String,
    val imagePath: String, // Storage reference / local persistent file path or URI
    val description: String,
    val isReferenceImage: Boolean,
    val issueNumber: Int, // Denormalized copy from parent Issue
    val jobReference: String, // Denormalized copy from parent Report
    val createdAt: Long = System.currentTimeMillis()
)
