package com.example.Speech2ASL.utils.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresPermission
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.core.net.toUri
import com.example.Speech2ASL.R
import com.example.Speech2ASL.utils.checker.Checker
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 语音助手类，封装语音识别逻辑
 */
class SpeechHelper(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val binding: com.example.Speech2ASL.databinding.ActivityMainBinding,
    private val lifecycleScope: CoroutineScope,
    private val translateText: (String, (String) -> Unit) -> Unit
) {
    // 语音识别器实例
    internal lateinit var speechRecognizer: SpeechRecognizer
    //存放最新语音结果
    private var lastResult: String? = null
    //存放翻译结果
    internal var translate_lastResult: String? = null
    //标志onResults回调是否已经触发
    private var onResult_flag = 0

    internal var isRecording = false // 用于判断是否正在录音
    private var isManuallyStopped = false // 用于判断是否手动停止录音
    internal var exoPlayer: SimpleExoPlayer = SimpleExoPlayer.Builder(context).build()// 唯一的播放器变量


    //开始录音
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun startListening() {
        //检测网络连接情况
        if (!Checker.isNetworkAvailable(context)) {
            Toast.makeText(context, "需要网络连接", Toast.LENGTH_LONG).show()
            return
        }
        // 创建语音识别的Intent，该Intent用于启动语音识别服务
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // 指定处理该Intent的应用包名，这里强制使用Google语音搜索应用
            // 这确保了在不同Android设备上使用统一的语音识别引擎
            `package` = "com.google.android.googlequicksearchbox"
            // 设置语音识别的语言模式为自由形式，这种模式适用于识别自然语言
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请开始说话")
            // 设置是否优先使用离线语音识别，设为false表示优先使用在线识别，以获得更准确的结果
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            // 设置返回的最大结果数为1，只获取最可能的一个识别结果
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Checker.isHuaweiDevice()) {
                // 某些华为设备的语音识别可能对中文支持不佳
                // 添加额外的中文语言设置以确保中文识别的准确性
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("zh-CN"))
            }
        }
        //开始录音时重置onResults回调是否触发标志位
        // 重置结果文字和翻译结果以及ASL语法转换结果为空
        onResult_flag = 0
        binding.tvResult.text = ""
        binding.tvTranslation.text = ""
        binding.tvASLSentence.text = ""
        //改变正在录音和停止录音标志位
        isRecording = true
        isManuallyStopped = false
        binding.tvStatus.text = "状态：正在录音..."
        // 切换为录音状态样式
        binding.btnStart.setBackgroundResource(R.drawable.btn_voice_recording)
        // 隐藏确认和取消按钮
        binding.btnConfirm.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        // 开始录音时隐藏视频播放界面和重播按钮
        binding.playerView.visibility = View.GONE
        binding.btnReplay.visibility = View.GONE

        //无论上一次视频是否全部播放完成，开始录音时都要释放播放器资源exoPlayer
        exoPlayer.release()
        exoPlayer = SimpleExoPlayer.Builder(context).build()
        binding.playerView.player = exoPlayer
        // 开始语音识别
        speechRecognizer.startListening(intent)
    }

    //停止录音
    fun stopListening() {
        isRecording = false
        isManuallyStopped = true
        binding.tvStatus.text = "状态：处理中.."
        // 切换回正常状态样式
        binding.btnStart.setBackgroundResource(R.drawable.btn_voice_normal)
        //如果onResults回调已经被自动触发，显示最终结果，改变状态
        if (onResult_flag == 1) {
            binding.tvResult.text = lastResult
            binding.tvStatus.text = "状态：转换为文本、翻译完成"
            // 翻译逻辑,显示翻译结果
            translateText(lastResult.toString()) { translated ->
                // 在主线程执行UI更新操作
                lifecycleScope.launch {
                    //将翻译得到的结果赋给translate_lastResult
                    translate_lastResult = translated
                    //更新tvTranslation控件，显示翻译结果
                    binding.tvTranslation.text = translate_lastResult
                }
            }
            // 显示确认和取消按钮
            if (!lastResult.isNullOrEmpty()) {
                binding.btnConfirm.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.VISIBLE
            }
        }
        //如果onResults回调还没有被自动触发，
        // 语音识别器实例speechRecognizer停止语音识别，主动触发
        else {
            speechRecognizer.stopListening()
        }
    }


    // 实现语音识别器的初始化与回调配置
    fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            // 创建语音识别器实例，用于启动和管理语音识别会话
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            // 设置语音识别监听器，处理语音识别的各个阶段的事件（如开始录音、识别结果、错误等）
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // 去掉原有逻辑
                }

                override fun onBeginningOfSpeech() {
                    // 去掉原有逻辑
                }
                // 识别结果回调，当语音识别完成并返回结果时触发
                override fun onResults(results: Bundle?) {
                    //标志onResult回调已经被触发
                    onResult_flag = 1
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)?.let {
                        lastResult = it // 始终保存最新结果
                        // 如果是手动停止且有结果，显示结果并展示确认和取消按钮
                        if (isManuallyStopped) {
                            binding.tvResult.text = lastResult
                            binding.tvStatus.text = "状态：转换为文本、翻译完成"
                            // 翻译逻辑,显示翻译结果
                            translateText(lastResult.toString()) { translated ->
                                // 在主线程执行UI更新操作
                                lifecycleScope.launch {
                                    //将翻译得到的结果赋给translate_lastResult
                                    translate_lastResult = translated
                                    //更新tvTranslation控件，显示翻译结果
                                    binding.tvTranslation.text = translate_lastResult
                                }
                            }
                            // 显示确认和取消按钮
                            if (!lastResult.isNullOrEmpty()) {
                                binding.btnConfirm.visibility = View.VISIBLE
                                binding.btnCancel.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                // 错误处理
                override fun onError(error: Int) {
                    // 根据错误码生成错误信息
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足（尝试打开Google的麦克风权限）"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误（尝试打开vpn）"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "未检测到语音，无匹配结果"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器繁忙"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "说话超时"
                        else -> "未知错误 (代码: $error)"
                    }
                    //若发生错误，显示错误信息
                    AlertDialog.Builder(context)
                        .setTitle("识别错误")
                        .setMessage(errorMsg)
                        .setPositiveButton("确定", null)
                        .show()
                    //若发生错误，重置状态，隐藏确认和取消按钮
                    isRecording = false
                    isManuallyStopped = true
                    binding.tvStatus.text = "状态：等待中"
                    binding.btnStart.setBackgroundResource(R.drawable.btn_voice_normal)
                    binding.btnConfirm.visibility = View.GONE
                    binding.btnCancel.visibility = View.GONE

                }
                // 其他回调方法可留空
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            Toast.makeText(context, "设备不支持语音识别", Toast.LENGTH_SHORT).show()
        }
    }


    // 录音权限请求Launcher
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // 启动权限检查
    fun requestRecordPermission() {
        // 检查录音权限的授予状态，返回PackageManager.PERMISSION_GRANTED表示已授权
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            //如果录音权限未授予，请求录音权限
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 显示权限拒绝对话框
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("权限被拒绝")
            .setMessage("必须授予麦克风权限才能使用此功能")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${activity.packageName}".toUri()
                }
                activity.startActivity(intent)
            }
            .show()
    }

}