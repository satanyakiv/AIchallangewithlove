plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "com.portfolio.ai_challange_with_love"
version = "1.0.0"
application {
    mainClass.set("com.portfolio.ai_challange_with_love.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
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
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}