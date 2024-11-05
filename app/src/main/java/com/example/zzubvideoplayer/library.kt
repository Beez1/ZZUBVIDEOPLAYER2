package com.example.zzubvideoplayer.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.zzubvideoplayer.screens.VideoFile

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val videoFiles = remember { mutableStateListOf<VideoFile>() }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            videoFiles.addAll(getAllVideoFiles(context))
        } else {
            ActivityCompat.requestPermissions(
                (context as android.app.Activity),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Video Library", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (videoFiles.isNotEmpty()) {
            LazyColumn {
                items(videoFiles) { video ->
                    VideoItem(video = video, context = context)
                }
            }
        } else {
            Text("No videos found or permission not granted.")
        }
    }
}

@Composable
fun VideoItem(video: VideoFile, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display thumbnail
            val thumbnail = remember { getVideoThumbnail(context, video.filePath) }
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Thumbnail for ${video.title}",
                    modifier = Modifier.size(80.dp)
                )
            }

            // Display title and path
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = video.title, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = video.filePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

data class VideoFile(val title: String, val filePath: String)

fun getAllVideoFiles(context: Context): List<VideoFile> {
    val videoList = mutableListOf<VideoFile>()
    val contentResolver: ContentResolver = context.contentResolver
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA
    )

    val cursor = contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        MediaStore.Video.Media.DATE_ADDED + " DESC"
    )

    cursor?.use {
        val titleIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val filePathIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

        while (it.moveToNext()) {
            val title = it.getString(titleIndex)
            val filePath = it.getString(filePathIndex)
            videoList.add(VideoFile(title, filePath))
        }
    }
    return videoList
}

fun getVideoThumbnail(context: Context, filePath: String): Bitmap? {
    val contentResolver: ContentResolver = context.contentResolver
    val projection = arrayOf(MediaStore.Video.Media._ID)
    val selection = "${MediaStore.Video.Media.DATA} = ?"
    val selectionArgs = arrayOf(filePath)

    val cursor = contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )

    var thumbnail: Bitmap? = null
    cursor?.use {
        if (it.moveToFirst()) {
            val videoId = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                contentResolver,
                videoId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
        }
    }
    return thumbnail
}
