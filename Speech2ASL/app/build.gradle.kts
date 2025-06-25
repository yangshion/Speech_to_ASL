import java.io.FileInputStream
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.Speech2ASL"
    compileSdk = 35

    val localProperties = Properties().apply {
        load(FileInputStream(rootProject.file("local.properties")))
    }

    defaultConfig {
        applicationId = "com.example.Speech2ASL"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BAIDU_APP_ID",
            "\"${localProperties["BAIDU_APP_ID"]}\""
        )
        buildConfigField(
            "String",
            "BAIDU_APP_KEY",
            "\"${localProperties["BAIDU_APP_KEY"]}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // 统一 Java 和 Kotlin 编译目标版本
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            assets {
                // 添加原有 assets 目录
                srcDirs("src/main/assets")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.slf4j:slf4j-android:1.7.36")

    // 斯坦福 NLP（唯一声明并排除旧注解）
//    implementation(libs.stanford.corenlp) {
//        exclude(group = "com.sun.xml.bind", module = "jaxb-impl")
//        exclude(group = "javax.xml.bind", module = "jaxb-api")
//        exclude(group = "edu.stanford.nlp", module = "stanford-parser")
//    }

//    implementation("edu.stanford.nlp:stanford-corenlp:3.6.0:models-english") {
//        exclude(group = "edu.stanford.nlp", module = "stanford-parser")
//        exclude(group = "com.intellij", module = "annotations")
//    }
    implementation("edu.stanford.nlp:stanford-parser:3.6.0") {
        exclude(group = "com.intellij", module = "annotations")
    }

    // extJWNL（唯一声明并排除旧注解）
    implementation("net.sf.extjwnl:extjwnl:2.0.2") {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation("net.sf.extjwnl:extjwnl-data-wn31:1.2")

    // LanguageTool 排除旧注解
    implementation("org.languagetool:language-en:5.8") {
        exclude(group = "com.intellij", module = "annotations")
    }

    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("org.robolectric:robolectric:4.10.3") // 关键修正：改为 androidTestImplementation
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}