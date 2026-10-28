package com.example.minimalplayer

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.content.Context
import android.content.SharedPreferences

data class MediaEntry(val uri: String, val title: String, val isNetwork: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MinimalPlayerApp(onPip = { enterPip() }) }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9)).build()
            enterPictureInPictureMode(params)
        }
    }
}

@Composable
fun MinimalPlayerApp(onPip: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var current by remember { mutableStateOf<MediaEntry?>(null) }
    var search by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var showUrl by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    val history = remember { mutableStateListOf<MediaEntry>() }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val entry = MediaEntry(it.toString(), it.lastPathSegment?.substringAfterLast('/') ?: "Local video", false)
            if (history.none { old -> old.uri == entry.uri }) history.add(0, entry)
            current = entry
        }
    }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            current?.let { media ->
                PlayerHolder.player?.let { player ->
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(it)
                        .setMimeType(guessSubtitleMime(it))
                        .setLanguage("und")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    val item = MediaItem.Builder().setUri(media.uri).setSubtitleConfigurations(listOf(subtitle)).build()
                    player.setMediaItem(item)
                    player.prepare()
                    player.playWhenReady = true
                }
            }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            prefs.edit().putString("folder_uri", it.toString()).apply()
        }
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Minimal Player", style = M
