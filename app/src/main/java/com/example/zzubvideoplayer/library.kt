// File: LibraryScreen.kt
package com.example.zzubvideoplayer.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data class representing each video file
data class VideoFile(
    val title: String,
    val filePath: String,
    val thumbnail: Bitmap?
)




@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoFiles = remember { mutableStateListOf<VideoFile>() }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Permissions handling
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_VIDEO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, storagePermission) == PermissionChecker.PERMISSION_GRANTED

    // Request permissions and load videos when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            videoFiles.addAll(getAllVideoFiles(context))
        } else {
            // Optionally, you can show rationale or request permission here
        }
    }

    // Handle ExoPlayer lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.stop()
                    playingUri = null
                    isFullscreen = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (isFullscreen && playingUri != null) {
            // Fullscreen Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // Optional: Exit fullscreen on tap outside controls
                        isFullscreen = false
                        exoPlayer.pause()
                    }
            )
        } else {
            // Video Grid
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Video Library",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasPermission) {
                    VideoGrid(
                        videos = videoFiles,
                        onVideoSelected = { video ->
                            playingUri = video.filePath
                            isFullscreen = true
                            exoPlayer.apply {
                                setMediaItem(MediaItem.fromUri(Uri.parse(video.filePath)))
                                prepare()
                                play()
                            }
                        }
                    )
                } else {
                    // Permission Not Granted UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Storage permission is required to display videos.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            // Request permission here
                            Toast.makeText(context, "Please grant storage permission", Toast.LENGTH_SHORT).show()
                        }) {
                            Text(text = "Grant Permission")
                        }
                    }
                }
            }
        }

        // Optional: Display message if no videos are found
        if (videoFiles.isEmpty() && hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No videos found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun VideoGrid(
    videos: List<VideoFile>,
    onVideoSelected: (VideoFile) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5), // 5 columns
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(videos) { video ->
            VideoThumbnailItem(
                video = video,
                onVideoClick = { onVideoSelected(video) }
            )
        }
    }
}

@Composable
fun VideoThumbnailItem(
    video: VideoFile,
    onVideoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(2.dp) // Minimal padding for compact grid
            .aspectRatio(1f) // Ensures the item is square
            .clickable(onClick = onVideoClick),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (video.thumbnail != null) {
                // Display the thumbnail
                Image(
                    bitmap = video.thumbnail.asImageBitmap(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Optional: Use Coil to load image from filePath if thumbnails aren't available
                // Uncomment below lines if you have Coil integrated and prefer to load images dynamically
                /*
                val painter = rememberAsyncImagePainter(
                    model = video.filePath,
                    builder = {
                        crossfade(true)
                        placeholder(R.drawable.placeholder)
                        error(R.drawable.error)
                        size(200) // Adjust size as needed
                    }
                )
                Image(
                    painter = painter,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize()
                )
                */

                // Fallback UI for videos without thumbnails
                Text(
                    text = "No Thumbnail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

suspend fun getAllVideoFiles(context: Context): List<VideoFile> {
    return withContext(Dispatchers.IO) {
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
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val filePath = it.getString(filePathIndex)
                val id = it.getLong(idIndex)

                // Fetch thumbnail using ContentResolver.loadThumbnail (API 29+)
                val thumbnail: Bitmap? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                        contentResolver.loadThumbnail(uri, android.util.Size(200, 200), null)
                    } else {
                        // For API levels below 29, use MediaStore.Video.Thumbnails.getThumbnail
                        MediaStore.Video.Thumbnails.getThumbnail(
                            contentResolver,
                            id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                videoList.add(VideoFile(title, filePath, thumbnail))
            }
        }
        videoList
    }
}
