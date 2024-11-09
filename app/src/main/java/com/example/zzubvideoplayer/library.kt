package com.example.zzubvideoplayer.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val videoFiles = remember { mutableStateListOf<VideoFile>() }
    var playingUri by remember { mutableStateOf<String?>(null) }
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            videoFiles.addAll(getAllVideoFiles(context))
        } else {
            ActivityCompat.requestPermissions(
                (context as android.app.Activity),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
            Toast.makeText(context, "Permission required to display videos", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Video Library", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (playingUri != null) {
            // Display the player when a video is selected
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)
            )
        }

        if (videoFiles.isNotEmpty()) {
            LazyColumn {
                items(videoFiles) { video ->
                    VideoItem(
                        video = video,
                        onVideoClick = {
                            playingUri = video.filePath
                            val mediaItem = MediaItem.fromUri(playingUri!!)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        },
                        context = context
                    )
                }
            }
        } else {
            Text("No videos found or permission not granted.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (exoPlayer.isPlaying) {
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
    }
}

@Composable
fun VideoItem(video: VideoFile, onVideoClick: () -> Unit, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onVideoClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val thumbnail = remember { getVideoThumbnail(context, video.filePath) }
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Thumbnail for ${video.title}",
                    modifier = Modifier.size(80.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = video.title, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = video.filePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

// Data class and helper functions remain unchanged

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
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
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
