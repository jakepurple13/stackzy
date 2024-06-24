plugins {
    kotlin("jvm")
    kotlin("kapt")
}

group = "com.theapache64.stackzy"
version = "1.2.5"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Adam : The ADB client
    api("com.malinskiy.adam:adam:0.4.5")

    // Moshi : A modern JSON API for Android and Java
    val moshiVersion = "1.15.0"
    api("com.squareup.moshi:moshi:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")

    // Retrosheet : Turn Google Spreadsheet to JSON endpoint (for Android and JVM)
    api("com.github.theapache64:retrosheet:2.0.0")

    // Retrofit : A type-safe HTTP client for Android and Java.
    val retrofitVersion = "2.9.0"
    api("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")


    // Kotlinx Coroutines Core : Coroutines support libraries for Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Arbor : Like Timber, just different.
    api("com.ToxicBakery.logging:arbor-jvm:1.37.80")

    val daggerVersion: String by rootProject.extra
    api("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")


    // GooglePlay API
    implementation("com.google.protobuf:protobuf-java:3.21.5")
    api("com.github.theapache64:google-play-api:0.0.9")

    // SnakeYAML : YAML 1.1 parser and emitter for Java
    implementation("org.yaml:snakeyaml:1.30")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    // kotlinOptions.freeCompilerArgs += "-Xuse-experimental=androidx.compose.foundation.ExperimentalFoundationApi"
    // kotlinOptions.freeCompilerArgs += "-Xuse-experimental=androidx.compose.ui.ExperimentalComposeUiApi"
    // kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.io.path.ExperimentalPathApi"
}