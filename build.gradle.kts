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
dependencies {
    testImplementation("org.ow2.asm:asm-tree:9.7.1")
    testImplementation("org.joml:joml:1.10.5")
    compileOnly(files("../_deps_sable_2_0_5/sable-neoforge-1.21.1-2.0.5.jar"))
    compileOnly(files("../_deps_sable/META-INF/jarjar/sable-companion-common-1.21.1-1.6.0.jar"))
}
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
