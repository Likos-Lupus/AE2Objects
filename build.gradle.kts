plugins {
    base
    `maven-publish`
    alias(libs.plugins.moddev)
}

val defaultVersion = libs.versions.mod.get()
val mcVersion = sc.current.version
val commitHash = getGitCommitHash()

// Default (dirty) build: <default_version>-<commit>+<minecraft_version>
// Release build (releaseJar): <default_version>+<minecraft_version>
version = "$defaultVersion-$commitHash+$mcVersion"
val releaseVersion = "$defaultVersion+$mcVersion"

val neoForgeVersion = when (mcVersion) {
    "26.1.2" -> libs.versions.neoforge.get()
    "26.2" -> libs.versions.neoforge262.get()
    "26.3" -> libs.versions.neoforge263.get()
    else -> error("No NeoForge version configured for Minecraft $mcVersion")
}

val ae2Version = when (mcVersion) {
    "26.1.2" -> libs.versions.ae2.get()
    "26.2" -> libs.versions.ae2262.get()
    "26.3" -> libs.versions.ae2263.get()
    else -> error("No AE2 version configured for Minecraft $mcVersion")
}

// 26.2 replaced the creative-menu clone path with Slot.safeClone(Player), so the old
// AbstractContainerMenu mixin only applies to 26.1.2.
val mixinConfig = if (mcVersion == "26.1.2") {
    "ae2objects.mixins.json"
} else {
    "ae2objects-safeclone.mixins.json"
}

// 26.2 replaced the square mod icon field `logoFile` with `iconFile`.
val modIconKey = if (mcVersion == "26.1.2") "logoFile" else "iconFile"

val neoForgeRange = when (mcVersion) {
    "26.1.2" -> libs.versions.neoforgerange2612.get()
    "26.2" -> libs.versions.neoforgerange262.get()
    "26.3" -> libs.versions.neoforgerange263.get()
    else -> error("No NeoForge dependency range configured for Minecraft $mcVersion")
}

val minecraftRange = when (mcVersion) {
    "26.1.2" -> libs.versions.minecraftrange2612.get()
    "26.2" -> libs.versions.minecraftrange262.get()
    "26.3" -> libs.versions.minecraftrange263.get()
    else -> error("No Minecraft dependency range configured for Minecraft $mcVersion")
}

val ae2Range = when (mcVersion) {
    "26.1.2" -> libs.versions.ae2range2612.get()
    "26.2" -> libs.versions.ae2range262.get()
    "26.3" -> libs.versions.ae2range263.get()
    else -> error("No AE2 dependency range configured for Minecraft $mcVersion")
}

fun getGitCommitHash(): String {
    return try {
        val result = providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
            isIgnoreExitValue = true
        }
        val output = result.standardOutput.asText.get().trim()
        if (output.matches(Regex("^[0-9a-fA-F]{7}$"))) output else "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}

group = "top.likoslupus"
base.archivesName.set("ae2objects")

repositories {
    mavenCentral()
}

// Dedicated source set for NeoForge GameTests. It is added to the dev mod (so the harness can
// discover and run it) but is excluded from the release jar (the jar task only takes main output).
val gametest by sourceSets.creating
gametest.compileClasspath += sourceSets.main.get().output
gametest.runtimeClasspath += sourceSets.main.get().output

configurations {
    testImplementation {
        extendsFrom(compileClasspath.get())
    }
    named(gametest.implementationConfigurationName) {
        extendsFrom(configurations.getByName("implementation"))
    }
    named(gametest.compileOnlyConfigurationName) {
        extendsFrom(configurations.getByName("compileOnly"))
    }
}

dependencies {
    implementation("org.appliedenergistics:appliedenergistics2:$ae2Version")
    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    main {
        resources {
            srcDir(rootProject.file("src/generated/resources"))
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties = mapOf(
        "version" to project.version as String,
        "neoforgeRange" to neoForgeRange,
        "minecraftRange" to minecraftRange,
        "ae2Range" to ae2Range,
        "mixinConfig" to mixinConfig,
        "modIconKey" to modIconKey
    )
    inputs.properties(replaceProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }

    // Ship only the mixin config that matches this Minecraft version.
    if (mcVersion == "26.1.2") {
        exclude("ae2objects-safeclone.mixins.json")
    } else {
        exclude("ae2objects.mixins.json")
    }

    if (mcVersion == "26.3") {
        // 26.3 renamed the recipe-unlocked advancement field. Keep the generated data canonical
        // for 26.1/26.2 and rewrite it only in the 26.3 resources.
        filesMatching("data/ae2objects/advancement/**/*.json") {
            filter { line -> line.replace("\"recipe\":", "\"recipes\":") }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    // GameTest plot structures are only needed by the (non-shipped) gametest source set.
    exclude("data/ae2objects/structure/**")
    manifest {
        attributes(
            "Implementation-Version" to project.version
        )
    }
}

tasks.named<Jar>("sourcesJar") {
    // The runtime metadata is expanded for the concrete Minecraft version and is not source.
    exclude("META-INF/neoforge.mods.toml")
    if (mcVersion == "26.1.2") {
        exclude("ae2objects-safeclone.mixins.json")
    } else {
        exclude("ae2objects.mixins.json")
    }
}

tasks.register<Jar>("releaseJar") {
    group = "build"
    description = "Assembles a release jar archive without commit hash."
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output) {
        exclude("META-INF/neoforge.mods.toml")
        exclude("data/ae2objects/structure/**")
    }
    from(rootProject.file("src/main/resources/META-INF/neoforge.mods.toml")) {
        into("META-INF")
        expand(
            mapOf(
                "version" to releaseVersion,
                "neoforgeRange" to neoForgeRange,
                "minecraftRange" to minecraftRange,
                "ae2Range" to ae2Range,
                "mixinConfig" to mixinConfig,
                "modIconKey" to modIconKey
            )
        )
    }
    archiveBaseName.set("ae2objects")
    archiveVersion.set(releaseVersion)
    archiveClassifier.set("")
    manifest {
        attributes(
            "Implementation-Version" to releaseVersion
        )
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Copies the built mod artifacts into the root build/libs directory."
    dependsOn(tasks.named("build"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    from(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
}

neoForge {
    version = neoForgeVersion

    addModdingDependenciesTo(gametest)

    mods {
        create("ae2objects") {
            sourceSet(sourceSets.main.get())
            sourceSet(gametest)
        }
    }

    runs {
        create("client") {
            client()
            gameDirectory.set(rootProject.layout.projectDirectory.dir("run"))
            ideFolderName.set(mcVersion)
        }

        create("server") {
            server()
            gameDirectory.set(rootProject.layout.projectDirectory.dir("run"))
            ideFolderName.set(mcVersion)
            programArgument("--nogui")
        }

        create("gameTestServer") {
            type = "gameTestServer"
            gameDirectory.set(rootProject.layout.projectDirectory.dir("run"))
            ideFolderName.set(mcVersion)
        }

        create("serverData") {
            serverData()
            gameDirectory.set(rootProject.layout.projectDirectory.dir("run"))
            ideFolderName.set(mcVersion)
            val dataOutput = if (mcVersion == "26.3") {
                rootProject.layout.buildDirectory.dir("generated-data/26.3").get().asFile
            } else {
                rootProject.file("src/generated/resources")
            }
            programArguments.addAll(
                "--mod", "ae2objects",
                "--all",
                "--output", dataOutput.absolutePath,
                "--existing", rootProject.file("src/main/resources/").absolutePath
            )
        }
    }
}
