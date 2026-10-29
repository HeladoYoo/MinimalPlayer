package com.example.minimalplayer // 请根据你原本文件的包名修改这里

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinimalPlayerTheme {
                ModernHomeScreen()
            }
        }
    }
}

// 现代毛玻璃主界面
@Composable
fun ModernHomeScreen() {
    // 底部粉橙蓝弥散渐变背景（复刻你喜欢的高级感）
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // 自动避开手机顶部状态栏，防止重叠
        ) {
            // 顶部标题
            Text(
                text = "MinimalPlayer",
                fontSize = 26.sp,
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
            )

            // 网格分类卡片区
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { GlassCard(title = "本地电影", subtitle = "4K / 高清专区", count = "视频列表") }
                item { GlassCard(title = "最近播放", subtitle = "断点续播记录", count = "历史记录") }
                item { GlassCard(title = "我的收藏", subtitle = "精选喜爱视频", count = "快捷访问") }
                item { GlassCard(title = "设置中心", subtitle = "外观与解码偏好", count = "个性化") }
            }
        }
    }
}

// 半透明毛玻璃卡片组件
@Composable
fun GlassCard(title: String, subtitle: String, count: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.5f)) // 半透明白毛玻璃
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

@Composable
fun MinimalPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
