plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

fun quoted(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
val productionUrl = providers.gradleProperty("GASTRACK_PRODUCTION_URL").orElse("")
android {
    namespace = "com.gastrack.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gastrack.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", quoted(providers.gradleProperty("GASTRACK_DEV_URL").orElse("http://10.0.2.2:4000/").get()))
            manifestPlaceholders["cleartextAllowed"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "API_BASE_URL", quoted(productionUrl.get()))
            manifestPlaceholders["cleartextAllowed"] = "false"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kapt { correctErrorTypes = true }
val bundleWebFrontend by tasks.registering(Exec::class) {
    val webRoot = rootProject.projectDir.parentFile
    workingDir(webRoot)
    inputs.files(fileTree(webRoot.resolve("src")), fileTree(webRoot.resolve("public")))
    inputs.files(webRoot.resolve("package.json"), webRoot.resolve("package-lock.json"), webRoot.resolve("vite.config.js"), webRoot.resolve("scripts/build-android-web.mjs"))
    outputs.dir(projectDir.resolve("src/main/assets/web"))
    if (System.getProperty("os.name").startsWith("Windows")) commandLine("cmd", "/c", "npm.cmd", "run", "build:android")
    else commandLine("npm", "run", "build:android")
}
tasks.named("preBuild") { dependsOn(bundleWebFrontend) }
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst { require(productionUrl.get().startsWith("https://")) { "Set -PGASTRACK_PRODUCTION_URL=https://your-api-host/ for release builds" } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-compiler:2.56.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
