package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HistoryJsonConverter
import com.example.data.InspectionDateHistoryEntry
import com.example.data.InspectionRepository
import com.example.data.IssueEntity
import com.example.data.PhotoEntity
import com.example.data.ReportEntity
import com.example.data.ReportStatus
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoiceOutlinedTextField
import com.example.ui.theme.BackgroundNeutral
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.DestructiveRedBorder
import com.example.ui.theme.DestructiveRedLight
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryAccentLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    repository: InspectionRepository,
    onBack: () -> Unit,
    onIssueClick: (issueId: String) -> Unit,
    onReportDeleted: () -> Unit
) {
    val reportFlow = remember(reportId) { repository.getReportFlow(reportId) }
    val report by reportFlow.collectAsState(initial = null)

    val issuesFlow = remember(reportId) { repository.getIssuesForReport(reportId) }
    val issues by issuesFlow.collectAsState(initial = emptyList())

    val allPhotosFlow = remember(reportId) { repository.getPhotosForReport(reportId) }
    val allPhotos by allPhotosFlow.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form fields
    var jobReference by remember { mutableStateOf("") }
    var reportName by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var reportDate by remember { mutableStateOf("") }
    var inspectionDate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ReportStatus.DRAFT.label) }

    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var showReportDatePicker by remember { mutableStateOf(false) }
    var showInspectionDatePicker by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showAddIssueDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Populate form when report loads
    LaunchedEffect(report) {
        report?.let { r ->
            jobReference = r.jobReference
            reportName = r.reportName
            clientName = r.clientName
            address = r.address
            reportDate = r.reportDate
            inspectionDate = r.inspectionDate
            status = r.status
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (report == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading inspection report...", color = TextSecondary)
        }
        return
    }

    val historyEntries = remember(report?.inspectionDateHistory) {
        HistoryJsonConverter.fromJson(report?.inspectionDateHistory ?: "[]")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundNeutral)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset)
        ) {
            // Header Bar
            Surface(
                color = SurfaceCard,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("report_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Report Details",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = report?.jobReference ?: "",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryAccent
                            )
                        }
                    }

                    // Save Report action (Automatically navigates back upon saving)
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                repository.updateReport(
                                    reportId = reportId,
                                    jobReference = jobReference,
                                    reportName = reportName,
                                    clientName = clientName,
                                    address = address,
                                    reportDate = reportDate,
                                    status = status,
                                    newInspectionDate = inspectionDate
                                )
                                isSaving = false
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_report_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Form Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Inspection Metadata",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            // Job Reference
                            VoiceOutlinedTextField(
                                value = jobReference,
                                onValueChange = { jobReference = it },
                                label = { Text("Job Reference") },
                                singleLine = true,
                                testTag = "detail_job_reference_input",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Report Name
                            VoiceOutlinedTextField(
                                value = reportName,
                                onValueChange = { reportName = it },
                                label = { Text("Report Name") },
                                singleLine = true,
                                testTag = "detail_report_name_input",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Client Name
                            VoiceOutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Client Name") },
                                singleLine = true,
                                testTag = "detail_client_name_input",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Address
                            VoiceOutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Site Address") },
                                testTag = "detail_address_input",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Report Date with Calendar Widget
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        VoiceOutlinedTextField(
                                            value = reportDate,
                                            onValueChange = { reportDate = it },
                                            label = { Text("Report Date (YYYY-MM-DD)") },
                                            singleLine = true,
                                            testTag = "detail_report_date_input",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Glove-friendly Calendar Widget Launch Button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PrimaryAccentLight,
                                        border = BorderStroke(1.5.dp, PrimaryAccent.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(56.dp)
                                            .clickable { showReportDatePicker = true }
                                            .testTag("report_date_calendar_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Select report date from calendar",
                                                tint = PrimaryAccent,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Status Selector styled with the color-coded pill
                            Column {
                                Text(
                                    text = "Report Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CardBorderColor),
                                    color = SurfaceCard,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { statusDropdownExpanded = !statusDropdownExpanded }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                StatusBadge(status = status)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (statusDropdownExpanded) "Select status" else "Tap to change",
                                                    fontSize = 15.sp,
                                                    color = TextTertiary
                                                )
                                            }
                                            Icon(
                                                imageVector = if (statusDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Toggle status selector",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        AnimatedVisibility(visible = statusDropdownExpanded) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp)
                                            ) {
                                                ReportStatus.entries.forEach { opt ->
                                                    val isSelected = opt.label.equals(status, ignoreCase = true)
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected) PrimaryAccentLight else BackgroundNeutral,
                                                        border = BorderStroke(
                                                            if (isSelected) 1.5.dp else 1.dp,
                                                            if (isSelected) PrimaryAccent else CardBorderColor
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                status = opt.label
                                                                statusDropdownExpanded = false
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            StatusBadge(status = opt.label)
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = PrimaryAccent,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Inspection Date Field with View History on top
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Inspection Date",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Surface(
                                        onClick = { showHistorySheet = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryAccentLight,
                                        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.4f)),
                                        modifier = Modifier.testTag("view_date_history_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "View inspection date history",
                                                tint = PrimaryAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "View history (${historyEntries.size})",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryAccent
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        VoiceOutlinedTextField(
                                            value = inspectionDate,
                                            onValueChange = { inspectionDate = it },
                                            placeholder = { Text("YYYY-MM-DD") },
                                            singleLine = true,
                                            testTag = "detail_inspection_date_input",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Glove-friendly Calendar Widget Launch Button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PrimaryAccentLight,
                                        border = BorderStroke(1.5.dp, PrimaryAccent.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clickable { showInspectionDatePicker = true }
                                            .testTag("inspection_date_calendar_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Select inspection date from calendar",
                                                tint = PrimaryAccent,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Updates are recorded in append-only history",
                                    fontSize = 13.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Full-width accent "Add Issue" button (Only "Add Issue", no + + icons, enlarged for gloves)
                item {
                    Button(
                        onClick = { showAddIssueDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("add_issue_button")
                    ) {
                        Text(
                            text = "Add Issue",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Issues Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inspection Issues (${issues.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Issues List
                if (issues.isEmpty()) {
                    item {
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No issues logged for this report yet.",
                                    fontSize = 16.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Add Issue' to log defects and attach inspection photos.",
                                    fontSize = 14.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                } else {
                    items(issues, key = { it.id }) { issue ->
                        val issuePhotos = allPhotos.filter { it.issueId == issue.id }
                        IssueCardItem(
                            issue = issue,
                            photoCount = issuePhotos.size,
                            onClick = { onIssueClick(issue.id) }
                        )
                    }
                }

                // Destructive "Delete Report" button at the bottom (Enlarged for gloves)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DestructiveRedLight,
                            contentColor = DestructiveRed
                        ),
                        border = BorderStroke(1.5.dp, DestructiveRedBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("delete_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Report",
                            tint = DestructiveRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Report",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DestructiveRed
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Inspection Date History Bottom Sheet
    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = SurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Inspection Date History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Read-only audit log. Values are appended automatically on date change.",
                    fontSize = 14.sp,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current Date
                Surface(
                    color = PrimaryAccentLight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PrimaryAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Active Inspection Date",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                            Text(
                                text = report?.inspectionDate.takeIf { !it.isNullOrBlank() } ?: "None set",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                        StatusBadge(status = "In Progress")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (historyEntries.isEmpty()) {
                    Text(
                        text = "No previous dates logged. When you edit the inspection date and tap 'Save', previous values will appear here.",
                        fontSize = 16.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "Previous Date Revisions (${historyEntries.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    historyEntries.reversed().forEachIndexed { index, entry ->
                        val formattedTime = remember(entry.changedAt) {
                            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(entry.changedAt))
                        }
                        Surface(
                            color = BackgroundNeutral,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = entry.date,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Changed: $formattedTime",
                                        fontSize = 14.sp,
                                        color = TextTertiary
                                    )
                                }
                                Text(
                                    text = "Revision #${historyEntries.size - index}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showHistorySheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Close", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Issue Dialog
    if (showAddIssueDialog) {
        var issueNameInput by remember { mutableStateOf("") }
        var issueDescInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddIssueDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, CardBorderColor),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Add Inspection Issue",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Next sequential issue will be assigned automatically (Issue ${(report?.nextIssueNumber ?: 1)})",
                        fontSize = 15.sp,
                        color = TextTertiary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    VoiceOutlinedTextField(
                        value = issueNameInput,
                        onValueChange = { issueNameInput = it },
                        label = { Text("Issue Name (e.g. Masonry Cracks)") },
                        singleLine = true,
                        testTag = "dialog_issue_name_input",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VoiceOutlinedTextField(
                        value = issueDescInput,
                        onValueChange = { issueDescInput = it },
                        label = { Text("Issue Description") },
                        minLines = 3,
                        testTag = "dialog_issue_desc_input",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddIssueDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Cancel", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (issueNameInput.isNotBlank()) {
                                    scope.launch {
                                        val newIssue = repository.addIssue(
                                            reportId = reportId,
                                            issueName = issueNameInput,
                                            issueDescription = issueDescInput
                                        )
                                        showAddIssueDialog = false
                                        onIssueClick(newIssue.id)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("dialog_save_issue_button")
                        ) {
                            Text("Add Issue", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Delete Report Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            title = "Delete Report?",
            message = "This will permanently delete this inspection report and all its associated issues and photos. This cannot be undone.",
            itemLabel = "${report?.jobReference} — ${report?.reportName}",
            onConfirm = {
                scope.launch {
                    repository.deleteReport(reportId)
                    showDeleteConfirmDialog = false
                    onReportDeleted()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Calendar Date Picker for Report Date
    if (showReportDatePicker) {
        ReportDatePickerDialog(
            title = "Select Report Date",
            initialDate = reportDate,
            onDateSelected = { selectedDate ->
                reportDate = selectedDate
            },
            onDismiss = { showReportDatePicker = false }
        )
    }

    // Calendar Date Picker for Inspection Date
    if (showInspectionDatePicker) {
        ReportDatePickerDialog(
            title = "Select Inspection Date",
            initialDate = inspectionDate,
            onDateSelected = { selectedDate ->
                inspectionDate = selectedDate
            },
            onDismiss = { showInspectionDatePicker = false }
        )
    }
}

@Composable
fun IssueCardItem(
    issue: IssueEntity,
    photoCount: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("issue_card_${issue.issueNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = PrimaryAccentLight,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "Issue ${issue.issueNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = issue.issueName.ifBlank { "Untitled Issue" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (issue.issueDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = issue.issueDescription,
                        fontSize = 15.sp,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Photo Count Chip
            Surface(
                color = BackgroundNeutral,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Photos",
                        tint = if (photoCount > 0) PrimaryAccent else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$photoCount ${if (photoCount == 1) "photo" else "photos"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (photoCount > 0) PrimaryAccent else TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDatePickerDialog(
    title: String,
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDate) {
        parseIsoDateToUtcMillis(initialDate)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(formatUtcMillisToIso(millis))
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("date_picker_confirm_button")
            ) {
                Text("Select", fontWeight = FontWeight.Bold, color = PrimaryAccent)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date_picker_cancel_button")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                )
            }
        )
    }
}

private fun parseIsoDateToUtcMillis(dateStr: String): Long {
    return try {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val parts = dateStr.trim().split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull() ?: return System.currentTimeMillis()
            val month = (parts[1].toIntOrNull() ?: 1) - 1
            val day = parts[2].toIntOrNull() ?: 1
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, month, day, 12, 0, 0)
            }
            cal.timeInMillis
        } else {
            System.currentTimeMillis()
        }
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

private fun formatUtcMillisToIso(millis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = millis
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}
