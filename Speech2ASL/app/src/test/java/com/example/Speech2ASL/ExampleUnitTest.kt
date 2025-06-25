//package com.example.Speech2ASL
//
//import android.content.Context
//import android.util.Log
//import androidx.test.core.app.ApplicationProvider
//import com.example.Speech2ASL.utils.service.ASLConversionService
//import com.example.Speech2ASL.utils.service.TrainedModelParser
//import org.junit.Test
//
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.runner.RunWith
////import org.robolectric.RobolectricTestRunner
////import org.robolectric.annotation.Config
////import org.robolectric.shadows.ShadowLog
//
///**
// * Example local unit test, which will execute on the development machine (host).
// *
// * See [testing documentation](http://d.android.com/tools/testing).
// */
////@RunWith(RobolectricTestRunner::class)
////@Config(
////    manifest = Config.NONE,
////    shadows = [ShadowLog::class] // 启用 Robolectric 的日志影子类
////    assetDir = "src/main/assets" // 正确位置
////)
//class ExampleUnitTest {
//    private val context = ApplicationProvider.getApplicationContext<Context>()
//    private val aslConversionService = ASLConversionService(context)
//
////    @Before
////    fun setupLogging() {
////        // 将日志重定向到标准输出（控制台）
////        ShadowLog.stream = System.out
////    }
//    @Test
//    fun testAslSentence() {
//        println(aslConversionService.getASLSentence("He is in the house."))
//        println(aslConversionService.getASLSentence("He will come tomorrow."))
//        println(aslConversionService.getASLSentence("She visited yesterday."))
//        println(aslConversionService.getASLSentence("They do not like it."))
//        println(aslConversionService.getASLSentence("A cat is here."))
//        println(aslConversionService.getASLSentence("It is big."))
//        println(aslConversionService.getASLSentence("She plays tennis."))
//        println(aslConversionService.getASLSentence("Cats meow."))
//        println(aslConversionService.getASLSentence("He quickly ran to the store."))
//        println(aslConversionService.getASLSentence("The beautiful flower smells nice."))
//    }
//}