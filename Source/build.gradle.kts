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
    implementation("black.ninia:jep:4.2.1")
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
    mainClass.set("Experiments.Homophily.NodeClass")
}

tasks.named<JavaExec>("run") {
    // Disable the default 'run' task to avoid conflict
    enabled = false
}

tasks.register("runNodeClass") {
    group = "application"

    doLast {
        for (i in 0..9) {
            exec {
                commandLine(
                        "xvfb-run", "-a",
                        "java", "-cp", sourceSets["main"].runtimeClasspath.asPath,
                        "Experiments.Homophily.NodeClass", i.toString()
                )
            }
        }
    }
}


tasks.register("runExperiment") {
    group = "application"

    doLast {
        val dataset = project.findProperty("dataset")?.toString() ?: "wisconsin"
        val nfeat = project.findProperty("nfeat")?.toString() ?: "1703"
        val model = project.findProperty("modelName")?.toString() ?: "GCN"
        val nhid = project.findProperty("nhid")?.toString() ?: "16"
        val nlayer = project.findProperty("nlayer")?.toString() ?: "2"
        val nclass = project.findProperty("nclass")?.toString() ?: "5"
        val decayRate = project.findProperty("decayRate")?.toString() ?: "1.0"
        val expName = project.findProperty("expName")?.toString() ?: "exp"
        val homType = project.findProperty("homType")?.toString() ?: "hom"

        for (i in 0..9) {
            exec {
                commandLine(
                    "java",
                    "-cp", sourceSets["main"].runtimeClasspath.asPath,
                    "Experiments.Homophily.NodeClass", i.toString(), dataset, nfeat, model, nhid, nlayer, nclass, decayRate, expName, "homGT"
                )
            }
        }

//        for (i in 0..9) {
//            exec {
//                commandLine(
//                        "java",
//                        "-cp", sourceSets["main"].runtimeClasspath.asPath,
//                        "Experiments.Homophily.NodeClass", i.toString(), dataset, nfeat, model, nhid, nlayer, nclass, decayRate, expName, "homProp"
//                )
//            }
//        }
    }
}


tasks.register("runIsing") {
    group = "application"

    doLast {
        exec {
            commandLine(
//                    "xvfb-run", "-a",
                    "java", "-cp", sourceSets["main"].runtimeClasspath.asPath,
                    "Experiments.Homophily.ising"
            )
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
