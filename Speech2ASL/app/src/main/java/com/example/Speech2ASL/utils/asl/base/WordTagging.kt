package com.example.Speech2ASL.utils.base

// 该数据类用于存储单词及其对应的词性标签
data class WordTagging(
    // 存储单词的属性，默认为空字符串
    var word: String = "",
    // 存储单词词性标签的属性，默认为空字符串
    var tag: String = ""
)