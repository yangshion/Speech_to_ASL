package com.example.Speech2ASL

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Speech2ASL.databinding.ActivityMainBinding
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.Speech2ASL.utils.checker.Checker
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.Speech2ASL.utils.grabber.Grabber
import com.example.Speech2ASL.utils.service.ASLConversionService
import com.example.Speech2ASL.utils.video.VideoHelper
import com.example.Speech2ASL.utils.speech.SpeechHelper
import com.example.Speech2ASL.utils.translate.BaiduTranslator


class MainActivity : AppCompatActivity() {
    // 视图绑定实例
    private lateinit var binding: ActivityMainBinding
    // ASL转换服务实例，用于将文本转换为ASL语法
    private var aslConversionService: ASLConversionService = ASLConversionService(this)
    //playedVideoPaths用来存储下载的视频所在的临时本地路径，用于重播视频
    private var playedVideoPaths: List<String> = emptyList()
    // 语音助手实例，封装语音识别逻辑
    private lateinit var speechHelper: SpeechHelper
    // 视频助手实例，管理视频下载和播放
    private lateinit var videoHelper: VideoHelper
    // 百度翻译器实例，用于中译英
    private val baiduTranslator = BaiduTranslator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //使用视图绑定类，可以直接通过 binding 访问视图
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //检查Google语音服务是否可用
        if (!Checker.checkSpeechService(this)) {
            // 若不可用，弹出提示框引导用户安装Google语音服务
            AlertDialog.Builder(this)
                .setTitle("服务不可用")
                .setMessage("请安装 Google")
                .setPositiveButton("安装") { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox".toUri()))
                }
                .show()
            return
        }
        //检查是否是华为设备
        if (Checker.isHuaweiDevice()) {
            // 若是华为设备，需要设置默认语音助手为Google
            // 弹出提示框告知用户额外配置信息
            AlertDialog.Builder(this)
                .setTitle("设备兼容性提示")
                .setMessage("华为设备需额外配置：\n1. 开启Google基础服务\n2. 设置默认语音助手为Google")
                .setPositiveButton("确定", null)
                .show()
        }

        //初始化语音助手实例
        speechHelper = SpeechHelper(
            context = this,
            activity = this,
            binding = binding,
            lifecycleScope = this.lifecycleScope, // 传递 lifecycleScope
            translateText = { query, callback ->
                // 调用百度翻译API，返回翻译结果
                baiduTranslator.translate(query) { Result ->
                    Result.onSuccess { translated ->
                        // 翻译成功，通过callback返回结果
                        callback(translated)
                    }.onFailure { e ->
                        // 翻译失败时显示Toast提示
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "翻译失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

        // 初始化语音识别器
        speechHelper.setupSpeechRecognizer()

        //初始化视频助手实例
        videoHelper = VideoHelper(
            context = this,
            binding = binding,
            speechHelper = speechHelper
        )

        //录音按键点击事件
        binding.btnStart.setOnClickListener {
            if (speechHelper.isRecording) {
                // 当前若正在录音，停止录音
                speechHelper.stopListening()
            } else {
                // 当前若未录音，检查麦克风权限是否开启
                // 若未开启，请求开启麦克风权限，若已开启，开始录音
                speechHelper.requestRecordPermission()
            }
        }

        // 确认按钮点击事件
        binding.btnConfirm.setOnClickListener {
            val inputText = binding.tvResult.text.toString()
            if (inputText.isEmpty()) {
                // 若输入文本为空，提示用户先录音获取文本
                Toast.makeText(this, "请先录音获取文本", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 隐藏确认和取消按钮
            binding.btnConfirm.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
            binding.tvStatus.text = "状态：正在转换为ASL语法，准备下载视频.."
            // 在协程中处理视频获取和播放逻辑
            lifecycleScope.launch {
                (speechHelper.translate_lastResult)?.let { sentence ->
                    //将翻译后的句子内容转换为美国手语（ASL）语法结构的句子
                    val aslSentence = aslConversionService.getASLSentence(sentence)
                    //添加日志，运行中查看是否打印出正确结果
                    Log.d("aslSentence","aslSentence:$aslSentence")
                    //爬取该美国手语（ASL）语法句子对应的手语视频URL列表
                    val testVideoUrls = Grabber.getVideoURLsFromSentence(aslSentence)
                    //添加日志，运行中查看爬取到的视频URL
                    Log.d("VideoURLs", "Fetched video URLs: $testVideoUrls")
                    if (testVideoUrls.isNotEmpty()) {
                        // 显示进度条
                        binding.progressBar.progress = 0
                        binding.tvProgress.text = "下载进度: 0%"
                        binding.progressContainer.visibility = View.VISIBLE
                        binding.tvStatus.text = "状态：正在下载视频..."

                        // 下载视频并更新进度
                        val localVideoPaths = videoHelper.downloadVideosAndCreateSequence(
                            testVideoUrls
                        ) { progress ->
                            binding.progressBar.progress = progress
                            binding.tvProgress.text = "下载进度: $progress%"
                        }

                        // 隐藏进度条
                        binding.progressContainer.visibility = View.GONE
                        binding.tvStatus.text = "状态：等待中"

                        if (localVideoPaths.isNotEmpty()) {
                            //在视频下方显示完成ASL语法转换的句子
                            binding.tvASLSentence.text=aslSentence
                            //localVideoPaths赋值给playedVideoPaths，用于接下来的重播视频
                            playedVideoPaths = localVideoPaths
                            // 依次播放下载后的视频
                            videoHelper.playVideosSequentially(localVideoPaths)
                        } else {
                            Toast.makeText(this@MainActivity, "未成功下载任何视频", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "未找到视频链接", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 取消按钮点击事件
        binding.btnCancel.setOnClickListener {
            // 处理取消逻辑，清空结果文本和状态信息
            binding.tvResult.text = ""
            binding.tvTranslation.text = ""
            binding.tvStatus.text = "状态：等待中"
            Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
            // 隐藏确认和取消按钮
            binding.btnConfirm.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
        }

        // 重播按键点击事件
        binding.btnReplay.setOnClickListener {
            lifecycleScope.launch {
                if (playedVideoPaths.isNotEmpty()) {
                    videoHelper.playVideosSequentially(playedVideoPaths)
                }
            }
        }
    }

    // 在Activity销毁时释放ExoPlayer资源（避免内存泄漏）
    override fun onDestroy() {
        super.onDestroy()
        speechHelper.speechRecognizer.destroy()
        speechHelper.exoPlayer.release()
    }

}