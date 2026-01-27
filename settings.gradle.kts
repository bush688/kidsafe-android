pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
rootProject.name = "KidSafe"
include(":childapp")
include(":parentapp")
include(":exceltool")
include(":probeapp")

val shouldBumpProbeVersion = gradle.startParameter.taskNames.any { name ->
    val n = name.lowercase()
    val hitProbe = n.contains(":probeapp:") || n.contains("exportprobe")
    val hitBuild = n.contains("assemble") || n.contains("bundle") || n.contains("exportprobe")
    hitBuild && hitProbe
}

val autoBumpProbeVersion = providers.gradleProperty("autoBumpProbeVersion").orNull?.toBoolean() == true ||
    System.getenv("KIDSAFE_AUTO_BUMP_PROBE_VERSION")?.toBoolean() == true

if (autoBumpProbeVersion && shouldBumpProbeVersion) {
    val versionFile = File(rootDir, "probeapp/version.properties")
    if (versionFile.exists()) {
        val props = java.util.Properties().apply {
            versionFile.inputStream().use { load(it) }
        }
        val base = props.getProperty("VERSION_NAME_BASE") ?: "1.0"
        val code = props.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1
        val nextCode = code + 1
        versionFile.writeText("VERSION_NAME_BASE=$base\nVERSION_CODE=$nextCode\n", Charsets.UTF_8)
    }
}
