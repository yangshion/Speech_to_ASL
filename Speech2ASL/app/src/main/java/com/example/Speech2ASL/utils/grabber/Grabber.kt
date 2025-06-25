package com.example.Speech2ASL.utils.grabber

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.util.ArrayList

/**
 * 该类主要用于抓取句子对应的手语视频URL列表，适用于Android平台。
 * 使用Jsoup库解析HTML页面，通过协程在后台线程执行网络请求，避免阻塞主线程。
 */
object Grabber {
    // 字母对应的手语页面路径，按A-Z顺序排列（部分字母为直接链接，部分需要搜索）
    private val letterURLs = arrayOf(
        "sign/A/5820/1", "search/b", "search/c", "search/d", "search/e",
        "search/f", "sign/G/5826/1", "search/h", "sign/I/5828/1", "search/j",
        "search/k", "sign/L/5831/1", "sign/M/5832/1", "search/n", "search/o",
        "search/p", "search/q", "search/r", "search/s", "sign/T/5839/1",
        "search/u", "search/v", "search/w", "search/x", "search/y", "search/z"
    )
    private const val baseURL = "https://www.signingsavvy.com/" // 目标网站基础URL
    private const val baseCSS = "html body#page_signs.bg div#frame div#main.index div#main.sub div#main_content div#main_content_inner div#main_content_left div.content_module" // 页面主内容区的CSS选择器

    /**
     * 在协程中获取句子对应的手语视频 URL 列表（Android 兼容版本）
     * @param sentence 待转换的文本句子
     * @return 包含每个单词对应视频URL的列表（按单词顺序排列）
     */
    suspend fun getVideoURLsFromSentence(sentence: String): ArrayList<String> {
        return withContext(Dispatchers.IO) { // 确保在后台线程执行网络请求
            val ret = ArrayList<String>() // 存储最终结果的列表
            val words = sentence.split(" ") // 将句子拆分为单词数组

            // 遍历每个单词，获取对应的手语视频URL
            for ((k, word) in words.withIndex()) {
                val searchUrl = "$baseURL/search/${word.lowercase()}" // 构建搜索URL
                try {
                    // 使用Jsoup连接到搜索URL并获取HTML文档
                    val document = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // 模拟浏览器请求
                        .timeout(10_000) // 10秒超时设置
                        .get()

                    // 检查单词是否重复（处理重复单词的情况）
                    val indexOfWord = words.indexOf(word)
                    if (indexOfWord != k) {
                        ret.add(indexOfWord.toString()) // 记录重复单词的首次出现位置
                        continue // 跳过后续处理
                    }

                    // 判断搜索结果类型（是否为搜索结果列表页）
                    val searchResults = document.selectFirst("$baseCSS h2")?.text() ?: ""
                    if (searchResults.contains("Search Results")) {
                        // 搜索结果页：提取多个含义选项
                        val searchResultsDiv = document.selectFirst("$baseCSS div.search_results")
                        if (searchResultsDiv != null) {
                            val meaningOptions = searchResultsDiv.select("ul > li")
                            if (meaningOptions.isNotEmpty()) {
                                // 有多个含义选项时，默认选择第一个（实际应用中应让用户选择）
                                val firstOption = meaningOptions.first()
                                val meaningUrl = baseURL + firstOption?.select("a")?.attr("href")
                                ret.add(getVideoURL(meaningUrl)) // 递归获取具体含义的视频URL
                            } else {
                                // 无搜索结果：按字母拆分处理
                                word.forEach { char ->
                                    val index = char.toUpperCase().code - 'A'.code
                                    if (index in 0..letterURLs.lastIndex) {
                                        // 从预定义的字母URL列表中获取对应字母的视频URL
                                        ret.add(getVideoURL("$baseURL${letterURLs[index]}"))
                                    }
                                }
                            }
                        }
                    } else {
                        // 直接找到单词页面：直接获取视频URL
                        ret.add(getVideoURL(searchUrl))
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    // 网络请求失败时返回空字符串
                    ret.add("")
                }
            }
            ret
        }
    }

    /**
     * 从指定页面URL获取手语视频的直接URL
     * @param pageURL 手语单词或字母的详情页URL
     * @return 视频的直接URL，失败时返回空字符串
     */
    private fun getVideoURL(pageURL: String): String {
        return try {
            // 使用Jsoup解析详情页，提取视频链接
            val document = Jsoup.connect(pageURL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10_000)
                .get()
            // 通过CSS选择器定位视频链接元素
            document.selectFirst("$baseCSS div.sign_module div.signing_body div.videocontent link")
                ?.attr("href") ?: ""
        } catch (e: IOException) {
            Log.e("Grabber", "获取文档失败: $pageURL", e)
            e.printStackTrace()
            ""
        }
    }
}