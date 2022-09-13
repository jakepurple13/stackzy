import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.7.0"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev741"
}

val daggerVersion by extra("2.43.2")
val stackzyVersion by extra("1.2.5") // TODO : Change in App.kt also

group = "com.theapache64"
version = stackzyVersion

repositories {
    // mavenLocal()
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit"))
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)

    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.material3)

    // Module dependencies
    implementation(projects.data)

    // Cyclone
    implementation("com.github.theapache64:cyclone:1.0.0-alpha02")

    // Dagger : A fast dependency injector for Android and Java.
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    kaptTest("com.google.dagger:dagger-compiler:$daggerVersion")

    // Decompose : Decompose
    val decomposeVersion = "0.8.0"
    implementation("com.arkivanov.decompose:decompose-jvm:$decomposeVersion")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains-jvm:$decomposeVersion")

    // Color naming (dev purpose only)
    implementation("com.github.theapache64:name-that-color:1.0.0-alpha02")

    // Kamel : Image loading library
    implementation("com.alialbaali.kamel:kamel-image:0.4.1")

    val bouncyCastleVersion = "1.70"
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")

    // Bugsnag
    implementation("com.bugsnag:bugsnag:3.6.4")

    /**
     * Testing Dependencies
     */
    testImplementation("org.mockito:mockito-inline:4.7.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

    // DaggerMock
    testImplementation("com.github.fabioCollini.daggermock:daggermock:0.8.5")
    testImplementation("com.github.fabioCollini.daggermock:daggermock-kotlin:0.8.5")

    // Mockito Core : Mockito mock objects library core API and implementation
    testImplementation("org.mockito:mockito-core:4.7.0")

    // Expekt : An assertion library for Kotlin
    testImplementation("com.github.theapache64:expekt:1.0.0")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}

tasks.test {
    useJUnit()
    // useJUnitPlatform()
    environment("ANDROID_HOME", System.getenv("ANDROID_HOME") ?: "/home/theapache64/Android/Sdk")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "15"
    // kotlinOptions.freeCompilerArgs += "-Xuse-experimental=androidx.compose.foundation.ExperimentalFoundationApi"
    // kotlinOptions.freeCompilerArgs += "-Xuse-experimental=androidx.compose.ui.ExperimentalComposeUiApi"
    // kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.io.path.ExperimentalPathApi"
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    exclude("META-INF/BC1024KE.RSA", "META-INF/BC1024KE.SF", "META-INF/BC1024KE.DSA")
    exclude("META-INF/BC2048KE.RSA", "META-INF/BC2048KE.SF", "META-INF/BC2048KE.DSA")
}

tasks.jar {
    exclude("META-INF/BC1024KE.RSA", "META-INF/BC1024KE.SF", "META-INF/BC1024KE.DSA")
    exclude("META-INF/BC2048KE.RSA", "META-INF/BC2048KE.SF", "META-INF/BC2048KE.DSA")
}


compose.desktop {
    application {
        mainClass = "com.theapache64.stackzy.AppKt"
        nativeDistributions {
            packageName = "Stackzy"
            packageVersion = project.version as String
            description = "An application to identify libraries used inside an android application"
            copyright = "© 2021 theapache64. All rights reserved."
            vendor = "theapache64"
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            modules(
                "java.logging",
                "java.naming",
                "jdk.crypto.ec"
            )
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            val iconsRoot = project.file("src/main/resources/drawables")

            linux {
                iconFile.set(iconsRoot.resolve("launcher_icons/linux.png"))
            }

            windows {
                iconFile.set(iconsRoot.resolve("launcher_icons/windows.ico"))
                // Wondering what the heck is this? See : https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "31575EDF-D0D5-4CEF-A4D2-7562083D6D88"
                menuGroup = packageName
                perUserInstall = true
            }

            macOS {
                iconFile.set(iconsRoot.resolve("launcher_icons/macos.icns"))
            }
        }
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
}

gradle.buildFinished {
    val pkgTasks =
        project.gradle.startParameter.taskNames.filter { it.startsWith("package", ignoreCase = true) }

    val pkgFormat =
        compose.desktop.application.nativeDistributions.targetFormats.firstOrNull { it.isCompatibleWithCurrentOS }
    val nativePkg = buildDir.resolve("compose/binaries").findPkg(pkgFormat?.fileExt)
    val jarPkg = buildDir.resolve("compose/jars").findPkg(".jar")
    nativePkg.ghActionOutput("app_pkg")
    jarPkg.ghActionOutput("uber_jar")
}

fun File.findPkg(format: String?) = when (format != null) {
    true -> walk().firstOrNull { it.isFile && it.name.endsWith(format, ignoreCase = true) }
    else -> null
}

fun File?.ghActionOutput(prefix: String) = this?.let {
    when (System.getenv("GITHUB_ACTIONS").toBoolean()) {
        true -> println(
            """
        ::set-output name=${prefix}_name::${it.name}
        ::set-output name=${prefix}_path::${it.absolutePath}
      """.trimIndent()
        )

        else -> println("$prefix: $this")
    }
}