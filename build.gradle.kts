plugins {
    kotlin("jvm") version "1.7.21"
    id("kr.entree.spigradle") version "2.4.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "pe.chalk.bukkit"
version = "1.0.0"

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc", "spigot-api", "1.19.4-R0.1-SNAPSHOT")
    implementation("org.bstats", "bstats-bukkit", "3.0.2")
}

spigot {
    apiVersion = "1.19"
    description = "BookDelivery"
    main = "pe.chalk.bukkit.bookdelivery.BookDelivery"
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.bstats", "pe.chalk.bukkit.bstats")
}