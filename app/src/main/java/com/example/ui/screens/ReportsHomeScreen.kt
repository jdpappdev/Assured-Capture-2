package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InspectionRepository
import com.example.data.ReportEntity
import com.example.data.ReportStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoiceOutlinedTextField
import com.example.ui.theme.BackgroundNeutral
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryAccentLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.DocxReportExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsHomeScreen(
    reports: List<ReportEntity>,
    repository: InspectionRepository,
    onReportClick: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var exportingReportId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var showNewReportDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredReports = remember(reports, searchQuery, selectedStatusFilter) {
        reports.filter { r ->
            val matchesQuery = searchQuery.isBlank() ||
                    r.jobReference.contains(searchQuery, ignoreCase = true) ||
                    r.reportName.contains(searchQuery, ignoreCase = true) ||
                    r.clientName.contains(searchQuery, ignoreCase = true) ||
                    r.address.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatusFilter == null ||
                    r.status.equals(selectedStatusFilter, ignoreCase = true)

            matchesQuery && matchesStatus
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundNeutral)
                .padding(top = topInset)
        ) {
        // Header row
        Surface(
            color = SurfaceCard,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo_1789544621539),
                        contentDescription = "Assured Capture Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Assured Capture",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Glove-friendly round button with + icon in the middle (enlarged for field use)
                IconButton(
                    onClick = { showNewReportDialog = true },
                    modifier = Modifier
                        .size(60.dp)
                        .background(PrimaryAccent, CircleShape)
                        .testTag("new_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Report",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Search Bar below header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            VoiceOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search reports...",
                        color = TextTertiary,
                        fontSize = 17.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                testTag = "search_reports_input",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }

        // Quick status filter chips for field inspectors wearing gloves
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = {
                        Text(
                            text = "All (${reports.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryAccent,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedStatusFilter == null,
                        borderColor = CardBorderColor,
                        selectedBorderColor = PrimaryAccent
                    ),
                    modifier = Modifier.heightIn(min = 44.dp)
                )
            }
            items(ReportStatus.entries) { status ->
                val isSelected = selectedStatusFilter == status.label
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedStatusFilter = if (isSelected) null else status.label
                    },
                    label = {
                        Text(
                            text = status.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryAccent,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = CardBorderColor,
                        selectedBorderColor = PrimaryAccent
                    ),
                    modifier = Modifier.heightIn(min = 44.dp)
                )
            }
        }

        // Scrollable list of report cards
        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching reports" else "No inspection reports yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search query" else "Tap '+ New Report' to start your first field inspection",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reports_list")
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    ReportCardItem(
                        report = report,
                        isExporting = exportingReportId == report.id,
                        onClick = { onReportClick(report.id) },
                        onExportDocx = {
                            scope.launch {
                                exportingReportId = report.id
                                try {
                                    val docxFile = repository.exportReportDocx(report.id)
                                    if (docxFile != null) {
                                        val safeRef = report.jobReference.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                        val displayName = "${safeRef}_Inspection_Report.docx"
                                        DocxReportExporter.saveToDownloads(context, docxFile, displayName)
                                        Toast.makeText(context, "Saved $displayName to Downloads", Toast.LENGTH_SHORT).show()

                                        val result = snackbarHostState.showSnackbar(
                                            message = "Exported $displayName to Downloads",
                                            actionLabel = "Share",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            DocxReportExporter.shareDocx(context, docxFile, "Share Inspection Report")
                                        }
                                    } else {
                                        Toast.makeText(context, "Could not export report", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    exportingReportId = null
                                }
                            }
                        }
                    )
                }
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

    // New Report Dialog
    if (showNewReportDialog) {
        NewReportDialog(
            onDismiss = { showNewReportDialog = false },
            onCreate = { jobRef, reportName, client, address, reportDate, status, inspectionDate ->
                scope.launch {
                    val newRep = repository.createReport(
                        jobReference = jobRef,
                        reportName = reportName,
                        clientName = client,
                        address = address,
                        reportDate = reportDate,
                        status = status,
                        inspectionDate = inspectionDate
                    )
                    showNewReportDialog = false
                    onReportClick(newRep.id)
                }
            }
        )
    }
}

