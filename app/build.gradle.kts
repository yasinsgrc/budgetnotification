import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// --- Release imzalama ---
// Anahtar deposu ve parolalar repoda YOK ve olmamali (.gitignore: *.jks,
// *.keystore, keystore.properties). Iki kaynak okunuyor; ortam degiskeni once
// geliyor (CI sirri), yoksa kokteki keystore.properties (yerel makine).
// Hicbiri yoksa release derlemesi IMZASIZ cikar ve `assembleRelease` yine de
// calisir - CI'in imzalama sirri olmadan da R8/kucultme yolunu derleyebilmesi
// gerekiyor. Ornek dosya: keystore.properties.example
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingSecret(envName: String, propertyName: String): String? =
    (System.getenv(envName) ?: keystoreProperties.getProperty(propertyName))
        ?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingSecret("BB_KEYSTORE_FILE", "storeFile")
val releaseStorePassword = signingSecret("BB_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingSecret("BB_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingSecret("BB_KEY_PASSWORD", "keyPassword")
val releaseSigningReady = releaseStoreFile != null && releaseStorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.bildirimbutce.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bildirimbutce.app"
        minSdk = 26          // NotificationListenerService + isimli regex gruplari icin gerekli
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Sir yoksa signingConfig atanmiyor: imzasiz APK uretilir, derleme
            // kirilmaz. Yayin oncesi `releaseSigningReady` true olmali - asagidaki
            // uyari basiliyorsa yuklenecek APK/AAB imzasizdir.
            signingConfig = if (releaseSigningReady) signingConfigs.getByName("release") else null

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Robolectric testleri gercek assets'i (patterns.json) okuyor; bu bayrak
    // olmadan asset yukleyici bos doner ve testler emulator ister.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true

            all {
                // Conscrypt yerel kutuphane adini varsayilan yerel ayarla
                // kucultuyor; Turkce yerel ayarda "windows" -> "wındows" olur,
                // kutuphane bulunamaz ve tum Robolectric testleri patlar.
                // Testlerin TLS'e ihtiyaci yok.
                it.systemProperty("robolectric.conscryptMode", "OFF")
            }
        }
    }

    // Tek kaynak: kokteki patterns/patterns.json dogrudan assets olarak paketlenir.
    sourceSets["main"].assets.srcDir(rootProject.file("patterns"))
}

// Room sema dosyalari surum yukseltmelerinde migration yazmak icin gerekli;
// git'e commit edilmeleri gerekir.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":parser"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Imzasiz bir release APK'si sessizce uretilirse Play'e yuklenene kadar fark
// edilmez. Uyari yalnizca release gorevi calisirken basiliyor - her `assembleDebug`
// ciktisini kirletmesin.
gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { it.project == project && it.name.contains("Release") }
    if (buildsRelease && !releaseSigningReady) {
        logger.warn(
            "UYARI: release imzalama yapilandirilmadi (keystore.properties yok ve " +
                "BB_KEYSTORE_* ortam degiskenleri bos). Cikan APK/AAB IMZASIZ - " +
                "Play Console kabul etmez. keystore.properties.example'a bakin."
        )
    }
}
