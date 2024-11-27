package com.example.zzubvideoplayer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.sqrt

@Composable
fun ShortsScreen(
    videos: List<MediaFile>,
    exoPlayer: ExoPlayer,
    context: Context
) {
    // Filter for videos less than 1 minute in duration
    val shorts = videos.filter { it.duration < 60_000 }
    var currentIndex by remember { mutableStateOf(0) }

    // Load the current video based on currentIndex
    val currentVideo = shorts.getOrNull(currentIndex)

    // Define the function to load the next video
    val loadNextVideo: () -> Unit = {
        if (currentIndex < shorts.size - 1) {
            currentIndex++
        } else {
            // Optionally, loop back to the first video
            currentIndex = 0
        }
    }

    // Define the function to load the previous video
    val loadPreviousVideo: () -> Unit = {
        if (currentIndex > 0) {
            currentIndex--
        } else {
            // Optionally, jump to the last video
            currentIndex = shorts.size - 1
        }
    }

    // Integrate shake detection logic
    // Note: ShakeDetector is not defined, so this part is commented out
    // Uncomment and implement ShakeDetector if it's available

    ShakeDetector(
        context = context,
        onShakeDetected = {
            loadNextVideo()
            Toast.makeText(context, "Shake detected! Loading next video...", Toast.LENGTH_SHORT).show()
        }
    )


    // Only display if there is a current video
    currentVideo?.let { video ->
        FullScreenVideoPlayer(
            videoUri = video.uri,
            onVideoEnded = loadNextVideo,
            onSwipeUp = loadNextVideo,
            onSwipeDown = loadPreviousVideo,
            exoPlayer = exoPlayer
        )
    }
}

@Composable
fun ShakeDetector(
    context: Context,
    onShakeDetected: () -> Unit
) {
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val shakeThreshold = 12f
    val shakeInterval = 500 // Minimum time between shakes in milliseconds
    var lastShakeTimestamp by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate the acceleration magnitude
                    val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

                    // Get current time
                    val currentTime = System.currentTimeMillis()

                    // Check if the acceleration exceeds the threshold and if enough time has passed since the last shake
                    if (acceleration > shakeThreshold && currentTime - lastShakeTimestamp > shakeInterval) {
                        lastShakeTimestamp = currentTime
                        onShakeDetected()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }

        // Register sensor listener
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            // Unregister sensor listener to avoid memory leaks
            sensorManager.unregisterListener(listener)
        }
    }
}


@Composable
fun FullScreenVideoPlayer(
    videoUri: Uri,
    onVideoEnded: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    exoPlayer: ExoPlayer
) {
    val context = LocalContext.current
    val swipeThreshold = 100f // Define threshold for swipe detection

    // Manage ExoPlayer lifecycle and playback
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

    // Implement AndroidView for ExoPlayer integration in Compose
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // Hide default video controls
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        val dragDistance = dragAmount
                        if (dragDistance > swipeThreshold) {
                            onSwipeDown()
                        } else if (dragDistance < -swipeThreshold) {
                            onSwipeUp()
                        }
                    }
                )
            }
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
            val thumbnail = null // Add a null thumbnail for now

            mediaFiles.add(MediaFile(id, uri, displayName, duration, size, dateModified, thumbnail))
        }
    }

    return mediaFiles
}