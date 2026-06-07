package dev.stoneworks.common.util

import io.github.oshai.kotlinlogging.KotlinLogging

fun logger(func: () -> Unit) = KotlinLogging.logger(func)
fun logger(obj: Any) = KotlinLogging.logger(toCleanClassName(obj::class.qualifiedName!!))

private fun toCleanClassName(clsName: String): String {
    return if (clsName.endsWith("$")) {
        if (clsName.endsWith("Kt$")) {
            clsName.substring(0, clsName.length - 3)
        } else {
            clsName.substring(0, clsName.length - 1)
        }
    } else {
        clsName
    }
}
