package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.data.util.ImagePickerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun MarkerImageView(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Marker Photo"
) {
    if (imageUrl.isNullOrBlank()) return

    var bitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }
    var hasError by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        isLoading = true
        hasError = false
        withContext(Dispatchers.IO) {
            try {
                if (imageUrl.startsWith("data:image")) {
                    bitmap = ImagePickerHelper.base64ToBitmap(imageUrl)
                } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    val stream = URL(imageUrl).openStream()
                    bitmap = BitmapFactory.decodeStream(stream)
                    stream.close()
                }
            } catch (_: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
