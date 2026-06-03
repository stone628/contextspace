plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
}

application {
    mainClass.set("dev.stoneworks.contextspace.ApplicationKt")
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:3.1.0"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-config-yaml")

    implementation("org.jetbrains.exposed:exposed-core:0.57.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.57.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.57.0")
    implementation("org.jetbrains.exposed:exposed-json:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")

    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.zaxxer:HikariCP:6.2.1")

    implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")

    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.exposed:exposed-dao:0.57.0")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
    testImplementation("io.ktor:ktor-client-content-negotiation")
}

tasks.test {
    useJUnitPlatform()
}
