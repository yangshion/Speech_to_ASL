package com.example.Speech2ASL.utils.translate

// OkHttp网络请求相关
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
// JSON解析
import org.json.JSONObject
// 安全相关（MD5）
import java.security.MessageDigest
// 随机数生成
import kotlin.random.Random
import com.example.Speech2ASL.BuildConfig

/**
 * 该类用于实现基于百度翻译 API 的文本翻译功能。
 */
class BaiduTranslator {

    // 百度通用文本翻译 API的应用ID和密钥，需要在local.properties中配置
    // 即在local.properties添加BAIDU_APP_ID=？ BAIDU_APP_KEY=？
    // 可在https://api.fanyi.baidu.com/product/11获取个人app_id和密钥
    private val appId: String = BuildConfig.BAIDU_APP_ID
    private val appKey: String = BuildConfig.BAIDU_APP_KEY
    // 百度翻译 API 的请求 URL
    private val translateUrl: String = "https://fanyi-api.baidu.com/api/trans/vip/translate"

    /**
     * 使用百度翻译 API 对给定的文本进行翻译。
     *
     * @param query 要翻译的文本。
     * @param fromLang 源语言代码，默认为中文（"zh"）。
     * @param toLang 目标语言代码，默认为英文（"en"）。
     * @param callback 翻译结果的回调函数，接收一个 Result<String> 类型的参数。
     */
    fun translate(
        query: String,
        fromLang: String = "zh",
        toLang: String = "en",
        callback: (Result<String>) -> Unit
    ) {
        // 在新线程中执行网络请求
        Thread {
            try {
                // 生成一个 0 到 9999 之间的随机数作为盐值
                val salt = Random.nextInt(10000).toString()
                // 生成签名，用于验证请求的合法性
                val sign = md5("$appId$query$salt$appKey")
                // 创建请求体，包含翻译所需的参数
                val formBody = FormBody.Builder()
                    .add("q", query)
                    .add("from", fromLang)
                    .add("to", toLang)
                    .add("appid", appId)
                    .add("salt", salt)
                    .add("sign", sign)
                    .build()
                // 创建请求对象，指定请求 URL 和请求方法为 POST
                val request = Request.Builder()
                    .url(translateUrl)
                    .post(formBody)
                    .build()
                // 使用 OkHttpClient 在后台线程发送 POST 请求到百度翻译 API
                val response = OkHttpClient().newCall(request).execute()
                // 解析响应的 JSON 数据
                val json = JSONObject(response.body?.string() ?: "")
                // 从 JSON 数据中提取翻译结果
                val result = json.getJSONArray("trans_result")
                    .getJSONObject(0)
                    .getString("dst")

                // 将翻译结果封装为成功的 Result 对象并传递给回调函数
                callback(Result.success(result))
            } catch (e: Exception) {
                // 如果发生异常，将异常信息传递给回调函数
                callback(Result.failure(e))
            }
        }.start()
    }
    /**
     * 对输入的字符串进行 MD5 加密。
     *
     * @param input 要加密的字符串。
     * @return 加密后的十六进制字符串。
     */
    private fun md5(input: String): String {
        // 获取 MD5 消息摘要实例
        val md = MessageDigest.getInstance("MD5")
        // 对输入字符串的字节数组进行摘要计算
        val digest = md.digest(input.toByteArray())
        // 将摘要结果转换为十六进制字符串
        return digest.joinToString("") { "%02x".format(it) }
    }
}