package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.InspectionRepository
import com.example.data.IssueEntity
import com.example.data.PhotoEntity
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.VoiceOutlinedTextField
import com.example.ui.theme.BackgroundNeutral
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.DestructiveRedBorder
import com.example.ui.theme.DestructiveRedLight
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryAccentLight
import com.example.ui.theme.StatusInProgressBg
import com.example.ui.theme.StatusInProgressBorder
import com.example.ui.theme.StatusInProgressText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun IssueDetailScreen(
    issueId: String,
    repository: InspectionRepository,
    onBack: () -> Unit,
    onPhotoClick: (photoId: String) -> Unit,
    onIssueDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val issueFlow = remember(issueId) { repository.getIssueFlow(issueId) }
    val issue by issueFlow.collectAsState(initial = null)

    val photosFlow = remember(issueId) { repository.getPhotosForIssue(issueId) }
    val photos by photosFlow.collectAsState(initial = emptyList())

    var issueName by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Multi-image upload progress state
    var isProcessingPhotos by remember { mutableStateOf(false) }
    var uploadCurrent by remember { mutableIntStateOf(0) }
    var uploadTotal by remember { mutableIntStateOf(0) }
    var recentUploadSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Camera capture state
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(issue) {
        issue?.let {
            issueName = it.issueName
            issueDescription = it.issueDescription
        }
    }

    // Gallery multi-select launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty() && issue != null) {
            val currentIssue = issue!!
            scope.launch {
                isProcessingPhotos = true
                uploadCurrent = 0
                uploadTotal = uris.size
                recentUploadSuccessMessage = null

                val saved = repository.saveMultipleGalleryImages(
                    reportId = currentIssue.reportId,
                    issueId = currentIssue.id,
                    uris = uris,
                    onProgress = { cur, tot ->
                        uploadCurrent = cur
                        uploadTotal = tot
                    }
                )
                isProcessingPhotos = false
                recentUploadSuccessMessage = "Successfully imported ${saved.size} photos"
                snackbarHostState.showSnackbar("Added ${saved.size} inspection photos")
            }
        }
    }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null && issue != null) {
            val currentIssue = issue!!
            val fileToSave = tempCameraFile!!
            scope.launch {
                isProcessingPhotos = true
                uploadCurrent = 1
                uploadTotal = 1
                repository.saveCapturedPhoto(
                    reportId = currentIssue.reportId,
                    issueId = currentIssue.id,
                    tempFile = fileToSave
                )
                isProcessingPhotos = false
                snackbarHostState.showSnackbar("Inspection photo captured")
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraFile = photoFile
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos")
            }
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (issue == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading issue details...", color = TextSecondary)
        }
        return
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
                            modifier = Modifier.testTag("issue_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Surface(
                                color = PrimaryAccentLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Issue ${issue?.issueNumber}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = issue?.issueName?.ifBlank { "Issue Details" } ?: "Issue Details",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    // Save Issue action (Automatically navigates back upon saving)
                    Button(
                        onClick = {
                            scope.launch {
                                repository.updateIssue(
                                    issueId = issueId,
                                    issueName = issueName,
                                    issueDescription = issueDescription
                                )
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_issue_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("issue_photos_grid")
            ) {
                // Issue Name & Description editable form card (full span)
                item(span = { GridItemSpan(2) }) {
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Issue Definition",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            VoiceOutlinedTextField(
                                value = issueName,
                                onValueChange = { issueName = it },
                                label = { Text("Issue Name") },
                                singleLine = true,
                                testTag = "issue_name_input",
                                modifier = Modifier.fillMaxWidth()
                            )

                            VoiceOutlinedTextField(
                                value = issueDescription,
                                onValueChange = { issueDescription = it },
                                label = { Text("Issue Description & Notes") },
                                minLines = 3,
                                testTag = "issue_desc_input",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Glove-friendly high-contrast photo action buttons (full span)
                item(span = { GridItemSpan(2) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Choose from Gallery (Multi-select)
                            Button(
                                onClick = {
                                    galleryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .testTag("choose_gallery_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gallery Multi",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Take Photo (In-app camera single capture)
                            Button(
                                onClick = {
                                    val hasCamPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCamPerm) {
                                        val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        tempCameraFile = photoFile
                                        tempCameraUri = uri
                                        takePictureLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A) // High-contrast Slate Dark for camera
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .testTag("take_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Take Photo",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Multi-image upload progress feedback banner
                if (isProcessingPhotos) {
                    item(span = { GridItemSpan(2) }) {
                        Surface(
                            color = PrimaryAccentLight,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, PrimaryAccent),
                            modifier = Modifier.fillMaxWidth().testTag("photo_upload_progress_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saving photo $uploadCurrent of $uploadTotal...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = PrimaryAccent
                                    )
                                    Text(
                                        text = "${((uploadCurrent.toFloat() / (uploadTotal.takeIf { it > 0 } ?: 1)) * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = PrimaryAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (uploadTotal > 0) uploadCurrent.toFloat() / uploadTotal else 0f
                                    },
                                    color = PrimaryAccent,
                                    trackColor = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Processing image pipeline & copying to safe storage",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                if (recentUploadSuccessMessage != null && !isProcessingPhotos) {
                    item(span = { GridItemSpan(2) }) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recentUploadSuccessMessage ?: "",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }

                // Photos section title (full span)
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attached Photos (${photos.size})",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap photo to edit notes or voice-transcribe",
                            fontSize = 14.sp,
                            color = TextTertiary
                        )
                    }
                }

                // Photo Thumbnails
                if (photos.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No photos captured for Issue ${issue?.issueNumber} yet",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use 'Gallery Multi' or 'Take Photo' above to record field evidence.",
                                    fontSize = 15.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(photos, key = { it.id }) { photo ->
                        PhotoThumbnailCard(
                            photo = photo,
                            onClick = { onPhotoClick(photo.id) }
                        )
                    }
                }

                // Destructive "Delete Issue" action at the bottom (full span)
                item(span = { GridItemSpan(2) }) {
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
                            .testTag("delete_issue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Issue",
                            tint = DestructiveRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Issue ${issue?.issueNumber}",
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

    // Universal Delete Issue Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            title = "Delete Issue ${issue?.issueNumber}?",
            message = "Are you sure you want to delete this issue and its ${photos.size} attached photos? Sequential numbering for remaining issues will be preserved (Issue ${issue?.issueNumber} will not be reused).",
            itemLabel = "Issue ${issue?.issueNumber}: ${issue?.issueName}",
            onConfirm = {
                scope.launch {
                    repository.deleteIssue(issueId)
                    showDeleteConfirmDialog = false
                    onIssueDeleted()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
fun PhotoThumbnailCard(
    photo: PhotoEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("photo_card_${photo.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(photo.imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Inspection Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Reference Image tag if checked
                if (photo.isReferenceImage) {
                    Surface(
                        color = StatusInProgressBg,
                        border = BorderStroke(1.dp, StatusInProgressBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StatusInProgressText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Reference",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusInProgressText
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                if (photo.description.isNotBlank()) {
                    Text(
                        text = photo.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 2,
                        lineHeight = 18.sp
                    )
                } else {
                    Text(
                        text = "No description. Tap to type or voice transcribe.",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
