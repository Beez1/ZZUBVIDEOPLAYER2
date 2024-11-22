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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
        videoFiles.addAll(getAllVideoFiles(context))
        filteredVideos = videoFiles
    }

    // Filter and sort logic
    fun applyFilterAndSort() {
        filteredVideos = videoFiles.filter {
            it.title.contains(filterQuery, ignoreCase = true)
        }.sortedWith(
            when (sortCriteria) {
                "Recently Added" -> compareByDescending { it.filePath }
                "File Size" -> compareByDescending { it.size }
                "Length" -> compareByDescending { it.duration }
                else -> compareBy { it.title }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isFullscreen && playingUri != null) {
            // Fullscreen video player
            FullscreenPlayer(exoPlayer = exoPlayer, playingUri = playingUri) {
                playingUri = null
                isFullscreen = false
            }
        } else {
            LibraryContent(
                videoFiles = filteredVideos,
                isSearchVisible = isSearchVisible,
                sortCriteria = sortCriteria,
                filterQuery = filterQuery,
                onSearchToggle = { isSearchVisible = !isSearchVisible },
                onSortChange = {
                    sortCriteria = it
                    applyFilterAndSort()
                },
                onVideoSelect = { video ->
                    playingUri = video.filePath
                    isFullscreen = true
                    exoPlayer.apply {
                        setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(video.filePath)))
                        prepare()
                        play()
                    }
                },
                onQueryChange = {
                    filterQuery = it
                    applyFilterAndSort()
                }
            )
        }
    }
}

@Composable
fun FullscreenPlayer(
    exoPlayer: ExoPlayer,
    playingUri: String?,
    onPlaybackEnd: () -> Unit
) {
    AndroidView(
        factory = { PlayerView(it).apply { player = exoPlayer; useController = true } },
        modifier = Modifier.fillMaxSize()
    ) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onPlaybackEnd()
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    videoFiles: List<VideoFile>,
    isSearchVisible: Boolean,
    sortCriteria: String,
    filterQuery: String,
    onSearchToggle: () -> Unit,
    onSortChange: (String) -> Unit,
    onVideoSelect: (VideoFile) -> Unit,
    onQueryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        // Header with Search and Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Library Videos",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search Videos"
                    )
                }
                // Replace inline filter logic with FilterButton
                FilterButton(sortCriteria = sortCriteria, onSortChanged = onSortChange)
            }
        }

        // Search Bar
        AnimatedVisibility(visible = isSearchVisible) {
            TextField(
                value = filterQuery,
                onValueChange = onQueryChange,
                label = { Text("Search Videos") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        // Video Grid
        VideoGrid(videos = videoFiles, onVideoSelected = onVideoSelect)
    }
}


@Composable
fun FilterButton(sortCriteria: String, onSortChanged: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) } // Manage dropdown visibility

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.List, contentDescription = "Filter Options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false } // Close dropdown when clicking outside
        ) {
            listOf("Recently Added", "File Size", "Length").forEach { criteria ->
                DropdownMenuItem(
                    onClick = {
                        onSortChanged(criteria) // Update sort criteria
                        expanded = false       // Close the dropdown
                    },
                    text = { Text(criteria) }
                )
            }
        }
    }
}



@Composable
fun VideoGrid(videos: List<VideoFile>, onVideoSelected: (VideoFile) -> Unit) {
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
fun VideoThumbnailItem(video: VideoFile, onVideoClick: () -> Unit) {
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
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val filePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex)
                val filePath = cursor.getString(filePathIndex)
                val size = cursor.getLong(sizeIndex)
                val duration = cursor.getLong(durationIndex)

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
                    null
                }

                videoList.add(VideoFile(title, filePath, size, duration, thumbnail))
            }
        }
        videoList
    }
}
