import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val customBuildDir = System.getenv("PROBEAPP_BUILD_DIR")?.takeIf { it.isNotBlank() }
if (customBuildDir != null) {
    buildDir = file(customBuildDir)
} else {
    buildDir = file("${System.getProperty("java.io.tmpdir")}/kidsafe_probeapp_build_${System.currentTimeMillis()}")
}

val versionPropsFile = layout.projectDirectory.file("version.properties").asFile
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}
val versionNameBase = versionProps.getProperty("VERSION_NAME_BASE") ?: "1.0"
val versionCodeFromFile = versionProps.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1
val versionNameFromFile = "$versionNameBase.$versionCodeFromFile"

android {
    namespace = "com.kidsafe.probe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kidsafe.probe"
        minSdk = 26
        targetSdk = 34
        versionCode = versionCodeFromFile
        versionName = versionNameFromFile
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
        create("standalone") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".standalone"
            versionNameSuffix = "-独立版"
            matchingFallbacks += listOf("debug")
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.13" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
}

val exportedApkDir = rootProject.layout.projectDirectory.dir(".build/apks")
val exportedApkBaseName = "仪表计算器"

fun exportedProbeVersionName(): String {
    val file = layout.projectDirectory.file("version.properties").asFile
    if (!file.exists()) return "0"
    val props = Properties().apply { file.inputStream().use { load(it) } }
    val base = props.getProperty("VERSION_NAME_BASE") ?: "1.0"
    val code = props.getProperty("VERSION_CODE")?.toIntOrNull() ?: 0
    return "$base.$code"
}

tasks.register("exportProbeDebugApk") {
    dependsOn("packageStandaloneUniversalApk")
    inputs.file(layout.projectDirectory.file("version.properties"))
    outputs.dir(exportedApkDir)
    outputs.upToDateWhen { false }
    doLast {
        val version = exportedProbeVersionName()
        val destDir = exportedApkDir.asFile.apply { mkdirs() }
        val dest = File(destDir, "${exportedApkBaseName}_v${version}_独立版.apk")
        val apksBundle = layout.buildDirectory
            .file("intermediates/incremental/packageStandaloneUniversalApk/universal_bundle.apks")
            .get()
            .asFile
        if (!apksBundle.exists()) throw GradleException("未找到 universal_bundle.apks：${apksBundle.absolutePath}")
        ZipFile(apksBundle).use { zip ->
            val entry = zip.getEntry("universal.apk") ?: throw GradleException("universal_bundle.apks 中缺少 universal.apk")
            zip.getInputStream(entry).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

tasks.register("exportProbeReleaseApk") {
    dependsOn("assembleRelease")
    inputs.file(layout.projectDirectory.file("version.properties"))
    outputs.dir(exportedApkDir)
    outputs.upToDateWhen { false }
    doLast {
        val version = exportedProbeVersionName()
        val destDir = exportedApkDir.asFile.apply { mkdirs() }
        val dest = File(destDir, "${exportedApkBaseName}_v${version}_release_未签名.apk")

        val releaseApkDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val candidates = if (releaseApkDir.exists()) {
            releaseApkDir.listFiles()?.filter { it.isFile && it.extension.equals("apk", ignoreCase = true) } ?: emptyList()
        } else {
            emptyList()
        }
        val src = candidates.firstOrNull { it.name.endsWith("-unsigned.apk", ignoreCase = true) }
            ?: candidates.firstOrNull()
            ?: throw GradleException("Release APK 不存在：${releaseApkDir.absolutePath}")

        src.copyTo(dest, overwrite = true)
    }
}

tasks.register("exportProbeReleaseAab") {
    dependsOn("bundleRelease")
    inputs.file(layout.projectDirectory.file("version.properties"))
    outputs.dir(exportedApkDir)
    outputs.upToDateWhen { false }
    doLast {
        val version = exportedProbeVersionName()
        val destDir = exportedApkDir.asFile.apply { mkdirs() }
        val dest = File(destDir, "${exportedApkBaseName}_v${version}_release.aab")

        val bundleDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        val candidates = if (bundleDir.exists()) {
            bundleDir.listFiles()?.filter { it.isFile && it.extension.equals("aab", ignoreCase = true) } ?: emptyList()
        } else {
            emptyList()
        }
        val src = candidates.firstOrNull() ?: throw GradleException("Release AAB 不存在：${bundleDir.absolutePath}")
        src.copyTo(dest, overwrite = true)
    }
}

tasks.register<Copy>("exportProbeDebugOriginalApk") {
    dependsOn("assembleDebug")
    val versionProvider = providers.provider { exportedProbeVersionName() }
    val src = layout.buildDirectory.file("outputs/apk/debug/probeapp-debug.apk")
    from(src)
    into(exportedApkDir.asFile)
    rename { "${exportedApkBaseName}_v${versionProvider.get()}_debug_同包.apk" }
    inputs.file(layout.projectDirectory.file("version.properties"))
    outputs.upToDateWhen { false }
    doFirst {
        val file = src.get().asFile
        if (!file.exists()) throw GradleException("Debug APK 不存在：${file.absolutePath}")
    }
}
