import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

group = "de.tsenger"
version = "0.22.0"
description = "Kotlin multiplatform library to encode/sign and decode/verify Visible Digital Seals"

repositories {
    mavenCentral()
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/tsenger/vdstools")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "giteaPackages"
            url = uri("https://gitea.t-senger.de/api/packages/tsenger/maven")
            credentials(PasswordCredentials::class)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "vdstools",
        version = version.toString()
    )

    pom {
        name = "Visible Digital Seal Tools"
        description = "Kotlin multiplatform library to encode/sign and decode/verify Visible Digital Seals"
        inceptionYear = "2024"
        url = "https://github.com/tsenger/vdstools"

        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        organization {
            name = "tsenger.de"
            url = "https://www.tsenger.de"
        }

        developers {
            developer {
                name = "Tobias Senger"
                email = "info@tsenger.de"
            }
        }

        scm {
            url = "https://github.com/tsenger/vdstools"
            connection = "scm:git:git://github.com/tsenger/vdstools.git"
        }

    }

}



kotlin {

    jvmToolchain(21)

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64() // iOS simulator
    iosArm64() // iOS device
    iosSimulatorArm64() // iOS simulator on Apple Silicon

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/kotlin/resourceConstants")
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.xmlutil.core)
                implementation(libs.okio)
                implementation(libs.cryptography.core)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))

        }

        jvmMain.dependencies {
            implementation(libs.bouncycastle.bcprov)
            implementation(libs.cryptography.provider.jdk)
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.zxing.core)
            implementation(libs.zxing.javase)

        }

        iosMain.dependencies {
            implementation(libs.cryptography.provider.openssl3.prebuilt)

        }

    }
}


tasks.register<Copy>("copyiOSTestResources") {
    from("src/commonTest/resources")
    into("build/bin/iosSimulatorArm64/debugTest")
}


tasks.named("iosSimulatorArm64Test") { dependsOn("copyiOSTestResources") }

tasks.withType<Test> {
    useJUnit()
    testLogging.showStandardStreams = true
}

tasks.register("generateResourceConstants") {
    description = "Generate Kotlin constants from JSON resources"
    group = "build"

    val inputDir = file("src/commonMain/resources")
    val outputDir = file("build/generated/kotlin/resourceConstants")

    inputs.dir(inputDir)
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()

        // Definitionen je Spezifikation: <spec>/spec.json + <spec>/<Kind>.json.
        // Das Land steht nur in spec.json und wird in jeden Eintrag übernommen; internationale zuerst.
        @Suppress("UNCHECKED_CAST")
        fun readJson(f: File): Any = groovy.json.JsonSlurper().parse(f)
        val specs = inputDir.listFiles { f -> f.isDirectory }!!
            .map { it.name to (readJson(file("$it/spec.json")) as Map<String, Any?>)["country"] as String? }
            .sortedWith(compareBy({ it.second != null }, { it.first }))
        val tripleQuote = "\"\"\""
        fun constant(kind: String, name: String): String {
            val entries = specs.filter { file("$inputDir/${it.first}/$kind.json").exists() }.joinToString(",\n") { (spec, country) ->
                val path = "$spec/$kind.json"
                @Suppress("UNCHECKED_CAST")
                val list = (readJson(file("$inputDir/$path")) as List<Map<String, Any?>>).map { entry ->
                    require("country" !in entry) { "$path: 'country' belongs in $spec/spec.json, not in an entry" }
                    if (country == null) entry else entry + ("country" to country)
                }
                "        // $path\n        $tripleQuote${groovy.json.JsonOutput.toJson(list)}$tripleQuote"
            }
            return "    val $name: List<String> = listOf(\n$entries\n    )"
        }

        // Kotlin Code generieren
        val kotlinCode = """
package de.tsenger.vdstools.generated

/**
 * Generated resource constants from JSON files.
 * Do not edit manually - regenerate with: ./gradlew generateResourceConstants
 *
 * Source: src/commonMain/resources/<specification>/, international first. Each list is loaded in order.
 */
internal object ResourceConstants {
${constant("VdsDocumentTypes", "VDS_DOCUMENT_TYPES_JSON")}

${constant("IdbMessageTypes", "IDB_MESSAGE_TYPES_JSON")}

${constant("IdbDocumentTypes", "IDB_DOCUMENT_TYPES_JSON")}
}
""".trimStart()

        // Ausgabe-Datei schreiben
        file("$outputDir/de/tsenger/vdstools/generated/ResourceConstants.kt").apply {
            parentFile.mkdirs()
            writeText(kotlinCode)
        }

        println("Generated ResourceConstants.kt from ${specs.map { it.first }}")
    }
}

// Task an Kotlin-Compilation und Source-Packaging binden
tasks.matching { task ->
    (task.name.startsWith("compile") && task.name.contains("Kotlin")) ||
            task.name.endsWith("SourcesJar") ||
            task.name == "sourcesJar"
}.configureEach {
    dependsOn("generateResourceConstants")
}


