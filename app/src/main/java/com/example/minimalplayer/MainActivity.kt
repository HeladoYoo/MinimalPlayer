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
                        Text("Minimal Player", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text("Your videos, beautifully simple.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (dark) "Dark" else "Light", style = MaterialTheme.typography.labelMedium)
                        Switch(checked = dark, onCheckedChange = { dark = it; prefs.edit().putBoolean("dark", it).apply() })
                    }
                }

                if (current != null) {
                    val entry = current!!
                    val player = remember(entry.uri) {
                        PlayerHolder.release()
                        ExoPlayer.Builder(context).build().also { exo ->
                            PlayerHolder.player = exo
                            exo.setMediaItem(MediaItem.fromUri(entry.uri))
                            val saved = prefs.getLong("pos:${entry.uri}", 0L)
                            exo.seekTo(saved)
                            exo.prepare()
                            exo.playWhenReady = true
                            exo.addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    if (playbackState == Player.STATE_ENDED) prefs.edit().putLong("pos:${entry.uri}", 0L).apply()
                                }
                            })
                        }
                    }
                    DisposableEffect(player) {
                        onDispose {
                            prefs.edit().putLong("pos:${entry.uri}", player.currentPosition).apply()
                            player.release()
                            if (PlayerHolder.player === player) PlayerHolder.player = null
                        }
                    }
                    Card(shape = RoundedCornerShape(24.dp)) {
                        Column {
                            AndroidView(
                                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 300.dp)
                            )
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showSpeed = true }) { Text("${speed}×") }
                                OutlinedButton(onClick = { subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream")) }) { Text("字幕") }
                                OutlinedButton(onClick = onPip) { Text("画中画") }
                                OutlinedButton(onClick = { current = null }) { Text("关闭") }
                            }
                        }
                    }
                    LaunchedEffect(speed, player) { player.playbackParameters = PlaybackParameters(speed) }
                } else {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("开始播放", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("选择设备中的视频，或粘贴一个可直接播放的视频地址。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Text("选择视频") }
                                OutlinedButton(onClick = { showUrl = true }) { Text("网络地址") }
                                OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("选择文件夹") }
                            }
                        }
                    }
                }

                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索最近播放") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("最近播放", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { history.sortBy { it.title.lowercase() } }) { Text("按名称排序") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history.filter { it.title.contains(search, ignoreCase = true) }, key = { it.uri }) { item ->
                        Row(
                            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                .clickable { current = item }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Medium)
                                Text(if (item.isNetwork) "网络视频" else "本地视频", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("播放  ›", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showSpeed) {
        AlertDialog(
            onDismissRequest = { showSpeed = false },
            title = { Text("播放速度") },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f).forEach { value ->
                        TextButton(onClick = { speed = value; showSpeed = false }) {
                            Text(if (speed == value) "✓  ${value}×" else "${value}×")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSpeed = false }) { Text("完成") } }
        )
    }
    if (showUrl) {
        AlertDialog(
            onDismissRequest = { showUrl = false },
            title = { Text("播放网络视频") },
            text = {
                OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("https://…") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = urlInput.trim()
                    if (url.startsWith("https://") || url.startsWith("http://")) {
                        val entry = MediaEntry(url, url.substringAfterLast('/').ifBlank { url }, true)
                        if (history.none { it.uri == entry.uri }) history.add(0, entry)
                        current = entry
                        showUrl = false
                        urlInput = ""
                    }
                }) { Text("播放") }
            },
            dismissButton = { TextButton(onClick = { showUrl = false }) { Text("取消") } }
        )
    }
}

private object PlayerHolder {
    var player: ExoPlayer? = null
    fun release() { player?.release(); player = null }
}

private fun guessSubtitleMime(uri: Uri): String {
    val name = uri.lastPathSegment?.lowercase().orEmpty()
    return when {
        name.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        name.endsWith(".ttml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

private object C { const val SELECTION_FLAG_DEFAULT = 1 }
