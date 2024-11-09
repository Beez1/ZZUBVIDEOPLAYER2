package com.example.zzubvideoplayer.utils

import MediaFile
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi

fun fetchMediaFiles(context: Context, sortMethod: String = MediaStore.Video.Media.DATE_ADDED): List<MediaFile> {
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
        "$sortMethod DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn) * 1000 // Convert to milliseconds
                val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                mediaFiles.add(MediaFile(id, contentUri, name, duration, size, dateModified, lastPlayed = 0L))
            } catch (e: Exception) {
                Log.e("fetchMediaFiles", "Error parsing media file entry: ${e.message}")
            }
        }
    }

    return mediaFiles
}

@RequiresApi(35)
fun saveRecentlyWatched(context: Context, mediaFile: MediaFile) {
    val sharedPreferences = context.getSharedPreferences("recently_watched", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    val currentList = loadRecentlyWatched(context).toMutableList()
    currentList.removeAll { it.uri == mediaFile.uri } // Remove existing entry if present
    currentList.add(0, mediaFile) // Add new entry at the start
    if (currentList.size > 10) currentList.removeLast() // Limit to last 10 items

    val mediaStringSet = currentList.map {
        "${it.id}|${Uri.encode(it.uri.toString())}|${it.displayName}|${it.duration}|${it.size}|${it.dateModified}|${it.lastPlayed}"
    }.toSet()

    editor.putStringSet("media_files", mediaStringSet)
    editor.apply()
}

fun loadRecentlyWatched(context: Context): List<MediaFile> {
    val sharedPreferences = context.getSharedPreferences("recently_watched", Context.MODE_PRIVATE)
    val mediaStringSet = sharedPreferences.getStringSet("media_files", emptySet()) ?: emptySet()

    return mediaStringSet.mapNotNull { entry ->
        try {
            val parts = entry.split("|")
            MediaFile(
                id = parts[0].toLong(),
                uri = Uri.parse(Uri.decode(parts[1])),
                displayName = parts[2],
                duration = parts[3].toLong(),
                size = parts[4].toLong(),
                dateModified = parts[5].toLong(),
                lastPlayed = parts[6].toLong()
            )
        } catch (e: Exception) {
            Log.e("loadRecentlyWatched", "Error parsing recently watched entry: ${e.message}")
            null // Skip entries that can't be parsed
        }
    }
}
