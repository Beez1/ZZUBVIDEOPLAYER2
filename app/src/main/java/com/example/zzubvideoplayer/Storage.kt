
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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

data class User(val username: String, val password: String)
data class AuthResponse(val message: String, val token: String? = null)
data class Video(val title: String, val url: String)

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
        @Part("title") title: MultipartBody.Part
    ): AuthResponse

    @GET("video")
    suspend fun fetchVideos(@Header("Authorization") token: String): List<Video>
}

val api: VideoPlatformApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://video-platform-8lwk.onrender.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().build())
        .build()
        .create(VideoPlatformApi::class.java)
}

@Composable
fun StorageScreen(navController: NavController) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var token by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        videoUri = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Registration Section
        Text("Register", color = Color.White, fontSize = 20.sp)
        BasicTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.2f))
                .padding(8.dp),
            decorationBox = { innerTextField ->
                if (username.text.isEmpty()) Text("Username", color = Color.Gray) else innerTextField()
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
                if (password.text.isEmpty()) Text("Password", color = Color.Gray) else innerTextField()
            }
        )
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.registerUser(User(username.text, password.text))
                    message = response.message
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                }
            }
        }) {
            Text("Register")
        }

        // Login Section
        Text("Login", color = Color.White, fontSize = 20.sp)
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.loginUser(User(username.text, password.text))
                    token = response.token ?: ""
                    message = response.message
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                }
            }
        }) {
            Text("Login")
        }

        // Upload Section
        Text("Upload Video", color = Color.White, fontSize = 20.sp)
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.2f))
                .padding(8.dp),
            decorationBox = { innerTextField ->
                if (title.text.isEmpty()) Text("Video Title", color = Color.Gray) else innerTextField()
            }
        )
        Button(onClick = { filePickerLauncher.launch("video/*") }) {
            Text("Pick Video")
        }
        Button(onClick = {
            if (videoUri != null && token.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val file = File(videoUri!!.path!!)
                        val videoPart = MultipartBody.Part.createFormData(
                            "video", file.name, file.asRequestBody("video/*".toMediaTypeOrNull())
                        )
                        val titlePart = MultipartBody.Part.createFormData(
                            "title", title.text
                        )
                        val response = api.uploadVideo("Bearer $token", videoPart, titlePart)
                        message = response.message
                    } catch (e: Exception) {
                        message = "Error: ${e.message}"
                    }
                }
            } else {
                message = "Please log in and pick a video"
            }
        }) {
            Text("Upload")
        }

        // Fetch Videos Section
        Text("Your Videos", color = Color.White, fontSize = 20.sp)
        Button(onClick = {
            if (token.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        videos = api.fetchVideos("Bearer $token")
                    } catch (e: Exception) {
                        message = "Error: ${e.message}"
                    }
                }
            } else {
                message = "Please log in"
            }
        }) {
            Text("Fetch Videos")
        }
        Column {
            videos.forEach { video ->
                Text(
                    text = "${video.title}: ${video.url}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Message Display
        message?.let {
            Text(it, color = if (it.contains("success", true)) Color.Green else Color.Red)
        }
    }
}
