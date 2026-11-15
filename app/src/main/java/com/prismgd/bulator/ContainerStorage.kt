package com.prismgd.bulator

import android.content.Context
import java.io.File

/**
 * Persistent on-device layout for independent Linux containers.
 * This class prepares storage only; it does not claim that PRoot or Lubuntu
 * is installed. Runtime installation is the next integration step.
 */
class ContainerStorage(context: Context) {
    private val root = File(context.filesDir, "containers")

    fun directory(containerId: String): File {
        require(containerId.matches(Regex("[a-zA-Z0-9_-]{1,64}"))) {
            "Invalid container identifier"
        }
        return File(root, containerId)
    }

    fun prepare(containerId: String): ContainerDirectories {
        val base = directory(containerId)
        val rootfs = File(base, "rootfs")
        val downloads = File(base, "downloads")
        val cache = File(base, "cache")
        val config = File(base, "config")
        listOf(rootfs, downloads, cache, config).forEach { dir ->
            if (!dir.exists() && !dir.mkdirs()) {
                throw IllegalStateException("Could not create container directory: ${dir.name}")
            }
        }
        return ContainerDirectories(base, rootfs, downloads, cache, config)
    }

    fun sizeBytes(containerId: String): Long {
        val base = directory(containerId)
        if (!base.exists()) return 0L
        return base.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}

data class ContainerDirectories(
    val base: File,
    val rootfs: File,
    val downloads: File,
    val cache: File,
    val config: File
)
