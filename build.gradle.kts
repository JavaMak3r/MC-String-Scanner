plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(arrayOf("-Xlint:unchecked", "-Xlint:deprecation"))
        options.isIncremental = true
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from("src/main/resources")
        filteringCharset = "UTF-8"
    }

    shadowJar {
        manifest {
            attributes(
                "Main-Class" to "org.example.MinecraftMemoryScanner",
                "Implementation-Version" to project.version
            )
        }
        archiveBaseName.set("strings")
        archiveClassifier.set("")
    }
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources")
        }
    }
}