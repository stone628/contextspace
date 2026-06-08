plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
}

application {
    mainClass.set("dev.stoneworks.contextspace.ApplicationKt")
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:3.5.0"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-cors")

    implementation("org.jetbrains.exposed:exposed-core:1.3.0")
    implementation("org.jetbrains.exposed:exposed-dao:1.3.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.jetbrains.exposed:exposed-json:1.3.0")
    implementation("org.jetbrains.exposed:exposed-java-time:1.3.0")

    implementation("org.postgresql:postgresql:42.7.11")
    implementation("com.zaxxer:HikariCP:7.0.2")

    implementation("io.lettuce:lettuce-core:7.6.0.RELEASE")

    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("ch.qos.logback:logback-classic:1.5.34")
    implementation("io.github.oshai:kotlin-logging:8.0.4")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.exposed:exposed-dao:1.3.0")
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation("org.jetbrains.exposed:exposed-java-time:1.3.0")
    testImplementation("io.ktor:ktor-client-content-negotiation")
}

tasks.test {
    useJUnitPlatform()
}

abstract class CollectPreloadClassesTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val withImport = listOf(
        "dev.stoneworks.common.registerInit",
        "dev.stoneworks.common.registerShutdown",
        "dev.stoneworks.common.registerTable",
        "dev.stoneworks.common.registerRoute",
    )

    private val preloadClassCanonicalName = "dev.stoneworks.common.PreloadClasses"

    @TaskAction
    fun run() {
        val componentClasses = mutableListOf<String>()
        val ktFiles = sourceFiles.asFileTree.matching { include("**/*.kt") }
        val withFullImport = withImport.map { "import $it" }.toHashSet()

        ktFiles.forEach { file ->
            var hasComponentImport = false
            var packageName: String? = null

            file.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()

                    if (trimmed.isBlank()) continue

                    if (trimmed.startsWith("package ")) {
                        packageName = trimmed.substringAfter("package ").trim()
                        continue
                    }

                    if (withFullImport.contains(trimmed)) {
                        hasComponentImport = true
                        break
                    }

                    if (!trimmed.startsWith("import ")) break
                }
            }

            if (hasComponentImport) {
                val className = file.nameWithoutExtension
                val fullyQualifiedName = if (packageName.isNullOrBlank()) className else "$packageName.$className"

                componentClasses.add(fullyQualifiedName)
            }
        }

        val outputFile = outputDir.file(preloadClassCanonicalName.replace(".", "/") + ".kt").get().asFile

        outputFile.parentFile.mkdirs()

        val sb = StringBuilder()
        val separatorPos = preloadClassCanonicalName.lastIndexOf('.')

        sb.appendLine("@file:Suppress(\"UNUSED_EXPRESSION\")")

        if (separatorPos > 0) {
            sb.appendLine("package ${preloadClassCanonicalName.substring(0, separatorPos)}").appendLine()
        }

        sb.appendLine("fun preload() {")

        componentClasses.forEach { sb.appendLine("    $it") }

        sb.appendLine("}")

        outputFile.writeText(sb.toString())
        logger.lifecycle("Generated collected preload classes with ${componentClasses.size} classes at: ${outputFile.absolutePath}")
    }
}

val collectPreloadClasses = tasks.register<CollectPreloadClassesTask>("collectPreloadClasses") {
    sourceFiles.from(project.fileTree("src/main/kotlin"))
    outputDir.set(project.layout.buildDirectory.dir("generated/sources/preload/kotlin/main"))
}

tasks.named("compileKotlin") {
    dependsOn(collectPreloadClasses)
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(collectPreloadClasses.flatMap { it.outputDir })
}
