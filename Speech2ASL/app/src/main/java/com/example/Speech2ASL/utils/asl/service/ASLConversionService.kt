package com.example.Speech2ASL.utils.service

import android.content.Context
import com.example.Speech2ASL.utils.base.GrammarConfiguration
import com.example.Speech2ASL.utils.base.WordTagging
import net.sf.extjwnl.data.POS
import java.util.*

// 该类用于将英语句子转换为符合美国手语（ASL）语法结构的句子
class ASLConversionService(private val context: Context) {

    // 标记时间项是否在句首的标志
    private var isTimeItemFirst = false
    // 初始化训练模型解析器实例
    private val trainedModelParser = TrainedModelParser(context)

    // 该方法用于将输入的英语句子转换为 ASL 句子
    @Throws(Exception::class)
    fun getASLSentence(sentence: String): String {
        // 替换句子中的缩写形式
        var processedSentence = replaceContractions(sentence)

        // 对处理后的句子进行解析和词性标注，并进行语法检查
        var tagWords = trainedModelParser.getTaggedResults(processedSentence)

        // 删除无效词性的单词，并将有效单词转换为大写
        tagWords = deleteInvalidPOSCapitalize(tagWords)

        // 将时间项移到句首
        tagWords = bringTimeItemsFirst(tagWords)
        if (!isTimeItemFirst) tagWords = bringPastItemsFirst(tagWords)
        // 将否定项移到句尾
        tagWords = pushNegationItemsLast(tagWords)
        // 删除冠词
        tagWords = deleteArticles(tagWords)
        // 删除系动词
        tagWords = deleteBeWords(tagWords)
        // 替换动词形式
        tagWords = replaceVerbWords(tagWords)
        // 替换复数名词为单数形式
        tagWords = replacePluralNounWords(tagWords)
        // 调整动词和副词的顺序
        tagWords = replaceVerbAdverb(tagWords)
        // 调整形容词和名词的顺序
        tagWords = replaceAdjectiveNoun(tagWords)

        // 将处理后的单词列表转换为 ASL 句子
        var aslSentence = getASLSentence(tagWords)
        return aslSentence
    }

    // 该方法用于替换句子中的缩写形式
    private fun replaceContractions(sentence: String): String {
        var result = sentence
        GrammarConfiguration.Contractions.contractions.forEach { (key, value) ->
            result = result.replace(key, value, ignoreCase = true)
        }
        return result
    }

    // 该方法用于删除无效词性的单词，并将有效单词转换为大写
    private fun deleteInvalidPOSCapitalize(tagWords: List<WordTagging>): List<WordTagging> {
        return tagWords.filter { wordTag ->
            // 过滤出有效词性的单词
            GrammarConfiguration.ValidPOS.validPos.any { pos -> pos.trim() == wordTag.tag }
        }.map { wordTag ->
            // 将有效单词转换为大写
            WordTagging(wordTag.word.uppercase(Locale.getDefault()), wordTag.tag)
        }
    }

    // 该方法用于将时间项移到句首
    private fun bringTimeItemsFirst(tagWords: List<WordTagging>): List<WordTagging> {
        val timeWords = GrammarConfiguration.TimeWords.timeWords
        return tagWords.partition { wordTag ->
            // 分离时间项和其他项
            timeWords.any { timeWord -> timeWord.trim() == wordTag.word }
        }.let { (timeItems, others) ->
            isTimeItemFirst = timeItems.isNotEmpty()
            // 将时间项放在其他项之前
            timeItems + others
        }
    }

    // 该方法用于将过去时间项移到句首，此处复用 bringTimeItemsFirst 方法
    private fun bringPastItemsFirst(tagWords: List<WordTagging>) = bringTimeItemsFirst(tagWords)

    // 该方法用于将否定项移到句尾
    private fun pushNegationItemsLast(tagWords: List<WordTagging>): List<WordTagging> {
        val negationWords = GrammarConfiguration.NegationWords.negationWords
        return tagWords.partition { wordTag ->
            // 分离非否定项和否定项
            !negationWords.any { negation -> negation.trim() == wordTag.word }
        }.let { (nonNegations, negations) ->
            // 将非否定项放在否定项之前
            nonNegations + negations
        }
    }

