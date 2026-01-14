import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

group = "de.tsenger"
version = "0.10.4-SNAPSHOT"
description = "Kotlin multiplatform library to encode/sign and decode/verify Visible Digital Seals"

repositories {
    mavenCentral()
    google()
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/tsenger/vdstools")
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
            jvmTarget.set(JvmTarget.JVM_21)
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
                implementation(libs.okio)
                implementation(libs.kermit)
                implementation(libs.cryptography.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))

        }


        jvmMain.dependencies {
            implementation(libs.bouncycastle.bcprov)
            implementation(libs.jetbrains.annotations)
            implementation(libs.kotlin.stdlib.jdk8)
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

        iosTest.dependencies {
            implementation(libs.cryptography.provider.openssl3.prebuilt)

        }

    }
}



tasks.register<Copy>("copyiOSTestResources") {
    from("src/commonTest/resources")
    into("build/bin/iosSimulatorArm64/debugTest")
}


tasks.findByName("iosSimulatorArm64Test")!!.dependsOn("copyiOSTestResources")

tasks.withType<Test> {
    useJUnit()
    testLogging.showStandardStreams = true
}

tasks.register("generateMappingsFile") {
    doLast {
        val inputFile = file("src/main/resources/countrycodes.csv")
        val outputFile = file("src/main/java/de/tsenger/vdstools/generated/CountryCodeMap.kt")

        val lines = inputFile.readLines().drop(1) // Überspringe die Kopfzeile
        val mappings = mutableListOf<Pair<String, String>>()

        lines.forEach { line ->
            // CSV-Zeile mit Anführungszeichen und Kommas behandeln
            val fields = parseCsvLine(line)
            if (fields.size >= 3) {
                val alpha2 = fields[1].trim()
                val alpha3 = fields[2].trim()
                mappings.add(alpha2 to alpha3)
            }
        }

        // Generiere die Kotlin-Datei mit bidirektionalem Mapping
        outputFile.writeText(
            """
package de.tsenger.vdstools.generated

object CountryCodeMap {
    val alpha2ToAlpha3 = mapOf(
        ${mappings.joinToString(",\n\t\t") { "\"${it.first}\" to \"${it.second}\"" }}
    )

    val alpha3ToAlpha2 = mapOf(
        ${mappings.joinToString(",\n\t\t") { "\"${it.second}\" to \"${it.first}\"" }}
    )
}
""".trimIndent()
        )
        println("Mappings file generated: $outputFile")

    }
}

// CSV-Zeile mit eingebetteten Kommas und Anführungszeichen korrekt parsen
fun parseCsvLine(line: String): List<String> {
    // Regex zum Parsen von Feldern mit oder ohne Anführungszeichen
    val regex = """(?<=^|,)(?:"([^"]*)"|([^",]*))""".toRegex()
    return regex.findAll(line)
        .map { it.groupValues[1].takeIf { str -> str.isNotEmpty() } ?: it.groupValues[2] }
        .toList()
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

        // JSON-Dateien einlesen
        val sealCodings = file("$inputDir/SealCodings.json").readText()
        val idbMessageTypes = file("$inputDir/IdbMessageTypes.json").readText()
        val idbDocumentTypes = file("$inputDir/IdbNationalDocumentTypes.json").readText()

        // Kotlin Code generieren
        val kotlinCode = """
package de.tsenger.vdstools.generated

/**
 * Generated resource constants from JSON files.
 * Do not edit manually - regenerate with: ./gradlew generateResourceConstants
 *
 * Source: src/commonMain/resources/
 */
internal object ResourceConstants {
    const val SEAL_CODINGS_JSON: String = ""${'"'}$sealCodings""${'"'}

    const val IDB_MESSAGE_TYPES_JSON: String = ""${'"'}$idbMessageTypes""${'"'}

    const val IDB_DOCUMENT_TYPES_JSON: String = ""${'"'}$idbDocumentTypes""${'"'}
}
""".trimIndent()

        // Ausgabe-Datei schreiben
        file("$outputDir/de/tsenger/vdstools/generated/ResourceConstants.kt").apply {
            parentFile.mkdirs()
            writeText(kotlinCode)
        }

        println("✅ Generated ResourceConstants.kt with ${sealCodings.length + idbMessageTypes.length + idbDocumentTypes.length} bytes")
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


