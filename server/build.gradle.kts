plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    application
}

group = "com.portfolio.ai_challenge"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}
application {
    mainClass.set("com.portfolio.ai_challenge.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.test {
    if (System.getenv("CI") != null) {
        exclude("**/*IntegrationTest*")
    }
    if (project.hasProperty("day10.integration")) {
        systemProperty("day10.integration", project.property("day10.integration").toString())
    }
    environment("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY") ?: "")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.json.jvm)
    implementation(libs.ktor.client.core.jvm)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koog.agents)
    implementation(libs.koin.core)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.ktor.client.contentNegotiation.jvm)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock.jvm)
}