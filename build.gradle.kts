plugins {
    base
    `maven-publish`
    alias(libs.plugins.moddev)
}

val modVersion = libs.versions.mod.get()
val mcVersion = libs.versions.minecraft.get()
val releaseVersion = "$modVersion+$mcVersion"

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

val commitHash = getGitCommitHash()
val defaultVersion = "$releaseVersion-$commitHash"

group = "top.likoslupus"
version = defaultVersion
base.archivesName.set("ae2objects")

repositories {
    mavenCentral()
}

configurations {
    testImplementation {
        extendsFrom(compileClasspath.get())
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
            srcDir("src/generated/resources")
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
    }
    from("src/main/resources/META-INF/neoforge.mods.toml") {
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

neoForge {
    version = libs.versions.neoforge.get()

    mods {
        create("ae2objects") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", "ae2objects")
        }

        create("server") {
            server()
            systemProperty("forge.enabledGameTestNamespaces", "ae2objects")
            programArgument("--nogui")
        }

        create("serverData") {
            serverData()
            programArguments.addAll(
                "--mod", "ae2objects",
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
    }
}
