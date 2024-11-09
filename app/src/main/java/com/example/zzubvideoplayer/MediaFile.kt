import android.net.Uri

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val duration: Long,
    val size: Long,
    val dateModified: Long,
    val lastAccessed: Long
)
