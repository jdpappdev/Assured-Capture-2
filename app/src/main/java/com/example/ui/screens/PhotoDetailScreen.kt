package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.InspectionRepository
import com.example.data.PhotoEntity
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.VoiceOutlinedTextField
import com.example.ui.components.VoiceTranscriptionBar
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoDetailScreen(
    photoId: String,
    repository: InspectionRepository,
    onBack: () -> Unit,
    onPhotoDeleted: () -> Unit
) {
    val photoFlow = remember(photoId) { repository.getPhotoFlow(photoId) }
    val photo by photoFlow.collectAsState(initial = null)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var description by remember { mutableStateOf("") }
    var isReferenceImage by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(photo) {
        photo?.let {
            description = it.description
            isReferenceImage = it.isReferenceImage
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (photo == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading photo details...", color = TextSecondary)
        }
        return
    }

    val currentPhoto = photo!!
    val formattedCaptureTime = remember(currentPhoto.createdAt) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(currentPhoto.createdAt))
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
                            modifier = Modifier.testTag("photo_detail_back_button")
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
                                text = "Photo Inspection Record",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${currentPhoto.jobReference} • Issue ${currentPhoto.issueNumber}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryAccent
                            )
                        }
                    }

                    // Save Photo Action (navigates back to previous screen)
                    Button(
                        onClick = {
                            scope.launch {
                                repository.updatePhoto(
                                    photoId = photoId,
                                    description = description,
                                    isReferenceImage = isReferenceImage
                                )
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_photo_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large full-width image preview at top
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .background(Color(0xFF0F172A))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(File(currentPhoto.imagePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Full Inspection Photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isReferenceImage) {
                                Surface(
                                    color = StatusInProgressBg,
                                    border = BorderStroke(1.dp, StatusInProgressBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = StatusInProgressText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PRIMARY REFERENCE",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StatusInProgressText
                                        )
                                    }
                                }
                            }
                        }

                        // Denormalized metadata row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = PrimaryAccentLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Job: ${currentPhoto.jobReference}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = BackgroundNeutral,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Issue ${currentPhoto.issueNumber}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = formattedCaptureTime,
                                fontSize = 13.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }

                // Photo Defect Description Card with the single voice-to-text button
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Photo Defect Description",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type notes or use the voice-to-text recording button below.",
                            fontSize = 14.sp,
                            color = TextTertiary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dedicated Voice-to-Text Button for Photo Description
                        VoiceTranscriptionBar(
                            currentText = description,
                            onTextAppended = { updatedText ->
                                description = updatedText
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        VoiceOutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = {
                                Text(
                                    text = "Describe defect observed, specific location, severity or dimensions...",
                                    color = TextTertiary,
                                    fontSize = 15.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(12.dp),
                            testTag = "photo_description_input",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Reference Image checkbox row with large tap target
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isReferenceImage) StatusInProgressBg.copy(alpha = 0.5f) else SurfaceCard
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isReferenceImage) StatusInProgressBorder else CardBorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isReferenceImage = !isReferenceImage }
                        .testTag("reference_image_toggle")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isReferenceImage,
                            onCheckedChange = { isReferenceImage = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryAccent
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mark as Reference Image",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Flagged for highlight in the Word report assembly",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Save button (navigates back upon save)
                Button(
                    onClick = {
                        scope.launch {
                            repository.updatePhoto(
                                photoId = photoId,
                                description = description,
                                isReferenceImage = isReferenceImage
                            )
                            onBack()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_photo_large_button")
                ) {
                    Text(
                        text = "Save",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Destructive "Delete Photo" action at the bottom
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
                        .testTag("delete_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Photo",
                        tint = DestructiveRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Photo",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DestructiveRed
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Universal Delete Photo Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            title = "Delete Photo?",
            message = "This will permanently remove this inspection photo and its description from Issue ${currentPhoto.issueNumber}.",
            itemLabel = if (currentPhoto.description.isNotBlank()) "\"${currentPhoto.description}\"" else "Captured on $formattedCaptureTime",
            onConfirm = {
                scope.launch {
                    repository.deletePhoto(photoId)
                    showDeleteConfirmDialog = false
                    onPhotoDeleted()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}
