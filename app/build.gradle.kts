import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.7.2"
val k9rapidVersion = "1.f20388f"
val dusseldorfVersion = "2.1.6.0-d31132e"
val ktorVersion = "1.6.0"
val jsonassertVersion = "1.5.0"
val mockkVersion = "1.11.0"
val assertjVersion = "3.19.0"

val mainClass = "no.nav.punsjbolle.ApplicationKt"

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
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
    maven {
        url = uri("https://jitpack.io")
    }
    mavenCentral()
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "15"
    }

    named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "15"
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
        gradleVersion = "7.0.2"
    }
}
