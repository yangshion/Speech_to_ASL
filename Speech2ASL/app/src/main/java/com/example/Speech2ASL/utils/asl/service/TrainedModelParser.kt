package com.example.Speech2ASL.utils.service

import android.content.Context
import android.util.Log
import com.example.Speech2ASL.utils.base.WordTagging
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.process.CoreLabelTokenFactory
import edu.stanford.nlp.process.PTBTokenizer
import edu.stanford.nlp.process.TokenizerFactory
import edu.stanford.nlp.trees.Tree
import net.sf.extjwnl.dictionary.Dictionary
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.StringReader

/**
 * 该类负责加载预训练的语言模型（专门用于英文句子的句法解析的Stanford NLP的englishPCFG模型）
 * 对输入句子进行词性标注和语法分析。
 */
class TrainedModelParser(private val context: Context) {

    // 模型文件的完整路径（相对于assets目录）
    private val PCG_MODEL_FILENAME = "models/lexparser/englishPCFG.ser"

    //LexicalizedParser是斯坦福NLP库中用于实现基于概率的句法解析的一个核心类
    //它能够识别句子的句法结构，如主语、谓语、宾语等，并生成对应的树状结构，即用于将句子解析成语法树
    // 懒加载LexicalizedParser的实例parser，避免重复加载模型
    private val parser: LexicalizedParser by lazy {
        val cacheFile = File(context.cacheDir, "englishPCFG.ser") // 缓存文件路径，缓存文件名保持不变
        Log.d("TrainedModelParser", "缓存文件是否存在: ${cacheFile.exists()}, 路径: ${cacheFile.absolutePath}")
        if (!cacheFile.exists()) {
            copyAssetsToCache(PCG_MODEL_FILENAME, cacheFile)// 从assets复制到缓存
        }
        Log.d("TrainedModelParser", "开始从 ${cacheFile.absolutePath} 加载模型")
        return@lazy try {
            LexicalizedParser.loadModel(cacheFile.absolutePath) // 加载模型
        } catch (e: IOException) {
            Log.e("TrainedModelParser", "加载模型失败: ${e.message}", e)
            throw RuntimeException("加载模型失败", e)
        }
    }

    /**
     * 将assets目录中的模型文件复制到应用缓存目录
     * @param assetName assets中的文件名（"models/lexparser/englishPCFG.ser"）
     * @param targetFile 目标缓存文件
     */
    private fun copyAssetsToCache(assetName: String, targetFile: File) {
        try {
            context.assets.open(assetName).use { inputStream -> // 打开assets文件流
                Log.d("TrainedModelParser", "成功打开模型文件: $assetName")
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream) // 复制到缓存文件
                    Log.d("TrainedModelParser", "成功复制 $assetName 到 ${targetFile.absolutePath}")
                }
            }
        } catch (e: IOException) {
            Log.e("TrainedModelParser", "复制模型文件失败: ${e.message}", e)
            throw e
        }
    }


    // 配置PTB分词器工厂，分词器工厂也只创建一次
    private val tokenizerFactory: TokenizerFactory<CoreLabel> = PTBTokenizer.factory(
        CoreLabelTokenFactory(),
        "invertible=true" // 分词结果可逆，可还原为原始文本
    )

    /**
     * 解析句子并生成语法树。
     * @param sentence 输入的英文句子
     * @return 语法树（Tree对象），失败时返回null
     */
    fun getLexicalizedParserTree(sentence: String): Tree? {
        return try {
            // 创建分词器
            val tokenizer = tokenizerFactory.getTokenizer(StringReader(sentence))
            val tokens = tokenizer.tokenize() // 分词结果（List<CoreLabel>）
            Log.d("TrainedModelParser", "句子 '$sentence' 的分词结果: $tokens")
            return parser.apply(tokens) // 使用模型解析器来解析分词结果tokens，生成语法树
        } catch (e: Exception) {
            Log.e("TrainedModelParser", "解析句子失败: ${e.message}")
            return null
        }
    }

    /**
     * 对句子进行词性标注，返回WordTagging列表。
     * @param sentence 输入的英文句子
     * @return List<WordTagging> 包含单词及其词性标签
     * @throws Exception 解析失败时抛出异常
     */
    fun getTaggedResults(sentence : String) : List<WordTagging>{
        // 用于存储单词及其词性标签的可变列表
        val tagWords = mutableListOf<WordTagging>()
        // 获取解析后的语法树
        val parsedTree = getLexicalizedParserTree(sentence)
        parsedTree?.let { tree ->
            // 手动遍历语法树获取所有叶子节点
            val leaves = mutableListOf<Tree>()
            collectLeaves(tree, leaves)

            for (leaf in leaves) {
                // 获取父节点
                val parent: Tree = leaf.parent(tree) ?: continue
                // 提取单词文本和词性标签
                val word = (leaf.label() as CoreLabel).word() // 使用 CoreLabel 类型转换
                val tag = (parent.label() as CoreLabel).tag()

                // 将单词及其词性标签添加到列表中
                tagWords.add(WordTagging(word, tag))
            }
        } ?: throw Exception("Parsed Tree is Empty")

        return tagWords
    }

    /**
     * 递归收集语法树中的所有叶子节点
     */
    private fun collectLeaves(tree: Tree, leaves: MutableList<Tree>) {
        if (tree.isLeaf) {
            // 若为叶子节点，添加到叶子节点列表中
            leaves.add(tree)
        } else {
            // 若不是叶子节点，递归遍历其子节点
            for (child in tree.children()) {
                collectLeaves(child, leaves)
            }
        }
    }

    // 懒加载ExtJWNL词典实例dictionary，用于词形还原（如动词原形、名词单数）
    private val dictionary: Dictionary by lazy {
        try {
            Dictionary.getDefaultResourceInstance() // 加载默认词典
        } catch (e: Exception) {
            Log.e("TrainedModelParser", "Error initializing dictionary: ${e.message}")
            throw RuntimeException("Failed to initialize dictionary", e)
        }
    }

    /**
     * 获取词形还原处理器（MorphologicalProcessor），用于获取单词原形。
     * @return MorphologicalProcessor实例，失败时返回null
     */
    fun getMorphologicalProcessor() = try {
        //使用已创建的 dictionary 实例
        dictionary.morphologicalProcessor
    } catch (e: Exception) {
        Log.e("TrainedModelParser", "Error getting morphological processor: ${e.message}")
        null
    }
}