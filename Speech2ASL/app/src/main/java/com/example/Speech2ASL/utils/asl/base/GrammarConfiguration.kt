package com.example.Speech2ASL.utils.base

// 该类用于存储语法配置信息，如系动词、动词标签、名词标签等
class GrammarConfiguration {
    // 静态内部类，存储系动词列表
    object BeWords {
        val beWords = arrayListOf(
            "IS", "ARE", "AM", "WAS", "WERE", "BEEN", "BEING", "BE",
            "WILL", "WOULD", "SHOULD", "COULD", "CAN", "HAVE", "HAD", "HAS",
            "SHALL", "MUST", "MAY", "MIGHT"
        )
    }

    // 静态内部类，存储动词标签和副词标签列表
    object VerbTags {
        val verbTags = arrayListOf("VB", "VBD", "VBG", "VBN", "VBP", "VBZ")
        val adverbTags = arrayListOf("RB", "RBR", "RBS")
        fun isVerb(tag: String) = verbTags.contains(tag.trim().uppercase())
        fun isAdverb(tag: String) = adverbTags.contains(tag.trim().uppercase())
    }

    // 静态内部类，存储名词标签、复数名词标签和形容词标签列表
    object NounTags {
        val nounTags = arrayListOf("NN", "NNS", "NNP", "NNPS")
        val pluralNounTags = arrayListOf("NN", "NNS", "NNPS")
        val adjectiveTags = arrayListOf("JJ", "JJR", "JJS")
        fun isNoun(tag: String) = nounTags.contains(tag.trim().uppercase())
        fun isAdjective(tag: String) = adjectiveTags.contains(tag.trim().uppercase())
    }

    // 静态内部类，存储有效词性标签列表
    // 比如“CC”表示并列连词，例子有and、or、but；“JJ”表示形容词；“PRP”表示人称代词，如I、he、she；“VB”表示动词原形
    object ValidPOS {
        val validPos = arrayListOf(
            "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS",
            "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "PRP", "PRP$", "RB",
            "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN",
            "VBP", "VBZ", "WDT", "WP", "WP$", "WRB"
        )
    }

    // 静态内部类，存储时间词列表
    object TimeWords {
        val timeWords = arrayListOf(
            "YESTERDAY", "MORNING", "AFTERNOON", "EVENING", "NIGHT", "TOMORROW"
        )
    }

    // 静态内部类，存储否定词列表
    object NegationWords {
        val negationWords = arrayListOf("NOT", "NONE", "NEVER", "NOBODY", "NOTHING", "NOONE", "N'T")
    }

    // 静态内部类，存储缩写形式及其对应的完整形式的映射
    object Contractions {
        val contractions = hashMapOf(
            "aren't" to "are not",
            "can't" to "can not",
            "could've" to "could have",
            "couldn't" to "could not",
            "didn't" to "did not",
            "doesn't" to "does not",
            "don't" to "do not",
            "hadn't" to "had not",
            "hasn't" to "has not",
            "haven't" to "have not",
            "how'll" to "how will",
            "everybody's" to "everybody is",
            "everyone's" to "everyone is",
            "I'm" to "I am",
            "I've" to "I have",
            "you've" to "you have",
            "you're" to "you are",
            "we're" to "we are",
            "weren't" to "were not",
            "isn't" to "is not",
            "it'd" to "it would",
            "they've" to "they have",
            "those've" to "those have",
            "'twas" to "it was",
            "let's" to "let us",
            "may've" to "may have",
            "might've" to "might have",
            "mustn't" to "must not",
            "must've" to "must have",
            "needn't" to "need not",
            "should've" to "should have",
            "shouldn't" to "should not",
            "gonna" to "going to",
            "wanna" to "want to",
            "wasn't" to "was not",
            "what've" to "what have",
            "where'd" to "where did",
            "who're" to "who are",
            "who've" to "who have",
            "why'd" to "why did",
            "why're" to "why are",
            "won't" to "will not",
            "would've" to "would have",
            "wouldn't" to "would not"
        )
    }
}