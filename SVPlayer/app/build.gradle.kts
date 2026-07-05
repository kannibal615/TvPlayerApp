import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localString(name: String): String = localProperties.getProperty(name).orEmpty()

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

fun localBoolean(name: String): Boolean =
    localString(name).equals("true", ignoreCase = true)

fun localAdString(name: String): String =
    localString(name).trim().removeSurrounding("\"").removeSurrounding("'")

fun productionVideoAdTagUrl(): String =
    localAdString("VIDEO_AD_TAG_URL").ifBlank { localAdString("HILLTOPADS_VAST_TAG_URL") }

fun activationBaseUrl(): String {
    val configured = localString("DOMAINE_SERVER").ifBlank { "smartvisions.net" }
    val normalized = configured.trim().trimEnd('/')
    val withScheme = if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
        normalized
    } else {
        "https://$normalized"
    }
    return "$withScheme/"
}

android {
    namespace = "com.smartvision.svplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.smartvision.svplayer"
        minSdk = 23
        targetSdk = 36
        versionCode = 104
        versionName = "0.1.100"
        manifestPlaceholders["profileableByShell"] = "false"

        buildConfigField("String", "ACTIVATION_BASE_URL", buildConfigString(activationBaseUrl()))
        buildConfigField("String", "YOUTUBE_API_KEY", buildConfigString(localAdString("YOUTUBE_API_KEY")))
        // PERF_DIAG: disabled by default; only releaseDiagnostic writes local performance artifacts.
        buildConfigField("boolean", "PERF_DIAGNOSTICS_ENABLED", "false")
        buildConfigField("String", "PERF_DIAGNOSTICS_LABEL", buildConfigString(""))
    }

    signingConfigs {
        create("release") {
            val storePath = localString("RELEASE_STORE_FILE")
            if (storePath.isNotBlank()) {
                storeFile = rootProject.file(storePath)
                storePassword = localString("RELEASE_STORE_PASSWORD")
                keyAlias = localString("RELEASE_KEY_ALIAS")
                keyPassword = localString("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            val testAppId = "ca-app-pub-3940256099942544~3347511713"
            val testVideoTag =
                "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/" +
                    "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1" +
                    "&output=vast&unviewed_position_start=1&env=vp&correlator="
            manifestPlaceholders["googleAdsApplicationId"] = testAppId
            buildConfigField("String", "GOOGLE_ADS_APPLICATION_ID", buildConfigString(testAppId))
            buildConfigField("String", "VIDEO_AD_TAG_URL", buildConfigString(testVideoTag))
            buildConfigField("boolean", "ADS_RUNTIME_CONFIGURED", "true")
            buildConfigField(
                "String",
                "DEBUG_MONETIZATION_STATUS",
                buildConfigString(localString("SMARTVISION_DEBUG_MONETIZATION_STATUS")),
            )
            buildConfigField(
                "boolean",
                "DEBUG_FORCE_AD_FAILURE",
                localBoolean("SMARTVISION_DEBUG_AD_FAILURE").toString(),
            )
        }
        getByName("release") {
            val productionAppId = localString("GOOGLE_ADS_APPLICATION_ID")
            val productionVideoTag = productionVideoAdTagUrl()
            manifestPlaceholders["googleAdsApplicationId"] = productionAppId
            buildConfigField("String", "GOOGLE_ADS_APPLICATION_ID", buildConfigString(productionAppId))
            buildConfigField("String", "VIDEO_AD_TAG_URL", buildConfigString(productionVideoTag))
            buildConfigField(
                "boolean",
                "ADS_RUNTIME_CONFIGURED",
                productionVideoTag.isNotBlank().toString(),
            )
            buildConfigField("String", "DEBUG_MONETIZATION_STATUS", buildConfigString(""))
            buildConfigField("boolean", "DEBUG_FORCE_AD_FAILURE", "false")
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("releaseFast") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("releaseDiagnostic") {
            // PERF_DIAG: local-only build used for Firestick Splash/Home captures. Do not deploy this variant.
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("release")
            versionNameSuffix = "-diag"
            manifestPlaceholders["profileableByShell"] = "true"
            buildConfigField("boolean", "PERF_DIAGNOSTICS_ENABLED", "true")
            buildConfigField("String", "PERF_DIAGNOSTICS_LABEL", buildConfigString("splash-home"))
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("releaseOptimized") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    kotlin {
        jvmToolchain(21)
    }
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.tv:tv-material:1.0.0")

    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    implementation("androidx.room:room-ktx:2.7.1")
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
