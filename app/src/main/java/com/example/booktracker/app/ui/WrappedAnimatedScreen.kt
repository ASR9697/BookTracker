package com.example.booktracker.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.booktracker.util.gif.AnimatedGifEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun WrappedAnimatedScreen(
    booksFinished: Int = 12,
    pagesRead: Int = 3450,
    topGenre: String = "Science Fiction"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isExporting by remember { mutableStateOf(false) }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "wrapped_anim")
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bg_offset"
    )
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "float_anim"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The exportable Canvas area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .padding(32.dp)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6200EE),
                            Color(0xFF03DAC5),
                            Color(0xFF6200EE)
                        ),
                        start = androidx.compose.ui.geometry.Offset(backgroundOffset, 0f),
                        end = androidx.compose.ui.geometry.Offset(backgroundOffset + 500f, 0f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = floatAnim.dp)
            ) {
                Text(
                    "Your Reading Year",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                WrappedStatBox("Books Finished", booksFinished.toString())
                Spacer(modifier = Modifier.height(16.dp))
                WrappedStatBox("Pages Read", pagesRead.toString())
                Spacer(modifier = Modifier.height(16.dp))
                WrappedStatBox("Top Genre", topGenre)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isExporting) {
            CircularProgressIndicator()
            Text("Generating Animated GIF...", modifier = Modifier.padding(top = 16.dp))
        } else {
            Button(
                onClick = {
                    isExporting = true
                    scope.launch {
                        try {
                            val gifFile = withContext(Dispatchers.IO) {
                                val file = File(context.cacheDir, "wrapped.gif")
                                val os = FileOutputStream(file)
                                val encoder = AnimatedGifEncoder()
                                encoder.start(os)
                                encoder.setDelay(100) // 10 fps
                                encoder.setRepeat(0) // loop infinite
                                
                                // Capture 20 frames (2 seconds)
                                for (i in 0 until 20) {
                                    // Delay to let Compose render the next frame of animation
                                    delay(100)
                                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    // Ensure it's in ARGB_8888 for the encoder
                                    val safeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    encoder.addFrame(safeBitmap)
                                    safeBitmap.recycle()
                                }
                                encoder.finish()
                                os.close()
                                file
                            }

                            // Share the GIF
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                gifFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/gif"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Wrapped"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export as Animated GIF")
            }
        }
    }
}

@Composable
fun WrappedStatBox(label: String, value: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp).width(200.dp)
        ) {
            Text(label, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
