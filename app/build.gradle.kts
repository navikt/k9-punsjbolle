import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.9.1"
val k9rapidVersion = "1.20221124140732-e07f6f7"
val dusseldorfVersion = "3.2.1.3-bd71b5a"
val ktorVersion = "2.2.2"
val jsonassertVersion = "1.5.1"
val mockkVersion = "1.13.2"
val assertjVersion = "3.23.1"
val k9SakKontraktVersion = "3.3.24"

val mainClass = "no.nav.punsjbolle.ApplicationKt"

plugins {
    kotlin("jvm") version "1.7.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfVersion")

    implementation("no.nav.k9.sak:kontrakt:$k9SakKontraktVersion")

    // Test
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "x-access-token"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

    maven {
        name = "K9SakPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-sak")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "x-access-token"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_READER_TOKEN")
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
        gradleVersion = "7.5.1"
    }
}
