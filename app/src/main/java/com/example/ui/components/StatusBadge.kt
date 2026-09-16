package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReportStatus
import com.example.ui.theme.StatusCompletedBg
import com.example.ui.theme.StatusCompletedBorder
import com.example.ui.theme.StatusCompletedText
import com.example.ui.theme.StatusDraftBg
import com.example.ui.theme.StatusDraftBorder
import com.example.ui.theme.StatusDraftText
import com.example.ui.theme.StatusInProgressBg
import com.example.ui.theme.StatusInProgressBorder
import com.example.ui.theme.StatusInProgressText
import com.example.ui.theme.StatusReviewBg
import com.example.ui.theme.StatusReviewBorder
import com.example.ui.theme.StatusReviewText
import com.example.ui.theme.StatusSentBg
import com.example.ui.theme.StatusSentBorder
import com.example.ui.theme.StatusSentText

data class StatusStyle(
    val bg: Color,
    val text: Color,
    val border: Color
)

fun getStatusStyle(statusStr: String): StatusStyle {
    val status = ReportStatus.fromString(statusStr)
    return when (status) {
        ReportStatus.DRAFT -> StatusStyle(StatusDraftBg, StatusDraftText, StatusDraftBorder)
        ReportStatus.IN_PROGRESS -> StatusStyle(StatusInProgressBg, StatusInProgressText, StatusInProgressBorder)
        ReportStatus.AWAITING_REVIEW -> StatusStyle(StatusReviewBg, StatusReviewText, StatusReviewBorder)
        ReportStatus.COMPLETED -> StatusStyle(StatusCompletedBg, StatusCompletedText, StatusCompletedBorder)
        ReportStatus.SENT_TO_CLIENT -> StatusStyle(StatusSentBg, StatusSentText, StatusSentBorder)
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val style = getStatusStyle(status)
    val displayLabel = ReportStatus.fromString(status).label

    Surface(
        color = style.bg,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, style.border),
        modifier = modifier
    ) {
        Text(
            text = displayLabel,
            color = style.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
