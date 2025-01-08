plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    //java
}

group = "de.tsenger"
version = "0.8.0"
description = "A JVM library to encode/sign and decode/verify Visible Digital Seals"

repositories {
    mavenCentral()
}



dependencies {
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.zxing:core:3.5.3")
    testImplementation("com.google.zxing:javase:3.5.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")

    // Implementation dependencies
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okio:okio:3.9.1")
    implementation("co.touchlab:kermit:2.0.4")

}

// Optional: Source and Javadoc tasks
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

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


