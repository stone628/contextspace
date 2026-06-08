package dev.stoneworks.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue

abstract class CollectPreloadClassesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val withImport: ListProperty<String>

    @get:Input
    abstract val preloadClassName: Property<String>

    @TaskAction
    fun exec() {
        val preloadClasses = ConcurrentLinkedQueue<String>()
        val ktFiles = sourceFiles.asFileTree.matching { include("**/*.kt") }
        val withFullImport = withImport.get().map { "import $it" }.toHashSet()

        ktFiles.toList().parallelStream().forEach { file ->
            var hasComponentImport = false
            var packageName: String? = null

            file.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()

                    if (trimmed.isBlank()) continue

                    if (trimmed.startsWith("package ") || trimmed.startsWith("@")) {
                        if (trimmed.startsWith("package ")) {
                            packageName = trimmed.substringAfter("package ").trim()
                        }
                        continue
                    }

                    if (trimmed.startsWith("import ")) {
                        if (withFullImport.contains(trimmed)) {
                            hasComponentImport = true
                            break
                        }
                    } else {
                        break
                    }
                }
            }

            if (hasComponentImport) {
                val className = file.nameWithoutExtension
                val fullyQualifiedName = if (packageName.isNullOrBlank()) className else "$packageName.$className"
                preloadClasses.add(fullyQualifiedName)
            }
        }

        val sorted = preloadClasses.sorted()
        val preloadClassName = preloadClassName.get()
        val outputFile = outputDir.file(preloadClassName.replace(".", "/") + ".kt").get().asFile
        outputFile.parentFile.mkdirs()

        val content = buildString {
            appendLine("@file:Suppress(\"UNUSED_EXPRESSION\")")

            val separator = preloadClassName.lastIndexOf('.')
            if (separator > 0) {
                appendLine("package ${preloadClassName.substring(0, separator)}")
                appendLine()
            }

            appendLine("fun preloadClasses() {")
            sorted.forEach { appendLine("    $it") }
            appendLine("}")
        }

        outputFile.writeText(content, StandardCharsets.UTF_8)
        logger.lifecycle("Generated preload classes with ${sorted.size} classes at: ${outputFile.absolutePath}")
    }
}
