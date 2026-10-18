package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Marker
import com.example.data.model.MarkerCategory
import com.example.data.model.SkinCatalog
import com.example.data.util.ImagePickerHelper
import com.example.data.util.Strings
import com.example.ui.components.MarkerImageView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditMarkerDialog(
    initialLocation: Pair<Double, Double>,
    editingMarker: Marker? = null,
    unlockedMarkerSkins: Set<String> = setOf("default"),
    defaultMarkerSkin: String = "default",
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        category: String,
        lat: Double,
        lon: Double,
        imageUrl: String?,
        markerSkin: String
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(editingMarker?.title ?: "") }
    var description by remember { mutableStateOf(editingMarker?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(editingMarker?.categoryEnum ?: MarkerCategory.TOURISM)
    }
    var selectedSkin by remember {
        mutableStateOf(editingMarker?.markerSkin ?: defaultMarkerSkin)
    }
    var imageUrl by remember { mutableStateOf(editingMarker?.imageUrl) }
    var titleError by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImagePickerHelper.uriToBase64(context, uri)
            if (base64 != null) {
                imageUrl = base64
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("add_marker_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(selectedCategory.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = selectedCategory.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (editingMarker != null) {
                                if (Strings.isUk) "Редагування мітки" else "Edit Marker"
                            } else {
                                if (Strings.isUk) "Додати нову мітку" else "Add New Marker"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.5f, %.5f", initialLocation.first, initialLocation.second),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info banner about moderation and points reward
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (Strings.isUk) {
                                "Після схвалення модератором мітка стане публічною, а ви отримаєте +50 поінтів!"
                            } else {
                                "After moderator approval, your marker will be public and you will get +50 points!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text(if (Strings.isUk) "Назва локації / об'єкта *" else "Location Name *") },
                    placeholder = {
                        Text(if (Strings.isUk) "Наприклад: Андріївський узвіз чи Кав'ярня" else "E.g.: St. Sophia Cathedral or Cafe")
                    },
                    singleLine = true,
                    isError = titleError,
                    supportingText = {
                        if (titleError) {
                            Text(if (Strings.isUk) "Будь ласка, вкажіть назву" else "Please enter a name")
                        } else {
                            Text("${title.length}/100")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marker_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (Strings.isUk) "Опис та особливості" else "Description") },
                    placeholder = {
                        Text(if (Strings.isUk) "Опишіть місце, чим воно цікаве..." else "Describe why this spot is special...")
                    },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("${description.length}/500") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marker_desc_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Photo Attachment Section
                Text(
                    text = Strings.photoLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        MarkerImageView(
                            imageUrl = imageUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { imageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Видалити фото",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.addPhotoBtn)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Marker Skin Selection
                Text(
                    text = if (Strings.isUk) "Скін мітки (видно всім):" else "Marker Pin Skin (visible to all):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SkinCatalog.markerSkins.forEach { skin ->
                        val isUnlocked = unlockedMarkerSkins.contains(skin.id) || skin.id == "default"
                        val isSelected = selectedSkin == skin.id

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) skin.pinColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) skin.pinColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = isUnlocked) {
                                    if (isUnlocked) selectedSkin = skin.id
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(skin.emoji, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (Strings.isUk) skin.nameUa else skin.nameEn,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                                if (!isUnlocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = if (Strings.isUk) "Категорія місця:" else "Place Category:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MarkerCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) cat.color else cat.color.copy(alpha = 0.12f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) cat.color else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else cat.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.localizedTitle(Strings.isUk),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Text(Strings.cancel)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                onSubmit(
                                    title,
                                    description,
                                    selectedCategory.key,
                                    initialLocation.first,
                                    initialLocation.second,
                                    imageUrl,
                                    selectedSkin
                                )
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_marker_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (editingMarker != null) (if (Strings.isUk) "Зберегти" else "Save") else (if (Strings.isUk) "Надіслати" else "Submit"))
                        }
                    }
                }
            }
        }
    }
}
