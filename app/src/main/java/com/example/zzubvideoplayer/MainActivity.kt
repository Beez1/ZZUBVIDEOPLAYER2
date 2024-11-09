package com.example.zzubvideoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.example.zzubvideoplayer.screens.VideoFile
import com.example.zzubvideoplayer.screens.VideoItem
import com.example.zzubvideoplayer.ui.theme.ZZUBVIDEOPLAYERTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZZUBVIDEOPLAYERTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavigationGraph(navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    var selectedItem by remember { mutableStateOf("home") }

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.inversePrimary
    ) {
        val items = listOf(
            NavigationItem("home", Icons.Default.Home, "Home"),
            NavigationItem("library", Icons.Default.List, "Library")
        )

        items.forEach { item ->
            val isSelected = selectedItem == item.route
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor
                    )
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
                    }
                }
            )
        }
    }
}

data class NavigationItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("library") { LibraryScreen() }
    }
}




@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val recentlyWatched = remember { mutableStateListOf<VideoFile>() }
    val exoPlayer = remember { 
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }
    var playingUri by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Recently Watched",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (playingUri != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {

                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }

        LazyColumn {
            items(recentlyWatched) { video ->
                VideoItem(
                    video = video,
                    isPlaying = video.filePath == playingUri,
                    onVideoClick = {
                        if (playingUri != video.filePath) {
                            playingUri = video.filePath
                            exoPlayer.apply {
                                setMediaItem(MediaItem.fromUri(video.filePath))
                                prepare()
                            }
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}
