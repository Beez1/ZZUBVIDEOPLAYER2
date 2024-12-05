
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.zzubvideoplayer.MainActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import java.io.File

// Data classes
data class User(val username: String, val password: String)
data class AuthResponse(val message: String, val token: String? = null)
data class Video(val id: Int, val title: String, val url: String)

// Retrofit API Interface
interface VideoPlatformApi {
    @POST("auth/register")
    suspend fun registerUser(@Body user: User): AuthResponse

    @PUT("auth/update-username")
    suspend fun updateUsername(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): AuthResponse

    @PUT("auth/update-password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): AuthResponse

    @POST("auth/login")
    suspend fun loginUser(@Body user: User): AuthResponse

    @Multipart
    @POST("video/upload")
    suspend fun uploadVideo(
        @Header("Authorization") token: String,
        @Part video: MultipartBody.Part,
        @Part("title") title: RequestBody
    ): AuthResponse

    @GET("video")
    suspend fun fetchAllVideos(@Header("Authorization") token: String): List<Video>

}

// Retrofit API Instance
val api: VideoPlatformApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://video-platform-8lwk.onrender.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().build())
        .build()
        .create(VideoPlatformApi::class.java)
}

// StorageScreen with Login/Signup and UserHomeScreen
@Composable
fun StorageScreen(navController: NavController) {
    var token by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showDialog) {
        AuthDialog(
            api = api,
            onDismiss = {
                // Navigate back to MainActivity on Cancel
                context.startActivity(Intent(context, MainActivity::class.java))
            },
            onTokenReceived = { userToken, userName ->
                token = userToken
                username = userName
                showDialog = false
            }
        )
    }

    // Display UserHomeScreen if logged in
    if (token.isNotEmpty()) {
        UserHomeScreen(
            api = api,
            token = token,
            initialUsername = username,
            onLogout = {
                token = ""
                showDialog = true
            }
        )
    }
}

// Authentication Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    api: VideoPlatformApi,
    onDismiss: () -> Unit,
    onTokenReceived: (String, String) -> Unit
) {
    // Universal color scheme
    val backgroundColor = Color(0xFF121212)
    val primaryColor = Color(0xFF4A90E2)
    val onPrimaryColor = Color.White
    val textColor = Color.White
    val hintColor = Color(0xFF888888)
    val errorColor = Color(0xFFFF5252)
    val buttonColor = primaryColor
    val buttonTextColor = onPrimaryColor

    var isRegistering by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isRegistering) "Sign Up" else "Log In",
                    color = textColor,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Username Input
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    textStyle = TextStyle(color = textColor),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = primaryColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = hintColor,
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = hintColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    textStyle = TextStyle(color = textColor),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = primaryColor,
                                fontSize = 14.sp
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = primaryColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = hintColor,
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = hintColor
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button (Login/Signup)
                Button(
                    onClick = {
                        if (usernameInput.isBlank() || password.isBlank()) {
                            message = "Please fill out all fields"
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (isRegistering) {
                                        val response = api.registerUser(User(usernameInput, password))
                                        message = response.message
                                    } else {
                                        val response = api.loginUser(User(usernameInput, password))
                                        if (response.token != null) {
                                            onTokenReceived(response.token, usernameInput)
                                        } else {
                                            message = response.message
                                        }
                                    }
                                } catch (e: Exception) {
                                    message = "Error: ${e.message}"
                                }
                            }
                            message = null // Clear any previous messages
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonTextColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isRegistering) "Sign Up" else "Log In",
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle between Login and Signup
                TextButton(onClick = { isRegistering = !isRegistering }) {
                    Text(
                        text = if (isRegistering) "Already have an account? Log In" else "Don't have an account? Sign Up",
                        color = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Message Display
                message?.let {
                    Text(
                        text = it,
                        color = if (it.contains("success", true)) Color.Green else errorColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Cancel Button
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = errorColor)
                }
            }
        }
    }
}

// UserHomeScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(api: VideoPlatformApi, token: String, initialUsername: String, onLogout: () -> Unit) {
    // Universal color scheme
    val backgroundColor = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFF4A90E2)
    val onPrimaryColor = Color.White
    val textColor = Color.White

    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var showMenu by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(initialUsername) }

    // Fetch user's videos
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fetchedVideos = api.fetchAllVideos("Bearer $token")
                videos = fetchedVideos
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Videos", color = textColor) },
                actions = {
                    Box {
                        TextButton(onClick = { showMenu = true }) {
                            Text(username, color = textColor)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.Black)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Name") },
                                onClick = {
                                    showMenu = false
                                    showChangeUsernameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Password") },
                                onClick = {
                                    showMenu = false
                                    showChangePasswordDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    onLogout()
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = primaryColor,
                contentColor = onPrimaryColor
            ) {
                Text("+", fontSize = 24.sp)
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Display list of videos
            LazyColumn {
                items(videos) { video ->
                    VideoItem(video)
                }
            }
        }
    }

    // Upload Video Dialog
    if (showUploadDialog) {
        UploadVideoDialog(api = api, token = token, onDismiss = {
            showUploadDialog = false
            // Refresh videos after upload
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val fetchedVideos = api.fetchAllVideos("Bearer $token")
                    videos = fetchedVideos
                } catch (e: Exception) {
                    // Handle error
                }
            }
        })
    }
    if (showChangeUsernameDialog) {
        UpdateUsernameDialog(api = api, token = token, onDismiss = {
            showChangeUsernameDialog = false
        }, onSuccess = { updatedUsername ->
            username = updatedUsername
        })
    }

    if (showChangePasswordDialog) {
        UpdatePasswordDialog(api = api, token = token, onDismiss = {
            showChangePasswordDialog = false
        })
    }
}
fun downloadVideo(context: Context, videoUrl: String, videoTitle: String) {
    val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
        setTitle(videoTitle)
        setDescription("Downloading video...")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$videoTitle.mp4")
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
    Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
}
// VideoItem Composable

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoItem(video: Video) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    val storagePermissionState = rememberPermissionState(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(video.url))
            prepare()
            playWhenReady = false
        }
    }

    // Handle ExoPlayer Lifecycle
    androidx.compose.runtime.DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Pause playback when scrolled out of view
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            exoPlayer.playWhenReady = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(video.title, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)) {

                // Thumbnail and Play Button
                if (!isPlaying) {
                    AsyncImage(
                        model = "${video.url}.jpg", // Assuming thumbnail is at this URL
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                isPlaying = true
                                exoPlayer.playWhenReady = true
                            }
                    )
                }

                // Video Player
                if (isPlaying) {
                    AndroidView(
                        factory = {
                            androidx.media3.ui.PlayerView(context).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Fullscreen Button
                TextButton(
                    onClick = { showFullScreen = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "Fullscreen",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

// Download Button
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            if (storagePermissionState.status.isGranted) {
                                downloadVideo(context, video.url, video.title)
                            } else {
                                storagePermissionState.launchPermissionRequest()
                            }
                        } else {
                            downloadVideo(context, video.url, video.title)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Download",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

            }
        }
    }

    // Fullscreen Video Dialog
    if (showFullScreen) {
        FullScreenVideoDialog(
            videoUrl = video.url,
            onDismiss = { showFullScreen = false }
        )
    }
}


fun rememberPermissionState(readExternalStorage: String) {
    TODO("Not yet implemented")
}

fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

@Composable
fun FullScreenVideoDialog(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    // Handle ExoPlayer Lifecycle
    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AndroidView(
            factory = {
                androidx.media3.ui.PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        )
    }
}

@Composable
fun UpdateUsernameDialog(
    api: VideoPlatformApi,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var newUsername by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Update Username", color = Color.White, fontSize = 20.sp)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("New Username", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newUsername.isBlank()) {
                            message = "Username cannot be empty"
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val response = api.updateUsername(
                                        token = "Bearer $token",
                                        request = mapOf("newUsername" to newUsername)
                                    )
                                    message = response.message
                                    if (response.message.contains("success", true)) {
                                        onSuccess(newUsername)
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    message = "Error: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update")
                }

                message?.let {
                    Text(
                        it,
                        color = if (it.contains("success", true)) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Red)
                }
            }
        }
    }
}
@Composable
fun UpdatePasswordDialog(
    api: VideoPlatformApi,
    token: String,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Update Password", color = Color.White, fontSize = 20.sp)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", color = Color.Gray) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password", color = Color.Gray) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                            message = "All fields are required"
                        } else if (newPassword != confirmPassword) {
                            message = "New passwords do not match"
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val response = api.updatePassword(
                                        token = "Bearer $token",
                                        request = mapOf(
                                            "currentPassword" to currentPassword,
                                            "newPassword" to newPassword
                                        )
                                    )
                                    message = response.message
                                    if (response.message.contains("success", true)) {
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    message = "Error: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update")
                }

                message?.let {
                    Text(
                        it,
                        color = if (it.contains("success", true)) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadVideoDialog(api: VideoPlatformApi, token: String, onDismiss: () -> Unit) {
    // Universal color scheme
    val backgroundColor = Color(0xFF121212)
    val primaryColor = Color(0xFF4A90E2)
    val textColor = Color.White
    val errorColor = Color(0xFFFF5252)

    var videoTitle by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Video Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        videoUri = uri
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Upload Video",
                    color = textColor,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Video Title Input
                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Video Title", color = textColor) },
                    singleLine = true,
                    textStyle = TextStyle(color = textColor),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = primaryColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Video Button
                Button(
                    onClick = {
                        videoPickerLauncher.launch("video/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = textColor
                    )
                ) {
                    Text("Select Video")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Upload Button
                Button(
                    onClick = {
                        if (videoTitle.isBlank() || videoUri == null) {
                            message = "Please provide a title and select a video"
                        } else {
                            // Handle video upload
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val file = FileUtils.getFileFromUri(context, videoUri!!)
                                    val requestFile = file.asRequestBody("video/*".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("video", file.name, requestFile)
                                    val titleRequestBody = videoTitle.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val response = api.uploadVideo(
                                        "Bearer $token",
                                        video = body,
                                        title = titleRequestBody
                                    )
                                    message = response.message
                                    onDismiss()
                                } catch (e: Exception) {
                                    message = "Error: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = textColor
                    )
                ) {
                    Text("Upload")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Message Display
                message?.let {
                    Text(
                        it,
                        color = if (it.contains("success", true)) Color.Green else errorColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Cancel Button
                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel", color = errorColor)
                }
            }
        }
    }
}

// Utility object to get File from Uri
object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "temp_video")
        val outputStream = file.outputStream()
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }
}
