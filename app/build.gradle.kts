import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.7.2"
val k9rapidVersion = "1.592c7c1"
val dusseldorfVersion = "1.5.4.f9a29f3"
val ktorVersion = "1.5.4"
val jsonassertVersion = "1.5.0"
val mockkVersion = "1.11.0"
val assertjVersion = "3.19.0"

val mainClass = "no.nav.punsjbolle.ApplicationKt"

plugins {
    kotlin("jvm") version "1.5.0"
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
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfVersion") {
        exclude(group = "com.github.kittinunf.fuel")
    }
    implementation("no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfVersion")
    implementation("org.skyscreamer:jsonassert:$jsonassertVersion")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfVersion") {
        exclude(group = "com.github.jknack")
    }
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
