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
    else -> error("No NeoForge version configured for Minecraft $mcVersion")
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
    implementation(libs.ae2)
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
        "version" to project.version as String
    )
    inputs.properties(replaceProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
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
        expand(mapOf("version" to releaseVersion))
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
            programArguments.addAll(
                "--mod", "ae2objects",
                "--all",
                "--output", rootProject.file("src/generated/resources/").absolutePath,
                "--existing", rootProject.file("src/main/resources/").absolutePath
            )
        }
    }
}
