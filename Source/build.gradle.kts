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
                    "Experiments.Homophily.NodeClass", i.toString(), dataset, nfeat, model, nhid, nlayer, nclass, decayRate, expName, "homLP"
                )
            }
        }

        for (i in 0..9) {
            exec {
                commandLine(
                        "java",
                        "-cp", sourceSets["main"].runtimeClasspath.asPath,
                        "Experiments.Homophily.NodeClass", i.toString(), dataset, nfeat, model, nhid, nlayer, nclass, decayRate, expName, "homProp"
                )
            }
        }
    }
}

tasks.register("runAllExperiments") {
    group = "application"

    doLast {
        val datasets = listOf(
                DatasetConfig("texas", nfeat = "1703", nclass = "5"),
                DatasetConfig("wisconsin", nfeat = "1703", nclass = "5"),
                DatasetConfig("film", nfeat = "932", nclass = "5"),
                DatasetConfig("squirrel", nfeat = "2089", nclass = "5"),
                DatasetConfig("chameleon", nfeat = "2325", nclass = "5"),
                DatasetConfig("cornell", nfeat = "1703", nclass = "5"),
                DatasetConfig("citeseer", nfeat = "3703", nclass = "6"),
//                DatasetConfig("pubmed", nfeat = "500", nclass = "3"),
                DatasetConfig("cora", nfeat = "1433", nclass = "7")
        )

        val models = listOf(
                ModelConfig(
                        "GCN",
                        datasetSettings = mapOf(
                                "texas" to ModelSettings(nhid = "16", nlayer = "2"),
                                "wisconsin" to ModelSettings(nhid = "16", nlayer = "2"),
                                "film" to ModelSettings(nhid = "16", nlayer = "2"),
                                "squirrel" to ModelSettings(nhid = "16", nlayer = "2"),
                                "chameleon" to ModelSettings(nhid = "16", nlayer = "2"),
                                "cornell" to ModelSettings(nhid = "16", nlayer = "2"),
                                "citeseer" to ModelSettings(nhid = "16", nlayer = "2"),
//                                "pubmed" to ModelSettings(nhid = "16", nlayer = "2"),
                                "cora" to ModelSettings(nhid = "16", nlayer = "2"),
                        )
                ),
                ModelConfig(
                        "GGCN",
                        datasetSettings = mapOf(
                                "texas" to ModelSettings(nhid = "16", nlayer = "2"),
                                "wisconsin" to ModelSettings(nhid = "16", nlayer = "5"),
                                "film" to ModelSettings(nhid = "32", nlayer = "4", decayRate = "1.2"),
                                "squirrel" to ModelSettings(nhid = "32", nlayer = "2"),
                                "chameleon" to ModelSettings(nhid = "32", nlayer = "5", decayRate = "0.8"),
                                "cornell" to ModelSettings(nhid = "16", nlayer = "6", decayRate = "0.7"),
                                "citeseer" to ModelSettings(nhid = "80", nlayer = "10", decayRate = "0.02"),
//                                "pubmed" to ModelSettings(nhid = "32", nlayer = "5", decayRate = "1.1", useSparse="true"),
                                "cora" to ModelSettings(nhid = "16", nlayer = "32", decayRate = "0.9"),
                        )
                )
        )

        val homTypes = listOf("homLP", "homProp")

        for (datasetConfig in datasets) {
            val datasetName = datasetConfig.name
            val datasetNfeat = datasetConfig.nfeat
            val datasetNclass = datasetConfig.nclass

            for (modelConfig in models) {
                val modelName = modelConfig.name
                val modelSettingsForDataset = modelConfig.datasetSettings[datasetName]

                if (modelSettingsForDataset != null) {
                    val modelNhid = modelSettingsForDataset.nhid
                    val modelNlayer = modelSettingsForDataset.nlayer
                    val modelDecayRate = modelSettingsForDataset.decayRate
                    val useSparse = modelSettingsForDataset.useSparse

                    for (homType in homTypes) {
                        for (i in 0..9) {
                            exec {
                                commandLine(
                                        "java",
                                        "-cp", sourceSets["main"].runtimeClasspath.asPath,
                                        "Experiments.Homophily.NodeClass",
                                        i.toString(),
                                        datasetName,
                                        datasetNfeat,
                                        modelName,
                                        modelNhid,
                                        modelNlayer,
                                        datasetNclass,
                                        modelDecayRate,
                                        useSparse,
                                        "realDataset",
                                        homType
                                )
                            }
                        }
                    }
                } else {
                    println("Warning: No settings defined for model '${modelName}' on dataset '${datasetName}'. Skipping this combination.")
                }
            }
        }
    }
}

data class DatasetConfig(
        val name: String,
        val nfeat: String,
        val nclass: String
)

data class ModelSettings(
        val nhid: String,
        val nlayer: String,
        val decayRate: String? = "1.0",
        val useSparse: String? = "false"
)
data class ModelConfig(
        val name: String,
        val datasetSettings: Map<String, ModelSettings>
)


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
