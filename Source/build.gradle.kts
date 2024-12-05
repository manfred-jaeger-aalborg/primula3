import java.io.FileOutputStream
import java.io.File

plugins {
    id("java")
    id("application")
}

group = "org.primula"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.dom4j:dom4j:2.1.3")
    implementation("black.ninia:jep:4.2.0")
    //implementation(kotlin("script-runtime"))

}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
//    mainClass.set("RBNgui.Primula")
    // applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true")
    mainClass.set("Experiments.Homophily.WebKB")
}

tasks.named<JavaExec>("run") {
    // Disable the default 'run' task to avoid conflict
    enabled = false
}

tasks.register("runWithArgs") {
    group = "application"

    doLast {
        for (i in 0..9) {
            exec {
                commandLine(
//                        "java",
//                        "-XX:+UseG1GC",
                        // "-XX:+UseZGC", // Use Z Garbage Collector (aggressive GC)
                        // "-XX:SoftRefLRUPolicyMSPerMB=50", // Aggressively clean soft references
                        // "-Xms256m", // Set initial heap size
                        // "-Xmx512m", // Set maximum heap size
                        // "-XX:+ExplicitGCInvokesConcurrent", // Allow explicit GC calls to run concurrently
                        // "-XX:+ParallelRefProcEnabled", // Parallelize reference processing
                        "java", "-cp", sourceSets["main"].runtimeClasspath.asPath,
                        "Experiments.Homophily.WebKB", i.toString()
                )
            }
        }
    }
}

tasks.register<JavaExec>("runPollution") {
    group = "application"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.Water.RiverPollution")
}

tasks.test {
    useJUnitPlatform()
}
