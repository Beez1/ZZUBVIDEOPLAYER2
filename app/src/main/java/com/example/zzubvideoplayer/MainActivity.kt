package com.example.zzubvideoplayer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zzubvideoplayer.screens.LibraryScreen
import com.example.zzubvideoplayer.ui.theme.ZZUBVIDEOPLAYERTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkGrayBackground = Color(0xFF121212)
private val SoftBlueAccent = Color(0xFF4A90E2)
private val LightGray = Color(0xFFA0A0A0)
private val White = Color.White

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val duration: Long,
    val size: Long,
    val dateModified: Long
)

class MainActivity : ComponentActivity() {
    private lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ExoPlayer instance
        exoPlayer = ExoPlayer.Builder(this).build()

        setContent {
            ZZUBVIDEOPLAYERTheme {
                MainApp(exoPlayer = exoPlayer)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop and release ExoPlayer when the app is no longer visible
        exoPlayer.stop()
        exoPlayer.release()
    }
}

@Composable
fun MainApp(exoPlayer: ExoPlayer) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val videos = remember { fetchShortVideos(context) }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = DarkGrayBackground
    ) { innerPadding ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            videos = videos,
            exoPlayer = exoPlayer
        )
    }
}

@Composable
fun PlayWithLightningIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play Icon",
            tint = color,
            modifier = Modifier
                .offset(x = 4.dp, y = 4.dp) // Adjust position for overlap
        )
        Icon(
            imageVector = Icons.Filled.PlayArrow, // Background icon
            contentDescription = "Flash Effect",
            tint = color.copy(alpha = 0.5f), // Semi-transparent effect
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("home") }
    val items = listOf(
        NavigationItem("home", Icons.Default.Home, "Home"),
        NavigationItem("shorts", Icons.Default.Home, "Shorts"), // Placeholder icon for Shorts
        NavigationItem("library", Icons.Default.List, "Library")
    )

    NavigationBar(containerColor = DarkGrayBackground) {
        items.forEach { item ->
            val isSelected = selectedItem == item.route
            val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f)
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) SoftBlueAccent else White
            )

            NavigationBarItem(
                icon = {
                    if (item.route == "shorts") {
                        // Use custom PlayWithLightningIcon for Shorts
                        PlayWithLightningIcon(
                            color = iconColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    } else {
                        // Default icons for other items
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        color = iconColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = isSelected,
                onClick = {
                    selectedItem = item.route
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val route: String, val icon: ImageVector, val label: String)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier, videos: List<MediaFile>, exoPlayer: ExoPlayer) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") { AnimatedScreen { HomeScreen() } }
        composable("shorts") { ShortsScreen(videos = videos, exoPlayer = exoPlayer, context = context) }
        composable("library") { AnimatedScreen { LibraryScreen() } }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedScreen(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        content()
    }
}
@Composable
fun FullScreenVideoPlayerr(videoUri: Uri, onVideoEnded: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        })

        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { PlayerView(context).apply { player = exoPlayer; useController = false } },
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val recentlyAccessedFiles = remember { mutableStateListOf<MediaFile>() }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        val files = fetchRecentlyAccessedMediaFiles(context)
        recentlyAccessedFiles.clear()
        recentlyAccessedFiles.addAll(files)
    }

    // BackHandler to exit full-screen mode when back is pressed
    BackHandler(enabled = selectedVideoUri != null) {
        selectedVideoUri = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        selectedVideoUri?.let { uri ->
            FullScreenVideoPlayerr(videoUri = uri, onVideoEnded = { selectedVideoUri = null })
        }

        if (selectedVideoUri == null) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Recently Added Videos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (recentlyAccessedFiles.isNotEmpty()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentlyAccessedFiles) { mediaFile ->
                            AnimatedCard(
                                mediaFile = mediaFile,
                                onClick = {
                                    selectedVideoUri = Uri.parse(mediaFile.uri.toString())
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SoftBlueAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCard(mediaFile: MediaFile, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale
            }
            .clickable { onClick() }, // Trigger the onClick function when the card is clicked
        colors = CardDefaults.cardColors(containerColor = DarkGrayBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnail(mediaFile)
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mediaFile.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(mediaFile.dateModified))
                Text(
                    text = "Recently Added: $formattedDate",
                    fontSize = 14.sp,
                    color = LightGray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Size",
                        tint = LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${mediaFile.size / (1024 * 1024)} MB",
                        fontSize = 12.sp,
                        color = LightGray
                    )
                }
            }
        }
    }
}


@Composable
fun VideoThumbnail(mediaFile: MediaFile) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkGrayBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Video",
            tint = White,
            modifier = Modifier.size(40.dp)
        )
    }
}

fun fetchRecentlyAccessedMediaFiles(context: Context): List<MediaFile> {
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
        null,
        null,
        MediaStore.Video.Media.DATE_MODIFIED + " DESC" // Sort by recent first
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

        var count = 0
        while (cursor.moveToNext() && count < 10) { // Limit to 10 items
            val id = cursor.getLong(idColumn)
            val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
            val displayName = cursor.getString(displayNameColumn)
            val duration = cursor.getLong(durationColumn)
            val size = cursor.getLong(sizeColumn)
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000

            mediaFiles.add(MediaFile(id, uri, displayName, duration, size, dateModified))
            count++
        }
    }

    return mediaFiles
}