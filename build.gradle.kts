plugins {
    base
    `maven-publish`
    alias(libs.plugins.moddev)
}

group = "top.likoslupus"
version = providers.environmentVariable("AE2OBJECTS_VERSION").getOrElse("0.0.0-dev")
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
