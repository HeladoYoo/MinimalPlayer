package com.example.minimalplayer // 请保留你原本的包名

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 动态申请读取本地视频文件的权限
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_VIDEO),
            100
        )

        setContent {
            MinimalPlayerTheme {
                MainAppNavigator()
            }
        }
    }
}

// 简单的页面导航状态：Home 代表主页，VideoList 代表视频列表页
enum class Screen {
    Home, VideoList
}

@Composable
fun MainAppNavigator() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    // 弥散渐变背景
    val meshBackgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFC0CB), // 浅粉色
            Color(0xFFFFDAB9), // 柔橙色
            Color(0xFFE0ECFF)  // 淡蓝色
        ),
        center = Offset(300f, 400f),
        radius = 1600f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(meshBackgroundBrush)
            .padding(16.dp)
    ) {
        when (currentScreen) {
            Screen.Home -> ModernHomeScreen(
                onCardClick = { 
                    // 点击“本地电影”时切换到视频列表页
                    currentScreen = Screen.VideoList 
                }
            )
            Screen.VideoList -> VideoListScreen(
                onBackClick = { 
                    // 返回主页
                    currentScreen = Screen.Home 
                }
            )
        }
    }
}

// 现代毛玻璃主界面
@Composable
fun ModernHomeScreen(onCardClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "MinimalPlayer",
            fontSize = 26.sp,
            color = Color(0xFF1D1B20),
            modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { 
                GlassCard(
                    title = "本地电影", 
                    subtitle = "4K / 高清专区", 
                    count = "视频列表",
                    onClick = onCardClick // 绑定点击事件
                ) 
            }
            item { GlassCard(title = "最近播放", subtitle = "断点续播记录", count = "历史记录", onClick = {}) }
            item { GlassCard(title = "我的收藏", subtitle = "精选喜爱视频", count = "快捷访问", onClick = {}) }
            item { GlassCard(title = "设置中心", subtitle = "外观与解码偏好", count = "个性化", onClick = {}) }
        }
    }
}

// 半透明毛玻璃卡片（支持点击）
@Composable
fun GlassCard(title: String, subtitle: String, count: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() } // 点击响应
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 17.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.sp, color = Color.DarkGray)
            }
            Text(text = count, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// 本地视频列表页（读取手机里的视频并展示）
@Composable
fun VideoListScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var videoList by remember { mutableStateOf(listOf<VideoItem>()) }

    // 扫描手机本地视频
    LaunchedEffect(Unit) {
        val list = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val path = it.getString(pathIndex)
                list.add(VideoItem(name, path))
            }
        }
        videoList = list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 顶部标题与返回按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "本地视频库", fontSize = 22.sp, color = Color.Black)
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.6f))
            ) {
                Text(text = "返回", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 视频列表展示
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(videoList) { video ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.45f))
                        .clickable {
                            // 点击视频项直接调用系统播放器播放
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse("file://${video.path}"), "video/*")
                            }
                            context.startActivity(intent)
                        }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = video.name, fontSize = 15.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "点击直接播放", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

data class VideoItem(val name: String, val path: String)

@Composable
fun MinimalPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
