

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.zzubvideoplayer.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
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

    @POST("auth/login")
    suspend fun loginUser(@Body user: User): AuthResponse

    @Multipart
    @POST("video/upload")
    suspend fun uploadVideo(
        @Header("Authorization") token: String,
        @Part video: MultipartBody.Part,
        @Part("title") title: String
    ): AuthResponse

    @GET("video/user")
    suspend fun fetchUserVideos(@Header("Authorization") token: String): List<Video>
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
        UserHomeScreen(api = api, token = token, username = username, onLogout = {
            token = ""
            showDialog = true
        })
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
fun UserHomeScreen(api: VideoPlatformApi, token: String, username: String, onLogout: () -> Unit) {
    // Universal color scheme
    val backgroundColor = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFF4A90E2)
    val onPrimaryColor = Color.White
    val textColor = Color.White

    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var showMenu by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }

    // Fetch user's videos
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fetchedVideos = api.fetchUserVideos("Bearer $token")
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
                                    // Change Name functionality (to be implemented)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Password") },
                                onClick = {
                                    // Change Password functionality (to be implemented)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    // Logout
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
                    val fetchedVideos = api.fetchUserVideos("Bearer $token")
                    videos = fetchedVideos
                } catch (e: Exception) {
                    // Handle error
                }
            }
        })
    }
}

// VideoItem Composable
@Composable
fun VideoItem(video: Video) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(video.title, color = Color.White, fontSize = 18.sp)
            // You can add more video details here
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
                    // Remove colors parameter
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
                                    val response = api.uploadVideo(
                                        "Bearer $token",
                                        video = body,
                                        title = videoTitle
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
