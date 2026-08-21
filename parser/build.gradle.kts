plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

// Bu modulun BAGIMLILIGI YOKTUR (test disinda).
// Sonuc: Android SDK olmadan, hicbir kutuphane indirmeden derlenip
// calistirilabilir - riskin tamaminin bulundugu kod hizlica test edilir.
dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "skipped", "failed") }
}

// Tek kaynak: kok dizindeki patterns/patterns.json hem testlere hem app assets'e gider.
tasks.named<ProcessResources>("processTestResources") {
    from(rootProject.file("patterns")) { include("patterns.json") }
}

// --- Bagimsiz dogrulama kosucusu ---
// `gradle :parser:verify` ile calisir. JUnit'e ihtiyac duymaz; ayristirma
// dogrulugunu ve uctan uca akisi (ayristir -> tekrar koru -> kategorile ->
// topla) tek komutta raporlar.
sourceSets {
    create("verify") {
        kotlin.srcDir("src/verify/kotlin")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

tasks.register<JavaExec>("verify") {
    group = "verification"
    description = "Ayristirici dogrulugunu ve uctan uca akisi dogrular"
    mainClass.set("com.bildirimbutce.parser.verify.Verify")
    classpath = sourceSets["verify"].runtimeClasspath
    args(
        rootProject.file("patterns/patterns.json").absolutePath,
        file("src/test/resources/fixtures.tsv").absolutePath
    )
}