@Composable
fun ReportCardItem(
    report: ReportEntity,
    isExporting: Boolean,
    onClick: () -> Unit,
    onExportDocx: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard
        ),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("report_card_${report.id}")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with Job Reference and Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.jobReference,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryAccent,
                        letterSpacing = 0.5.sp
                    )
                    StatusBadge(status = report.status)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Report Name
                Text(
                    text = report.reportName.ifBlank { "Untitled Report" },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Client Name row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Client",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.clientName.ifBlank { "Unassigned Client" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Inspection Date row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Inspection Date",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inspection: ${report.inspectionDate.ifBlank { "Not set" }}",
                        fontSize = 15.sp,
                        color = TextTertiary
                    )
                }
            }

            // Green round download button with arrow down icon positioned at lower right corner
            Surface(
                shape = CircleShape,
                color = Color(0xFF16A34A),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(46.dp)
                    .clickable(enabled = !isExporting) { onExportDocx() }
                    .testTag("download_docx_button_${report.id}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Export Report as DOCX",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportDialog(
    onDismiss: () -> Unit,
    onCreate: (
        jobRef: String,
        name: String,
        client: String,
        address: String,
        reportDate: String,
        status: String,
        inspectionDate: String
    ) -> Unit
) {
    // Current year digits e.g. "26" for BBC-[YY]-
    val currentYearTwoDigits = remember {
        val cal = Calendar.getInstance()
        val yearStr = cal.get(Calendar.YEAR).toString()
        if (yearStr.length >= 2) yearStr.takeLast(2) else "26"
    }

    val todayFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var jobRef by remember { mutableStateOf("BBC-$currentYearTwoDigits-") }
    var reportName by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var reportDate by remember { mutableStateOf(todayFormatted) }
    var inspectionDate by remember { mutableStateOf(todayFormatted) }
    var status by remember { mutableStateOf(ReportStatus.IN_PROGRESS.label) }

    Dialog(onDismissRequest = onDismiss) {
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
                    text = "New Inspection Report",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Field inspection setup",
                    fontSize = 15.sp,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Job Reference (pre-filled BBC-[YY]-, editable with voice)
                VoiceOutlinedTextField(
                    value = jobRef,
                    onValueChange = { jobRef = it },
                    label = { Text("Job Reference (BBC-[YY]-)") },
                    singleLine = true,
                    testTag = "dialog_job_ref_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Report Name
                VoiceOutlinedTextField(
                    value = reportName,
                    onValueChange = { reportName = it },
                    label = { Text("Report Name") },
                    singleLine = true,
                    testTag = "dialog_report_name_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Client Name
                VoiceOutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name") },
                    singleLine = true,
                    testTag = "dialog_client_name_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Address
                VoiceOutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Site Address") },
                    singleLine = true,
                    testTag = "dialog_address_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Initial Status Selection with Color-coded pills
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Initial Status",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ReportStatus.entries) { opt ->
                            val isSelected = opt.label.equals(status, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) PrimaryAccentLight else Color.Transparent,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) PrimaryAccent else CardBorderColor
                                ),
                                modifier = Modifier
                                    .clickable { status = opt.label }
                                    .padding(1.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    StatusBadge(status = opt.label)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Glove-friendly large action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("Cancel", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val finalJobRef = if (jobRef.isNotBlank()) jobRef else "BBC-$currentYearTwoDigits-01"
                            val finalName = if (reportName.isNotBlank()) reportName else "Site Inspection"
                            onCreate(finalJobRef, finalName, clientName, address, reportDate, status, inspectionDate)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("dialog_submit_report_button")
                    ) {
                        Text("Create", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
