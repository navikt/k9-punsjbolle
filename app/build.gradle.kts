import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.8.2"
val k9rapidVersion = "1.20220708075341-87c2b3c"
val dusseldorfVersion = "3.1.6.8-f3930ac"
val ktorVersion = "1.6.8"
val jsonassertVersion = "1.5.1"
val mockkVersion = "1.12.4"
val assertjVersion = "3.23.1"

val mainClass = "no.nav.punsjbolle.ApplicationKt"

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfVersion")

    // Test
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.assertj:assertj-core:$assertjVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://jitpack.io")
    mavenCentral()
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClass
                )
            )
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.4.2"
    }
}
