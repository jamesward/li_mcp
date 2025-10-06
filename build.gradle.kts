plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jamesward"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webflux")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
//    implementation("com.microsoft.playwright:playwright:1.55.0")
    implementation("com.microsoft.playwright:playwright:1.52.0")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.1.0-M3")
    }
}

// disable the plain jar because it confuses Heroku
tasks.named<Jar>("jar") {
    enabled = false
}

/*
tasks.register<JavaExec>("installChromiumDeps") {
    mainClass = "com.microsoft.playwright.CLI"
    args = listOf("install-deps", "chromium")
    classpath = sourceSets["main"].runtimeClasspath
}
 */

tasks.register<JavaExec>("installChromium") {
//    dependsOn("installChromiumDeps")
    mainClass = "com.microsoft.playwright.CLI"
    args = listOf("install", "--with-deps", "--only-shell") //install", "chromium")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.named("assemble").configure {
    dependsOn("installChromium")
}
