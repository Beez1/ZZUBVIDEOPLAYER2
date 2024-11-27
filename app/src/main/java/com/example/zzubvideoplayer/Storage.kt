package com.example.zzubvideoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.zzubvideoplayer.ui.theme.ZZUBVIDEOPLAYERTheme

class StorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZZUBVIDEOPLAYERTheme {
                val navController = rememberNavController()
                StorageScreen(navController)
            }
        }
    }
}

@Composable
fun StorageScreen(navController: NavController) {
    var showDialog by remember { mutableStateOf(true) } // State to manage dialog visibility
    var isSignedIn by remember { mutableStateOf(false) } // State to check if the user is signed in

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isSignedIn && showDialog) {
            // Display dialog if the user is not signed in
            LoginDialog(
                onDismiss = {
                    showDialog = false
                    navController.navigate("home") // Navigate back to the home page
                },
                onSignIn = {
                    isSignedIn = true
                    showDialog = false // Close dialog on successful sign-in
                }
            )
        }

        // Main content for signed-in users
        if (isSignedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Videos",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Video",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                // Logic to add a video (placeholder for now)
                            }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder for video list
                Text(
                    text = "No videos uploaded yet.",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun LoginDialog(onDismiss: () -> Unit, onSignIn: () -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Sign In Required",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (email.text.isEmpty()) {
                            Text(
                                text = "Enter email",
                                color = Color.Gray,
                                textAlign = TextAlign.Start
                            )
                        }
                        innerTextField()
                    }
                )
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (password.text.isEmpty()) {
                            Text(
                                text = "Enter password",
                                color = Color.Gray,
                                textAlign = TextAlign.Start
                            )
                        }
                        innerTextField()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSignIn() // Handle successful sign-in
                }
            ) {
                Text(text = "Sign In")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() } // Handle cancel action
            ) {
                Text(text = "Cancel")
            }
        }
    )
}
