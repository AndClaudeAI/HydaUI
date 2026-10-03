import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/** CI run number; doubles as versionCode so every build is an upgrade of the last. */
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

/**
 * Fingerprint of everything that ends up as *code* in the APK. Look-and-feel lives in
 * /config, outside this set, so a config-only change keeps the fingerprint and phones apply it
 * live instead of reinstalling.
 */
val codeVersion: String = run {
    val digest = MessageDigest.getInstance("SHA-256")
    val inputs = fileTree("src/main").files + file("build.gradle.kts") + file("proguard-rules.pro")
    inputs.sortedBy { it.relativeTo(projectDir).invariantSeparatorsPath }.forEach { f ->
        digest.update(f.relativeTo(projectDir).invariantSeparatorsPath.toByteArray())
        digest.update(f.readBytes())
    }
    digest.digest().joinToString("") { "%02x".format(it) }.take(16)
}

/** The repo's look-and-feel config, stamped with this build's number and shipped as an asset. */
val generatedConfigDir = layout.buildDirectory.dir("generated/hydaConfig").get().asFile
run {
    val source = rootProject.file("config/hyda-config.json").readText().trim()
    require(source.startsWith("{")) { "config/hyda-config.json must be a JSON object" }
    generatedConfigDir.mkdirs()
    File(generatedConfigDir, "hyda-config.json")
        .writeText("{\n  \"revision\": $buildNumber,\n" + source.removePrefix("{").trimStart('\n', '\r'))
}

android {
    namespace = "com.hydaui.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hydaui.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
        buildConfigField("String", "CODE_VERSION", "\"$codeVersion\"")
        buildConfigField("String", "UPDATE_REPO", "\"AndClaudeAI/HydaUI\"")
    }

    sourceSets["main"].assets.srcDir(generatedConfigDir)

    signingConfigs {
        // The update key lives outside git: CI decodes it from the HYDA_KEYSTORE_BASE64 secret.
        // Without it (local builds, forks) release falls back to the debug key, and CI won't
        // offer that APK to phones, since it couldn't update a real install anyway.
        val keystore = System.getenv("HYDA_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
        if (keystore != null) {
            create("hyda") {
                storeFile = keystore
                storePassword = System.getenv("HYDA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HYDA_KEY_ALIAS") ?: "hydaui"
                keyPassword = System.getenv("HYDA_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("hyda") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/** Writes build/update/update.json: what phones read to decide whether to install this build. */
tasks.register("writeUpdateManifest") {
    dependsOn("assembleRelease")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        val sha = MessageDigest.getInstance("SHA-256").digest(apk.readBytes())
            .joinToString("") { "%02x".format(it) }
        val notes = System.getenv("HYDA_NOTES_FILE")?.let { File(it).takeIf(File::exists)?.readText() }.orEmpty()
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val out = layout.buildDirectory.file("update/update.json").get().asFile
        out.parentFile.mkdirs()
        out.writeText(
            """
            {
              "versionCode": $buildNumber,
              "versionName": "0.1.$buildNumber",
              "codeVersion": "$codeVersion",
              "apkSha256": "$sha",
              "notes": "${esc(notes.trim())}"
            }
            """.trimIndent() + "\n",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
