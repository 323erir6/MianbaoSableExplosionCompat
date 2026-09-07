plugins {
    java
    id("net.neoforged.moddev") version "2.0.80"
}
version = property("mod_version")!!
group = "dev.mianbaosablecompat"
base { archivesName.set("mianbao-sable-explosion-compat") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
repositories { mavenCentral() }
neoForge {
    version = property("neo_version") as String
    mods { create("mianbao_sable_explosion_compat") { sourceSet(sourceSets.main.get()) } }
}

val sableJarPath = providers.gradleProperty("sableJar")
    .orElse("../_deps_sable_2_0_5/sable-neoforge-1.21.1-2.0.5.jar")
val sableJar = file(sableJarPath.get())
val sableCompanion = layout.buildDirectory.file("sable-deps/sable-companion-common.jar")
val extractSableCompanion by tasks.registering(Copy::class) {
    from(zipTree(sableJar))
    include("META-INF/jarjar/sable-companion-common-*.jar")
    eachFile { path = "sable-companion-common.jar" }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("sable-deps"))
    outputs.file(sableCompanion)
}

dependencies {
    testImplementation("org.ow2.asm:asm-tree:9.7.1")
    testImplementation("org.joml:joml:1.10.5")
    compileOnly(files(sableJar))
    compileOnly(files(sableCompanion))
}
tasks.compileJava { dependsOn(extractSableCompanion) }
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to project.version) }
}
val projectionTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.mianbaosablecompat.CellProjectionTest")
}
tasks.check { dependsOn(projectionTest) }
val coverageTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.mianbaosablecompat.HookCoverageTest")
    onlyIf { providers.gradleProperty("mianbaoJar").isPresent }
    doFirst { args(providers.gradleProperty("mianbaoJar").get()) }
}
tasks.check { dependsOn(coverageTest) }
val chunkCoverageTest by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.mianbaosablecompat.ChunkCoverageTest")
}
tasks.check { dependsOn(chunkCoverageTest) }
