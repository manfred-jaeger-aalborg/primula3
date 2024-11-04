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
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true")
    mainClass.set("Experiments.Homophily.WebKB")
}

tasks.named<JavaExec>("run") {
    // Define the output file for logging using the new layout API
    val outputFile = layout.buildDirectory.file("logs/output.log").get().asFile.apply {
        parentFile.mkdirs() // Ensure the directory exists
    }

    // Redirect standard output and error output to the file
    standardOutput = FileOutputStream(outputFile)
    errorOutput = standardOutput // Optionally, also redirect System.err to the same file
}

tasks.test {
    useJUnitPlatform()
}

// tasks.withType<JavaCompile> {
//     options.compilerArgs.add("-Xlint:none") // Disable Java compiler warnings
//     options.isFailOnError = false // Don't fail the build on compilation errors
// }

// tasks.jar {
//     manifest {
//         attributes["Main-Class"] = "RBNgui.Primula"
//     }
// }