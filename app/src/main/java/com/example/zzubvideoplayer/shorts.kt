package com.example.zzubvideoplayer

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun ShortsScreen(videos: List<MediaFile>, exoPlayer: ExoPlayer) {
    val shorts = videos.filter { it.duration < 60_000 }
    var currentVideo by remember { mutableStateOf(shorts.randomOrNull()) }

    // Function to load the next random video
    val loadNextVideo = {
        val remainingVideos = shorts.filter { it != currentVideo }
        currentVideo = if (remainingVideos.isNotEmpty()) {
            remainingVideos.random()
        } else {
            shorts.randomOrNull()
        }
    }

    currentVideo?.let { video ->
        FullScreenVideoPlayer(
            videoUri = video.uri,
            onVideoEnded = loadNextVideo,
            exoPlayer = exoPlayer
        )
    }
}

@Composable
fun FullScreenVideoPlayer(videoUri: Uri, onVideoEnded: () -> Unit, exoPlayer: ExoPlayer) {
    val context = LocalContext.current

    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        })

        onDispose {
            exoPlayer.stop()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // Hide controls for full-screen view
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


fun fetchShortVideos(context: Context): List<MediaFile> {
    val mediaFiles = mutableListOf<MediaFile>()
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_MODIFIED
    )

    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Video.Media.DURATION} <= ?",
        arrayOf("60000"), // 60000 ms (1 minute) duration filter
        MediaStore.Video.Media.DATE_MODIFIED + " DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
            val displayName = cursor.getString(displayNameColumn)
            val duration = cursor.getLong(durationColumn)
            val size = cursor.getLong(sizeColumn)
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000

            mediaFiles.add(MediaFile(id, uri, displayName, duration, size, dateModified))
        }
    }

    return mediaFiles
}