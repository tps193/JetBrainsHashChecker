plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.9.0"
//    id("kotlinx.serialization") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
}

group = "org.shadrin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
//  maven("https://www.jetbrains.com/intellij-repository/releases")
//    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(
       "com.intellij.gradle",
    ))
}

dependencies {
    implementation(fileTree("/Users/sergey/Library/Application Support/JetBrains/IntelliJIdea2021.3/plugins/package-checker/"))
   // implementation(fileTree(include: ['*.jar'], dir: '/Users/sergey/Library/Application Support/JetBrains/IntelliJIdea2021.3/plugins/package-checker/'))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("com.squareup.okhttp:okhttp:2.7.5") // To let the code compile
//    implementation("com.jetbrains.intellij.platform:jps-model-serialization:182.2949.4")
//    implementation("com.jetbrains.intellij.platform:jps-model-impl:182.2949.4")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("223.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
