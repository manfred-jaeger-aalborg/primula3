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
    implementation("black.ninia:jep:4.2.2")
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
//    mainClass.set("Experiments.Homophily.RiverPollutionMCMC")
    mainClass.set("Experiments.Homophily.Ising")
}

tasks.named<JavaExec>("run") {
    // Disable the default 'run' task to avoid conflict
    enabled = false
}

tasks.register("runPollution") {
    group = "application"
    val vals = listOf(1.0, 0.0)
    var i = 1;
    doLast {
        for (v in vals) {
            javaexec {
                classpath = sourceSets["main"].runtimeClasspath
                mainClass.set("Experiments.Water.RiverPollution")
                args(i.toString(), v.toString())
                i += 1;
            }
        }
    }
}

tasks.register("runPollutionMCMC") {
    group = "application"

    val vals = listOf(15)

    doLast {
        for (i in vals) { // exp
            for (j in 1..5) { // restart
                javaexec {
                    classpath = sourceSets["main"].runtimeClasspath
                    mainClass.set("Experiments.Water.RiverPollutionMCMC")
                    args(i.toString(), j.toString(), "/Users/lz50rg/Dev/water-hawqs/results/results_lambda_2/")
                }
            }
        }
    }
}

tasks.register("runIsing") {
    group = "application"

    val pairs: List<Pair<Double, Double>> = listOf(
            0.9 to 0.05
    )

    val exps = listOf("HP", "HP_noisy")
    val models = listOf("GGCN_raf", "GraphNet", "MLP")

    doLast {

        for (p in pairs) {
            for (e in exps) {
                for (m in models) {
                    for (r in 0..4) {
                        javaexec {
                            classpath = sourceSets["main"].runtimeClasspath
                            mainClass.set("Experiments.Homophily.ising")
                            args(p.first.toString(), p.second.toString(), e, m, r)
                        }
                    }
                }
            }
        }

    }
}

tasks.test {
    useJUnitPlatform()
}
