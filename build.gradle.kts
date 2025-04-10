plugins {
    kotlin("multiplatform") version "2.0.0-RC3"
    kotlin("plugin.serialization") version "2.0.0-RC3"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.mcptools"
version = "0.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

kotlin {
    jvmToolchain(17)
    
    jvm {
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
//
//    js(IR) {
//        moduleName = "mcp-tools-proxy"
//        binaries.executable()
//        nodejs {
//            useCommonJs()
//        }
//    }
//
//    macosArm64 {
//        binaries {
//            executable {
//                entryPoint = "main"
//            }
//        }
//    }
//
//    macosX64 {
//        binaries {
//            executable {
//                entryPoint = "main"
//            }
//        }
//    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.modelcontextprotocol:kotlin-sdk:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("io.github.oshai:kotlin-logging:5.1.0")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:1.4.7")
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation("io.mockk:mockk:1.13.8")
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
                implementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
            }
        }
//
//        val jsMain by getting {
//            dependencies {
//                implementation(npm("shebang-runtime", "^1.0.0"))
//            }
//        }
//
//        val nativeMain by creating {
//            dependsOn(commonMain)
//        }
//
//        val macosArm64Main by getting {
//            dependsOn(nativeMain)
//        }
//
//        val macosX64Main by getting {
//            dependsOn(nativeMain)
//        }
    }
}

application {
    mainClass.set("io.mcptools.proxy.JvmMainKt")
}

tasks {
    shadowJar {
        archiveBaseName.set("mcp-tools-proxy")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
        mergeServiceFiles()
    }
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
