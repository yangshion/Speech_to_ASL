package com.example.Speech2ASL.utils.video

import android.content.Context
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import com.example.Speech2ASL.databinding.ActivityMainBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.*
import com.example.Speech2ASL.utils.speech.SpeechHelper
/**
 * 视频助手类，管理视频下载和播放
 */
class VideoHelper(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val speechHelper: SpeechHelper
) {
    /**
     * 下载视频并创建视频序列（优化版）
     * @param videoUrls 视频URL列表
     * @return 下载后的本地文件路径列表
     */
    suspend fun downloadVideosAndCreateSequence(
        videoUrls: List<String>,
        onProgressUpdate: (Int) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "asl_videos").apply { mkdirs() }

        // 创建一个可变列表来存储下载成功的视频路径
        val localPaths = mutableListOf<String>()

        // 遍历所有视频URL
        videoUrls.forEachIndexed { index, url ->
            try {
                Log.d("VideoDownload", "Starting download for: $url")
                if (url.startsWith("http")) {
                    // 下载网络视频并获取本地路径
                    val localPath = downloadNetworkVideo(url, tempDir, index)
                    localPaths.add(localPath)
                } else {
                    // 非HTTP URL直接作为文件名
                    val localFile = File(tempDir, "${url}.mp4")
                    localPaths.add(localFile.absolutePath)
                }

                // 计算当前进度并通过回调通知UI更新
                val progress = ((index + 1) * 100) / videoUrls.size
                withContext(Dispatchers.Main) {
                    onProgressUpdate(progress)
                }
            } catch (e: Exception) {
                Log.e("VideoDownloader", "下载失败: $url", e)
                // 下载失败时不添加到结果列表，但仍更新进度
                val progress = ((index + 1) * 100) / videoUrls.size
                withContext(Dispatchers.Main) {
                    onProgressUpdate(progress)
                }
            }
        }

        return@withContext localPaths
    }

    /**
     * 下载单个网络视频（使用copyTo优化）
     * @param url 视频URL
     * @param dir 存储目录
     * @param index 视频索引
     * @return 下载后的本地文件路径
     */
    private suspend fun downloadNetworkVideo(
        url: String,
        dir: File,
        index: Int
    ): String = withContext(Dispatchers.IO) {
        // 创建输出文件
        val outputFile = File(dir, "${index}.mp4")
        // 创建OkHttpClient实例
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        // 发起HTTP请求下载视频
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP错误: ${response.code}")

            response.body?.byteStream()?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    // 使用带缓冲区的copyTo方法将视频数据写入文件
                    input.copyTo(output, 8192)
                }
            } ?: throw IOException("响应体为空")
        }

        outputFile.absolutePath
    }

    /**
     * 依次播放视频列表
     * @param videoPaths 视频本地路径列表
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun playVideosSequentially(videoPaths: List<String>) = withContext(Dispatchers.Main) {
        for (videoPath in videoPaths) {
            // 播放单个视频
            playVideo(videoPath)
            // 暂停当前协程，等待视频播放完成
            // 使用suspendCancellableCoroutine创建可取消的挂起点
            suspendCancellableCoroutine<Unit> { continuation ->
                // 为ExoPlayer添加状态监听器
                speechHelper.exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        // 当播放状态变为"已结束"时
                        if (playbackState == Player.STATE_ENDED) {
                            // 视频播放结束，移除监听器以避免内存泄漏
                            speechHelper.exoPlayer.removeListener(this)
                            // 恢复协程执行，继续处理下一个视频
                            continuation.resume(Unit, null)
                        }
                    }
                })
            }
        }
        // 所有视频播放完成后视频区域依然显现，用于接下来可能的重播
//        binding.playerView.visibility = View.VISIBLE
        binding.tvStatus.text = "状态：所有视频播放完成"
        binding.btnReplay.visibility = View.VISIBLE // 显示重播键
        // 暂时不释放播放器资源exoPlayer
//        speechHelper.exoPlayer.release()
    }


    /**
     * 播放单个视频
     * @param videoUrl 视频URL
     */
    private fun playVideo(videoUrl: String) {
        Log.d("VideoPlay", "Starting to play video: $videoUrl") // 添加日志
        binding.playerView.apply {
            controllerAutoShow = false // 禁止自动显示控制器
            useController = false // 隐藏控制器，包括进度条
        }
        // 创建媒体源（关键：使用ExoPlayer的MediaItem，修正导入错误）
        val mediaItem = MediaItem.fromUri(videoUrl)
        speechHelper.exoPlayer.setMediaItem(mediaItem)
        // 显示视频区域
        binding.playerView.visibility = View.VISIBLE
        binding.tvStatus.text = "状态：正在播放视频..."
        // 准备并播放
        speechHelper.exoPlayer.prepare()
        speechHelper.exoPlayer.play()
        // 单个播放完成监听（不再释放资源）
        speechHelper.exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // 添加日志，打印出该视频url
                    Log.d("VideoPlay", "Video playback finished: $videoUrl")
                }
            }
        })

    }

}