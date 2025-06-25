package com.example.Speech2ASL.utils.checker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.speech.RecognizerIntent
import androidx.annotation.RequiresPermission

object Checker {
    /**
     * 检查当前设备的网络是否可用。
     * @param context 上下文对象，用于获取系统服务。
     * @return 如果网络可用返回 true，否则返回 false。
     * @throws SecurityException 如果没有 ACCESS_NETWORK_STATE 权限。
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean {
        // 获取 ConnectivityManager 系统服务实例
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // 获取当前活动的网络，如果没有活动网络则返回 false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        // 获取当前活动网络的功能信息，如果无法获取则返回 false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        // 检查当前网络是否具备互联网连接能力
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    /**
     * 检查设备上是否有可用的Google语音识别服务。
     * @param context 上下文对象，用于查询包管理器。
     * @return 如果有可用的语音识别服务返回 true，否则返回 false。
     */
    fun checkSpeechService(context: Context): Boolean {
        // 创建一个用于语音识别的 Intent，并设置语言模型为自由形式
        val activityIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        // 通过查询系统中是否存在能够处理语音识别 Intent 的 Activity 来检测 Google 语音服务是否可用
        //MATCH_DEFAULT_ONLY 标志表示确保找到的是默认处理程序
        return context.packageManager.resolveActivity(activityIntent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    //检测是否是华为设备
    fun isHuaweiDevice(): Boolean {
        // 比较设备制造商名称是否为 "HUAWEI"，忽略大小写
        return Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)
    }
}