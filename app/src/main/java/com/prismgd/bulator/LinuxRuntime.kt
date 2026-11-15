package com.prismgd.bulator

import android.content.Context
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Installs an official Ubuntu Base ARM64 rootfs into a persistent container.
 * PRoot-RS is packaged as an Android ARM64 executable by the CI workflow.
 */
class LinuxRuntime(private val context: Context) {
    private val storage = ContainerStorage(context)
    private val rootfsUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz"
    private val sumsUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/SHA256SUMS"

    fun prootExecutable(): File {
        val executable = File(context.applicationInfo.nativeLibraryDir, "libproot-rs.so")
        check(executable.isFile && executable.canExecute()) {
            "PRoot-RS ARM64 não foi incluído neste APK."
        }
        return executable
    }

    fun isInstalled(containerId: String): Boolean =
        File(storage.prepare(containerId).rootfs, "bin/sh").exists()

    fun installUbuntuBase(containerId: String, progress: (String) -> Unit = {}) {
        val dirs = storage.prepare(containerId)
        val marker = File(dirs.base, ".ubuntu-base-installed")
        if (marker.exists() && File(dirs.rootfs, "bin/sh").exists()) return

        val archive = File(dirs.downloads, "ubuntu-base-arm64.tar.gz.part")
        val finalArchive = File(dirs.downloads, "ubuntu-base-arm64.tar.gz")
        progress("Baixando Ubuntu Base ARM64…")
        URL(sumsUrl).openConnection().apply {
            connectTimeout = 20000
            readTimeout = 30000
        }.getInputStream().bufferedReader().use { reader ->
            val expected = reader.lineSequence().firstOrNull { it.contains("ubuntu-base-24.04.5-base-arm64.tar.gz") }
                ?.trim()?.split(Regex("\\s+"))?.first()
                ?: error("Não foi possível obter o SHA256 oficial do Ubuntu Base.")
            URL(rootfsUrl).openConnection().apply {
                connectTimeout = 20000
                readTimeout = 60000
            }.getInputStream().use { input ->
                FileOutputStream(archive).use { output -> input.copyTo(output) }
            }
            progress("Verificando integridade do download…")
            val actual = sha256(archive)
            check(actual.equals(expected, ignoreCase = true)) {
                "A verificação SHA256 falhou. O arquivo baixado foi descartado."
            }
        }
        if (!archive.renameTo(finalArchive)) {
            archive.copyTo(finalArchive, overwrite = true)
            archive.delete()
        }

        progress("Extraindo o sistema de arquivos…")
        val staging = File(dirs.base, "rootfs.new")
        if (staging.exists()) staging.deleteRecursively()
        check(staging.mkdirs()) { "Não foi possível criar o diretório do sistema." }
        try {
            extractTarGz(finalArchive, staging)
            check(File(staging, "bin/sh").exists()) { "O rootfs não contém /bin/sh." }
            if (dirs.rootfs.exists()) check(dirs.rootfs.deleteRecursively()) { "Não foi possível substituir o rootfs." }
            check(staging.renameTo(dirs.rootfs)) { "Não foi possível ativar o rootfs instalado." }
            marker.writeText("Ubuntu Base 24.04.5 ARM64")
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
        progress("Ubuntu Base instalado. O sistema está pronto para iniciar após o teste do PRoot.")
    }

    fun execute(containerId: String, command: String): Process {
        val dirs = storage.prepare(containerId)
        check(File(dirs.rootfs, "bin/sh").exists()) { "Instale o Ubuntu Base primeiro." }
        val proot = prootExecutable()
        return ProcessBuilder(
            proot.absolutePath,
            "--rootfs", dirs.rootfs.absolutePath,
            "/bin/sh", "-lc", command
        ).directory(dirs.rootfs).redirectErrorStream(true).start()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractTarGz(archive: File, destination: File) {
        TarArchiveInputStream(BufferedInputStream(GZIPInputStream(archive.inputStream()))).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                val output = File(destination, entry.name)
                val canonicalRoot = destination.canonicalFile
                val canonicalOutput = output.canonicalFile
                require(canonicalOutput.path == canonicalRoot.path ||
                    canonicalOutput.path.startsWith(canonicalRoot.path + File.separator)) {
                    "Caminho inválido no arquivo Ubuntu."
                }
                when {
                    entry.isDirectory -> check(output.mkdirs() || output.isDirectory)
                    entry.isSymbolicLink -> {
                        output.parentFile?.mkdirs()
                        if (output.exists() || output.isSymbolicLink) output.delete()
                        Os.symlink(entry.linkName, output.absolutePath)
                    }
                    entry.isFile -> {
                        output.parentFile?.mkdirs()
                        FileOutputStream(output).use { tar.copyTo(it) }
                        output.setExecutable((entry.mode and 0b001001001) != 0, false)
                        output.setReadable((entry.mode and 0b100100100) != 0, false)
                        output.setWritable((entry.mode and 0b010010010) != 0, false)
                    }
                }
            }
        }
    }

    private val File.isSymbolicLink: Boolean
        get() = try { Os.readlink(absolutePath); true } catch (_: Exception) { false }
}
