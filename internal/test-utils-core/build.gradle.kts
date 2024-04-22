plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
}

addAllMultiplatformTargets()
androidLibrary(nameSpace = "com.github.panpf.sketch.test.utils")

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(kotlin("test"))
            api(projects.sketchCore)
            api(projects.internal.images)
            api(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            api(projects.sketchViewCore)
            api(libs.androidx.fragment)
            api(libs.androidx.test.runner)
            api(libs.androidx.test.rules)
            api(libs.androidx.test.ext.junit)
            api(libs.junit)
            api(libs.panpf.tools4a.device)
            api(libs.panpf.tools4a.dimen)
            api(libs.panpf.tools4a.display)
            api(libs.panpf.tools4a.network)
            api(libs.panpf.tools4a.run)
            api(libs.panpf.tools4a.test)
            api(libs.panpf.tools4j.reflect)
            api(libs.panpf.tools4j.security)
            api(libs.panpf.tools4j.test)
        }
    }
}