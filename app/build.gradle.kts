import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room3)
    jacoco
}

fun loadProperties(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use(::load)
}

// The committed, restricted key (maps.properties); local.properties may override it with your own.
val mapsApiKey: String = loadProperties("local.properties").getProperty("MAPS_API_KEY")
    ?: loadProperties("maps.properties").getProperty("MAPS_API_KEY").orEmpty()

android {
    namespace = "com.sap.codelab"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sap.codelab"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
    }

    // Shared debug keystore (committed): every developer's debug build has the same SHA-1, which the
    // Maps key is restricted to. Standard debug credentials; not a secret.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            // JaCoCo for unit tests: ./gradlew :app:createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // AGP's built-in Kotlin takes its JVM target from these.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            // Robolectric's SDK 36 image reaches into JDK internals that newer JDKs keep closed.
            it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED", "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            // Count classes loaded by Robolectric's sandbox class loader in the coverage report.
            it.extensions.findByType(JacocoTaskExtension::class.java)?.apply {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
        // android.jar is only stubs in unit tests; Compose (Molecule ViewModel tests) calls Trace and Log.
        unitTests.isReturnDefaultValues = true
        // Robolectric tests (Compose UI, notifications, receivers) need the merged resources.
        unitTests.isIncludeAndroidResources = true
    }

    packaging.resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*.md")
}

configure<androidx.room3.gradle.RoomExtension> {
    schemaDirectory(layout.projectDirectory.dir("schemas"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose (versions only from the BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest) // ComponentActivity for Compose UI tests

    // Lifecycle, navigation
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Data
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Kotlin
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.timber)

    // Unit tests: JUnit Jupiter + MockK + Turbine + Molecule
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.molecule.runtime)
    testImplementation(libs.kotlinx.coroutines.test)
    // Android-bound tests on the JVM: Robolectric (JUnit 4, run by the Vintage engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}

// Coverage of hand-written code: generated classes (Hilt, Room, Compose singletons, R) don't count.
// ./gradlew :app:coverageReport → build/reports/jacoco/coverageReport/html/index.html
// ./gradlew :app:coverageCheck  → fails below 80% line coverage
val coverageExcludes = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/Hilt_*.*", "**/HiltWrapper_*.*", "**/*_HiltModules*.*", "**/*_Factory*.*", "**/*_MembersInjector.*",
    "**/*_GeneratedInjector.*", "**/*_AssistedFactory*.*", "**/Dagger*.*", "hilt_aggregated_deps/**", "dagger/hilt/**",
    "**/*_Impl*.*", "**/ComposableSingletons*.*",
)
// Our code is all Kotlin; the javac output holds only Hilt's generated Java.
val coverageClasses = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
    exclude(coverageExcludes)
}
val coverageData = files(layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))

tasks.register<JacocoReport>("coverageReport") {
    group = "verification"
    description = "Unit test coverage of hand-written code (HTML + XML)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(coverageData)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("coverageCheck") {
    group = "verification"
    description = "Fails when line coverage of hand-written code is below 80%."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses)
    executionData.setFrom(coverageData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
