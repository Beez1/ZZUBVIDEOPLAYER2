package com.example.zzubvideoplayer

import StorageScreen
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zzubvideoplayer.screens.LibraryScreen
import com.example.zzubvideoplayer.ui.theme.DarkGrayBackground
import com.example.zzubvideoplayer.ui.theme.LightGray
import com.example.zzubvideoplayer.ui.theme.SoftBlueAccent
import com.example.zzubvideoplayer.ui.theme.White
import com.example.zzubvideoplayer.ui.theme.ZZUBVIDEOPLAYERTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class MediaFile(
    val id: Long,
    val uri: android.net.Uri,
    val displayName: String,
    val duration: Long,
    val size: Long,
    val dateModified: Long,
    val thumbnail: android.graphics.Bitmap? = null // Optional thumbnail
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

    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(shouldShowInfoDialog(context)) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            showPermissionDialog = true
        }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Main content if permission is granted
    if (hasPermission) {
        var videos by remember { mutableStateOf<List<MediaFile>>(emptyList()) }

        // Load videos asynchronously
        LaunchedEffect(Unit) {
            videos = getAllVideoFiles(context)
        }

        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController = navController)
            },
            containerColor = DarkGrayBackground
        ) { innerPadding ->
            NavigationGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                videos = videos,
                exoPlayer = exoPlayer
            )
        }
    } else {
        // Show permission rationale if permission is denied
        if (showPermissionDialog) {
            PermissionRationaleDialog(
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                },
                onDismiss = {
                    showPermissionDialog = false
                }
            )
        }
    }

    // Show Info Dialog
    if (showInfoDialog) {
        InfoDialog(
            onDismiss = { showInfoDialog = false },
            onDontShowAgain = {
                setDontShowInfoDialog(context, true)
                showInfoDialog = false
            }
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

@Composable
fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGrayBackground, shape = RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission Required",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This app requires access to your media files to display your videos. Please grant the permission.",
                    color = LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SoftBlueAccent)
                    }
                    TextButton(onClick = onRequestPermission) {
                        Text("Grant Permission", color = SoftBlueAccent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    videos: List<MediaFile>,
    exoPlayer: ExoPlayer
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            AnimatedScreen {
                HomeScreen(videos = videos, exoPlayer = exoPlayer, navController = navController)
            }
        }
        composable("shorts") {
            AnimatedScreen {
                ShortsScreen(exoPlayer = exoPlayer)
            }
        }
        composable("library") {
            AnimatedScreen { LibraryScreen() }
        }
        composable("storage") {
            AnimatedScreen { StorageScreen(navController = navController) }
        }
    }
}
fun setDontShowInfoDialog(context: Context, value: Boolean) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putBoolean("show_info_dialog", !value).apply()
}

fun shouldShowInfoDialog(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("show_info_dialog", true)
}
@Composable
fun InfoDialog(
    onDismiss: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "App Navigation Guide",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoItem(
                    title = "Home Screen",
                    description = "View recently added videos and navigate to the Cloud+."
                )
                InfoItem(
                    title = "Shorts Screen",
                    description = "Watch short videos seamlessly with shake or swipe navigation."
                )
                InfoItem(
                    title = "Library Screen",
                    description = "Access your library to manage and view all videos."
                )
                InfoItem(
                    title = "Cloud+",
                    description = "Upload and manage your videos in the cloud."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = SoftBlueAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDontShowAgain) {
                Text("Don't Show Again", color = SoftBlueAccent)
            }
        },
        containerColor = DarkGrayBackground
    )
}

@Composable
fun InfoItem(title: String, description: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
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
fun HomeScreen(videos: List<MediaFile>, exoPlayer: ExoPlayer, navController: NavHostController) {
    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // BackHandler to exit full-screen mode when back is pressed
    androidx.activity.compose.BackHandler(enabled = selectedVideoUri != null) {
        selectedVideoUri = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        selectedVideoUri?.let { uri ->
            FullScreenVideoPlayer(
                videoUri = uri,
                onVideoEnded = { selectedVideoUri = null },
                exoPlayer = exoPlayer
            )
        }

        if (selectedVideoUri == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Recently Added Videos",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )

                    // Navigate to Storage Screen
                    Text(
                        text = "Cloud+", // Replace with desired label
                        fontSize = 14.sp,       // Adjust font size as needed
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("storage") // Navigate to Storage screen
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp) // Add padding for touch area
                    )

                }

                if (videos.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos) { mediaFile ->
                            AnimatedCard(
                                mediaFile = mediaFile,
                                onClick = {
                                    selectedVideoUri = mediaFile.uri
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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
fun FullScreenVideoPlayer(
    videoUri: android.net.Uri,
    onVideoEnded: () -> Unit,
    exoPlayer: ExoPlayer
) {
    val context = LocalContext.current

    // Manage ExoPlayer lifecycle and playback
    DisposableEffect(videoUri) {
        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
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
    androidx.compose.ui.viewinterop.AndroidView(
        factory = {
            androidx.media3.ui.PlayerView(context).apply {
                player = exoPlayer
                useController = false // Hide default video controls
            }
        },
        modifier = Modifier.fillMaxSize()
    )
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
                val formattedDate = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(mediaFile.dateModified))
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
        if (mediaFile.thumbnail != null) {
            androidx.compose.foundation.Image(
                bitmap = mediaFile.thumbnail.asImageBitmap(),
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Video",
                tint = White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

suspend fun getAllVideoFiles(context: Context): List<MediaFile> = withContext(Dispatchers.IO) {
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
            val uri = android.net.Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
            val displayName = cursor.getString(displayNameColumn)
            val duration = cursor.getLong(durationColumn)
            val size = cursor.getLong(sizeColumn)
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000

            // Load thumbnail
            val thumbnail: android.graphics.Bitmap? = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val thumbnailSize = android.util.Size(200, 200)
                    context.contentResolver.loadThumbnail(uri, thumbnailSize, null)
                } else {
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (e: Exception) {
                null
            }

            mediaFiles.add(MediaFile(id, uri, displayName, duration, size, dateModified, thumbnail))
            count++
        }
    }

    mediaFiles
}