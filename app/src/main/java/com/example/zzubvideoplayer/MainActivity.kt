package com.example.zzubvideoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        content = { innerPadding ->
            NavigationGraph(navController = navController, modifier = Modifier.padding(innerPadding))
        }
    )
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    BottomAppBar {
        NavigationBarItem(
            icon = { Text("M") },
            label = { Text("Home") },
            selected = false,
            onClick = { navController.navigate("main") }
        )
        NavigationBarItem(
            icon = { Text("S") },
            label = { Text("Video") },
            selected = false,
            onClick = { navController.navigate("screen2") }
        )
        NavigationBarItem(
            icon = { Text("L") },
            label = { Text("Library") },
            selected = false,
            onClick = { navController.navigate("library") }
        )
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen() }
        composable("screen2") { Screen2() }
        composable("library") { Screen23() }
    }
}

@Composable
fun MainScreen() {
    Text(text = "Welcome to the Main Screen")
}
