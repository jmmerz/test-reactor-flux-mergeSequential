plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Last working version
    //implementation("io.projectreactor:reactor-core:3.4.37")
    //testImplementation("io.projectreactor:reactor-test:3.4.37")

    // First non-working version:
    //implementation("io.projectreactor:reactor-core:3.5.0")
    //testImplementation("io.projectreactor:reactor-test:3.5.0")

    // Latest version (as of 2024-04-29) does not work
    implementation("io.projectreactor:reactor-core:3.6.5")
    testImplementation("io.projectreactor:reactor-test:3.6.5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
