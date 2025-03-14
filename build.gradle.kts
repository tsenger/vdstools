import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.30.0"


}

group = "de.tsenger"
version = "0.9.3"
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
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

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosX64() // iOS simulator
    iosArm64() // iOS device
    iosSimulatorArm64() // iOS simulator on Apple Silicon


    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.kotlincrypto.hash:sha1:0.5.6")
            implementation("com.squareup.okio:okio:3.9.1")
            implementation("co.touchlab:kermit:2.0.4")
            implementation("dev.whyoleg.cryptography:cryptography-core:0.4.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))

        }


        jvmMain.dependencies {
            implementation("org.bouncycastle:bcprov-jdk18on:1.79")
            implementation("org.jetbrains:annotations:26.0.1")
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
            implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:0.4.0")
        }

        jvmTest.dependencies {
            implementation("junit:junit:4.13.2")
            implementation("com.google.zxing:core:3.5.3")
            implementation("com.google.zxing:javase:3.5.3")

        }

        iosMain.dependencies {
            implementation("dev.whyoleg.cryptography:cryptography-provider-openssl3-prebuilt:0.4.0")

        }

        iosTest.dependencies {
            implementation("dev.whyoleg.cryptography:cryptography-provider-openssl3-prebuilt:0.4.0")

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


