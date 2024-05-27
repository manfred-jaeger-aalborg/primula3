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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
//    mainClass.set("RBNgui.Primula")
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true")
    mainClass.set("Experiments.homophily_mcmc")
}

tasks.test {
    useJUnitPlatform()
}

// tasks.withType<JavaCompile> {
//     options.compilerArgs.add("-Xlint:none") // Disable Java compiler warnings
//     options.isFailOnError = false // Don't fail the build on compilation errors
// }

tasks.jar {
    manifest {
        attributes["Main-Class"] = "RBNgui.Primula"
    }
}