package com.example.zzubvideoplayer.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data class for video file information
data class VideoFile(
    val title: String,
    val filePath: String,
    val size: Long,
    val duration: Long,
    val thumbnail: Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val videoFiles = remember { mutableStateListOf<VideoFile>() }
    var filteredVideos by remember { mutableStateOf<List<VideoFile>>(emptyList()) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var sortCriteria by remember { mutableStateOf("Recently Added") }
    var filterQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Load video files
    LaunchedEffect(Unit) {
        val videos = getAllVideoFiles(context)
        videoFiles.addAll(videos)
        filteredVideos = videos
    }

    // Apply Filter and Sort
    fun applyFilterAndSort() {
        filteredVideos = videoFiles.filter { video ->
            video.title.contains(filterQuery, ignoreCase = true)
        }.sortedWith(
            when (sortCriteria) {
                "Recently Added" -> compareByDescending { it.filePath }
                "File Size" -> compareByDescending { it.size }
                "Length" -> compareByDescending { it.duration }
                else -> compareBy { it.title }
            }
        )
    }

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (isFullscreen && playingUri != null) {
            // Fullscreen video player
            AndroidView(
                factory = { PlayerView(it).apply { player = exoPlayer; useController = true } },
                modifier = Modifier.fillMaxSize()
            ) {
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            playingUri = null
                            isFullscreen = false
                        }
                    }
                })
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title Bar with Filter and Search Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Library Videos",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchVisible) "Hide Search" else "Show Search"
                        )
                    }
                    IconButton(onClick = {
                        // Show filter options (or open filter dialog)
                    }) {

                    }
                }

                // Search Bar (Toggleable)
                AnimatedVisibility(visible = isSearchVisible) {
                    TextField(
                        value = filterQuery,
                        onValueChange = {
                            filterQuery = it
                            applyFilterAndSort()
                        },
                        label = { Text("Search videos") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Spacer for additional padding
                Spacer(modifier = Modifier.height(8.dp))

                // Video Grid
                VideoGrid(
                    videos = filteredVideos,
                    onVideoSelected = { video ->
                        playingUri = video.filePath
                        isFullscreen = true
                        exoPlayer.apply {
                            setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(video.filePath)))
                            prepare()
                            play()
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun FilterButton(
    sortCriteria: String,
    onSortChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(text = sortCriteria)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Recently Added", "File Size", "Length").forEach { criteria ->
                DropdownMenuItem(
                    text = { Text(text = criteria) },
                    onClick = {
                        onSortChanged(criteria)
                        expanded = false
                    }
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
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(videos) { video ->
            VideoThumbnailItem(video = video, onVideoClick = { onVideoSelected(video) })
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
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable { onVideoClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (video.thumbnail != null) {
                Image(
                    bitmap = video.thumbnail.asImageBitmap(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder text for videos without thumbnails
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
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
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
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val filePath = it.getString(filePathIndex)
                val size = it.getLong(sizeIndex)
                val duration = it.getLong(durationIndex)
                val id = it.getLong(idIndex)

                val thumbnail: Bitmap? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                        contentResolver.loadThumbnail(uri, android.util.Size(200, 200), null)
                    } else {
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

                videoList.add(VideoFile(title, filePath, size, duration, thumbnail))
            }
        }
        videoList
    }
}
