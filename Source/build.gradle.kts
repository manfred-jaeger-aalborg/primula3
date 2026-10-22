import java.io.FileOutputStream
import java.io.File

plugins {
    id("java")
    id("application")
}

group = "org.primula"
version = "PRIMULA"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.dom4j:dom4j:2.1.3")
    implementation("black.ninia:jep:4.3.1")
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
    mainClass.set("RBNgui.PrimulaGUI")
    // applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true")
//    mainClass.set("Experiments.Homophily.RiverPollutionMCMC")
//    mainClass.set("Experiments.Homophily.Ising")
}

tasks.named<JavaExec>("run") {
    enabled = true
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "RBNgui.PrimulaGUI"
    }
    archiveFileName.set("primula.jar")
    // Include runtime dependencies in the JAR.
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }
                .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(file("../JAR"))
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

tasks.register<JavaExec>("runIsingPipelineSingle") {
    group = "application"
    description = "Run ising_pipeline_single; pass -PappArgs='basePath N J Jb temp withNoise model restart compiled'"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.Homophily.ising_pipeline_single")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runSecondTryMCMC") {
    group = "application"
    description = "Run MCMCPollSecondTry against temporal-logic/primula_out; pass -PappArgs='rainMode limit' " +
            "(both optional, e.g. -PappArgs=\"observed 1\" for a quick one-variant smoke test)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollSecondTry")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runMinimalMCMC") {
    group = "application"
    description = "Run MCMCPollMinimal against temporal-logic/minimal/primula_out; pass -PappArgs='rainMode limit' " +
            "(both optional, e.g. -PappArgs=\"observed 1\" for a quick one-variant smoke test)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollMinimal")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runFarmerMCMC") {
    group = "application"
    description = "Run MCMCPollFarmer against temporal-logic/primula_out_farmer; pass -PappArgs='rainMode limit variantId horizons' " +
            "(all optional, e.g. -PappArgs=\"observed 0 5 short,med,long\" for one specific variant)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollFarmer")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runFarmerClusterMCMC") {
    group = "application"
    description = "Run MCMCPollFarmerCluster against temporal-logic/primula_out_farmer_cluster (42-class, " +
            "NodeId, PERIOD=10 cluster run); pass -PappArgs='rainMode limit variantId horizons numSamples outDir labelSet resultsDir' " +
            "(all optional, e.g. -PappArgs=\"observed 0 6 m3,m6,m12 1000\" for one specific variant with " +
            "1000 MCMC samples, or -PappArgs=\"observed 100\" for every variant found, up to 100; " +
            "numSamples defaults to 150; outDir (no spaces) defaults to primula_out_farmer_cluster, e.g. " +
            "-PappArgs=\"submodel 0 5 m3,m6,m12 150 /Users/lz50rg/Dev/temporal-logic/primula_out_farmer700_L1\" " +
            "to point at a different generate_rbn.py out-dir, e.g. to compare GNN bridge variants, without recompiling; " +
            "labelSet = fine (default) | macro (LOW/MED/HIGH, -> mcmc_<variant>_macro.csv) | all | rel=NAME[,NAME]; " +
            "resultsDir = results subfolder of outDir (default mcmc_results))"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollFarmerCluster")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runBasicMCMC") {
    group = "application"
    description = "Run MCMCPollBasic against temporal-logic/primula_out_basic; pass -PappArgs='limit variantId horizons' " +
            "(all optional, e.g. -PappArgs=\"0 0 short,med,long\" for one specific variant)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollBasic")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runBasicAdvMCMC") {
    group = "application"
    description = "Run MCMCPollBasicAdv against temporal-logic/primula_out_basic_adv; pass -PappArgs='limit variantId horizons numSamples' " +
            "(all optional, e.g. -PappArgs=\"0 5 k5,k10,k20 400\" for variant 5)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollBasicAdv")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runBasicAdvSingleQuery") {
    group = "application"
    description = "Run MCMCSingleQuery against temporal-logic/primula_out_basic_adv for one ad hoc " +
            "eventually_<label>_<horizon>[node] atom; pass -PappArgs='variantId node label horizon numSamples' " +
            "(e.g. -PappArgs=\"5 24 HIGH k20 400\")"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCSingleQuery")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runNeverHighMCMC") {
    group = "application"
    description = "Run MCMCPollNeverHigh against temporal-logic/primula_out_basic; pass -PappArgs='variantId horizons numSamples' " +
            "(e.g. -PappArgs=\"5 k5,k10,k20,k50 400\") -- .rdef must be generated with --never-high"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollNeverHigh")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runCustomMCMC") {
    group = "application"
    description = "Run MCMCPollCustom against temporal-logic/primula_out_basic; pass -PappArgs='variantId relNames_comma_sep numSamples' " +
            "for arity-1 (sub) relations MCMCPollBasic doesn't know about, e.g. -PappArgs=\"5 always_C37_n9_s10_13 200\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollCustom")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runBasicAdvCustomMCMC") {
    group = "application"
    description = "Run MCMCPollCustomBasicAdv against temporal-logic/primula_out_basic_adv; pass -PappArgs='variantId relNames_comma_sep numSamples' " +
            "for arity-1 (sub) relations MCMCPollBasicAdv doesn't know about, e.g. -PappArgs=\"5 always_C36_s14_15 800\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollCustomBasicAdv")
    val rawArgs = project.findProperty("appArgs")?.toString()
    if (!rawArgs.isNullOrBlank()) args(rawArgs.split(" "))
}

tasks.register<JavaExec>("runDebugMCMC") {
    group = "application"
    description = "Run MCMCPollDebug (hardcoded VARIANT_ID/REL_NAMES/NUM_SAMPLES, edit the file directly) " +
            "against temporal-logic/primula_out_basic."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.LTL.MCMCPollDebug")
}

tasks.register<JavaExec>("runToyGnn") {
    group = "application"
    description = "Run Experiments.Misc.ToyGnn, the small GNN example in Examples/GNN-toy (needs PYTHON_PRIMULA)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("Experiments.Misc.ToyGnn")
    doFirst {
        // Jep's native library lives in the Python env: look it up only when the task runs
        val py = System.getenv("PYTHON_PRIMULA") ?: throw GradleException("set PYTHON_PRIMULA to the Python env")
        val jep = fileTree("$py/lib") { include("python*/site-packages/jep/libjep.*") }.files.first()
        jvmArgs("-Djava.library.path=${jep.parent}")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true          // show System.out / System.err of the tests
        events("passed", "skipped", "failed")
    }
}
