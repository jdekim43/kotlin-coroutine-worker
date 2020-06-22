import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    kotlin("jvm") version "1.3.72"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

val artifactName = "kotlin-coroutine-worker"
val artifactGroup = "kr.jadekim"
val artifactVersion = "0.1.0"
group = artifactGroup
version = artifactVersion

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    val kotlinxCoroutineVersion: String by project
    val jLoggerVersion: String by project
    val lettuceExtensionVersion: String by project

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion")

    implementation("kr.jadekim:j-logger:$jLoggerVersion")

    compileOnly("kr.jadekim:lettuce-extension:$lettuceExtensionVersion")
}

tasks.withType<KotlinCompile> {
    val jvmTarget: String by project

    kotlinOptions.jvmTarget = jvmTarget
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    publish = true

    setPublications("lib")

    pkg.apply {
        repo = "maven"
        name = rootProject.name
        setLicenses("MIT")
        setLabels("kotlin", "coroutine", "worker", "batch")
        vcsUrl = "https://github.com/jdekim43/kotlin-coroutine-worker.git"
        version.apply {
            name = artifactVersion
            released = Date().toString()
        }
    }
}