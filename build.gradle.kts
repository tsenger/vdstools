plugins {
    kotlin("jvm") version "2.1.0"
    java
}

group = "de.tsenger"
version = "0.7.0"

description = "A Java library to encode/sign and decode/verify Visible Digital Seals"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.zxing:core:3.5.3")
    testImplementation("com.google.zxing:javase:3.5.3")

    // Implementation dependencies
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("com.squareup.okio:okio:3.9.1")
    implementation("co.touchlab:kermit:2.0.4")
    implementation("at.asitplus.signum:indispensable:3.12.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")

}

// Optional: Source and Javadoc tasks
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isDeprecation = true
}

tasks.withType<Test> {
    useJUnit()
    testLogging.showStandardStreams = true
}
