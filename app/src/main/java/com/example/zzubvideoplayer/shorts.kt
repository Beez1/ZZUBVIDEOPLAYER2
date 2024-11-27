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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShortsScreen(
    exoPlayer: ExoPlayer
) {
    val context = LocalContext.current
    var shorts by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }

    // Load the short videos asynchronously
    LaunchedEffect(Unit) {
        shorts = fetchShortVideos(context)
    }

    // Define functions to load next and previous videos
    val loadNextVideo: () -> Unit = {
        if (currentIndex < shorts.size - 1) {
            currentIndex++
        } else {
            currentIndex = 0 // Loop back to the first video
        }
    }

    val loadPreviousVideo: () -> Unit = {
        if (currentIndex > 0) {
            currentIndex--
        } else {
            currentIndex = shorts.size - 1 // Jump to the last video
        }
    }

    // Shake detection to load the next video
    ShakeDetector(
        context = context,
        onShakeDetected = {
            loadNextVideo()
            Toast.makeText(context, "Shake detected! Loading next video...", Toast.LENGTH_SHORT).show()
        }
    )

    // Get the current video
    val currentVideo = shorts.getOrNull(currentIndex)

    // Display the current video
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
fun FullScreenVideoPlayer(
    videoUri: Uri,
    onVideoEnded: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    exoPlayer: ExoPlayer
) {
    val context = LocalContext.current

    // Manage ExoPlayer lifecycle and playback
    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
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
                    onVerticalDrag = { _, dragAmount ->
                        val swipeThreshold = 100f
                        if (dragAmount > swipeThreshold) {
                            onSwipeDown()
                        } else if (dragAmount < -swipeThreshold) {
                            onSwipeUp()
                        }
                    }
                )
            }
    )
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

suspend fun fetchShortVideos(context: Context): List<MediaFile> = withContext(Dispatchers.IO) {
    val mediaFiles = mutableListOf<MediaFile>()
    val contentResolver = context.contentResolver
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATE_MODIFIED
    )

    val selection = "${MediaStore.Video.Media.DURATION} <= ?"
    val selectionArgs = arrayOf("60000") // 60,000 ms (1 minute) duration filter

    contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
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

            // No need to load thumbnails for shorts

            mediaFiles.add(
                MediaFile(
                    id = id,
                    uri = uri,
                    displayName = displayName,
                    duration = duration,
                    size = size,
                    dateModified = dateModified
                    // thumbnail is optional and defaults to null
                )
            )
        }
    }

    mediaFiles
}
