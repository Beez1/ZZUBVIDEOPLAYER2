
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Data classes
data class User(val username: String, val password: String)
data class AuthResponse(val message: String, val token: String? = null)

// Retrofit API Interface
interface VideoPlatformApi {
    @POST("auth/register")
    suspend fun registerUser(@Body user: User): AuthResponse

    @POST("auth/login")
    suspend fun loginUser(@Body user: User): AuthResponse
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

// StorageScreen with only Login/Signup functionality
@Composable
fun StorageScreen(navController: NavController) {
    var token by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    if (showDialog) {
        AuthDialog(
            api = api,
            onDismiss = {
                // Navigate back to MainActivity on Cancel
                context.startActivity(Intent(context, MainActivity::class.java))
            },
            onTokenReceived = { userToken ->
                token = userToken
                showDialog = false
                message = "Login successful!"
                // Since we are only focusing on login/signup, no further actions are taken
            }
        )
    }

    // Display a simple welcome message if logged in
    if (token.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome!",
                color = Color.White,
                fontSize = 24.sp
            )
        }
    }
}

// Authentication Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    api: VideoPlatformApi,
    onDismiss: () -> Unit,
    onTokenReceived: (String) -> Unit
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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

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
                    text = if (isRegistering) "Sign Up" else "Log In",
                    color = textColor,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Username Input
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = primaryColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = hintColor,
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = hintColor
                    )
                )

                // Show/Hide Password Button
                TextButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (passwordVisible) "Hide Password" else "Show Password",
                        color = primaryColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button (Login/Signup)
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            message = "Please fill out all fields"
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (isRegistering) {
                                        val response = api.registerUser(User(username, password))
                                        message = response.message
                                    } else {
                                        val response = api.loginUser(User(username, password))
                                        if (response.token != null) {
                                            onTokenReceived(response.token)
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