    // 该方法用于删除冠词
    private fun deleteArticles(tagWords: List<WordTagging>): List<WordTagging> {
        return tagWords.filterNot { wordTag ->
            // 过滤掉冠词
            wordTag.tag in setOf("DT", "TO", "IN")
        }
    }

    // 该方法用于删除系动词
    private fun deleteBeWords(tagWords: List<WordTagging>): List<WordTagging> {
        val beWords = GrammarConfiguration.BeWords.beWords
        return tagWords.filterNot { wordTag ->
            // 过滤掉系动词
            beWords.any { beWord -> beWord.trim() == wordTag.word }
        }
    }

    // 该方法用于替换动词形式
    private fun replaceVerbWords(tagWords: List<WordTagging>): List<WordTagging> {
        return tagWords.map { wordTag ->
            if (GrammarConfiguration.VerbTags.verbTags.any { pos -> pos.trim() == wordTag.tag }) {
                // 若为动词，替换为动词原形并转换为大写
                WordTagging(getVerbWord(wordTag.word), wordTag.tag)
            } else {
                // 否则保持不变
                wordTag
            }
        }
    }

    // 该方法用于获取动词原形
    private fun getVerbWord(verb: String): String {
        val morphologicalProcessor = trainedModelParser.getMorphologicalProcessor()
        return try {
            morphologicalProcessor?.lookupBaseForm(POS.VERB, verb.lowercase())?.lemma?.toString()?.uppercase()
                ?: verb.uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            verb.uppercase()
        }
    }

    // 该方法用于替换复数名词为单数形式
    private fun replacePluralNounWords(tagWords: List<WordTagging>): List<WordTagging> {
        return tagWords.map { wordTag ->
            if (GrammarConfiguration.NounTags.pluralNounTags.any { pos -> pos.trim() == wordTag.tag }) {
                // 若为复数名词，替换为单数形式并转换为大写
                WordTagging(getSingularNoun(wordTag.word), wordTag.tag)
            } else {
                // 否则保持不变
                wordTag
            }
        }
    }

    // 该方法用于获取单数名词
    private fun getSingularNoun(noun: String): String {
        val morphologicalProcessor = trainedModelParser.getMorphologicalProcessor()
        return try {
            morphologicalProcessor?.lookupBaseForm(POS.NOUN, noun.lowercase())?.lemma?.toString()?.uppercase()
                ?: noun.uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            noun.uppercase()
        }
    }

    // 优化后的动副顺序调整方法
    private fun replaceVerbAdverb(tagWords: List<WordTagging>): List<WordTagging> {
        val list = tagWords.toMutableList()
        var modified = true

        while (modified) {
            modified = false
            for (i in 0 until list.size - 1) {
                if (GrammarConfiguration.VerbTags.isAdverb(list[i].tag) &&
                    GrammarConfiguration.VerbTags.isVerb(list[i + 1].tag)) {
                    list.swap(i, i + 1)
                    modified = true
                    break // 每次只交换一对，避免跳过后续可能的交换
                }
            }
        }
        return list
    }

    // 优化后的形名顺序调整方法
    private fun replaceAdjectiveNoun(tagWords: List<WordTagging>): List<WordTagging> {
        val list = tagWords.toMutableList()
        var modified = true

        while (modified) {
            modified = false
            for (i in 0 until list.size - 1) {
                if (GrammarConfiguration.NounTags.isAdjective(list[i].tag) &&
                    GrammarConfiguration.NounTags.isNoun(list[i + 1].tag)) {
                    list.swap(i, i + 1)
                    modified = true
                    break // 每次只交换一对，避免跳过后续可能的交换
                }
            }
        }
        return list
    }

    // 辅助方法：交换列表中两个元素的位置
    private fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
        val temp = this[index1]
        this[index1] = this[index2]
        this[index2] = temp
    }

    // 该方法用于将处理后的单词列表转换为 ASL 句子
    private fun getASLSentence(tagWords: List<WordTagging>) = tagWords.joinToString(" ") { it.word }
}