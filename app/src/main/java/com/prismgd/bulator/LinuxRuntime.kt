package com.prismgd.bulator

import android.content.Context
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Installs the official Ubuntu Base ARM64 rootfs into a persistent container.
 * PRoot-RS is packaged as an Android ARM64 executable by the CI workflow.
 */
class LinuxRuntime(private val context: Context) {
    private val storage = ContainerStorage(context)
    private val releaseBase = "https://cdimages.ubuntu.com/cdimage/ubuntu-base/releases/noble/release/"
    private val archiveName = "ubuntu-base-24.04.5-base-arm64.tar.gz"
    private val rootfsUrl = releaseBase + archiveName
    private val sumsUrl = releaseBase + "SHA256SUMS"

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

        val archive = File(dirs.downloads, "$archiveName.part")
        val finalArchive = File(dirs.downloads, archiveName)

        progress("Conectando aos servidores oficiais do Ubuntu…")
        val expected = downloadText(sumsUrl)
            .lineSequence()
            .firstOrNull { it.trim().endsWith(archiveName) }
            ?.trim()?.split(Regex("\\s+"))?.first()
            ?: error("O arquivo SHA256SUMS não contém a soma do Ubuntu Base ARM64.")

        progress("Baixando Ubuntu Base ARM64 (aproximadamente 29 MB)…")
        try {
            downloadFile(rootfsUrl, archive) { received, total ->
                val percent = if (total > 0L) (received * 100L / total).toInt() else -1
                val receivedMb = received / (1024 * 1024)
                val totalMb = if (total > 0L) total / (1024 * 1024) else 0L
                progress(if (percent >= 0) {
                    "Baixando Ubuntu Base: $percent% ($receivedMb/$totalMb MB)…"
                } else {
                    "Baixando Ubuntu Base: $receivedMb MB recebidos…"
                })
            }

            progress("Verificando integridade do download…")
            val actual = sha256(archive)
            check(actual.equals(expected, ignoreCase = true)) {
                "O SHA256 do download não confere. Baixe novamente para evitar um sistema corrompido."
            }

            if (finalArchive.exists()) check(finalArchive.delete()) {
                "Não foi possível substituir o download anterior."
            }
            check(archive.renameTo(finalArchive)) {
                "Download concluído, mas não foi possível salvar o arquivo."
            }
        } catch (error: Exception) {
            archive.delete()
            throw error
        }

        progress("Extraindo o sistema de arquivos…")
        val staging = File(dirs.base, "rootfs.new")
        if (staging.exists()) staging.deleteRecursively()
        check(staging.mkdirs()) { "Não foi possível criar o diretório do sistema." }
        try {
            extractTarGz(finalArchive, staging)
            check(File(staging, "bin/sh").exists()) { "O rootfs baixado não contém /bin/sh." }
            if (dirs.rootfs.exists()) check(dirs.rootfs.deleteRecursively()) {
                "Não foi possível substituir o rootfs."
            }
            check(staging.renameTo(dirs.rootfs)) {
                "Não foi possível ativar o rootfs instalado."
            }
            marker.writeText("Ubuntu Base 24.04.5 ARM64")
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
        progress("Ubuntu Base instalado. O sistema está pronto para iniciar após o teste do PRoot.")
    }

    private fun downloadText(address: String): String {
        val connection = openConnection(address)
        return try {
            val code = connection.responseCode
            check(code in 200..299) {
                "Servidor do Ubuntu respondeu HTTP $code ao consultar a lista de verificações."
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadFile(address: String, destination: File, onProgress: (Long, Long) -> Unit) {
        var lastError: Exception? = null
        for (attempt in 1..3) {
            try {
                val connection = openConnection(address)
                try {
                    val code = connection.responseCode
                    check(code in 200..299) {
                        "Servidor do Ubuntu respondeu HTTP $code ao baixar o sistema."
                    }
                    val total = connection.contentLengthLong
                    if (total > 0L) {
                        check(destination.parentFile?.usableSpace?.let { it > total + 20L * 1024 * 1024 } != false) {
                            "Não há espaço livre suficiente para baixar o Ubuntu Base."
                        }
                    }
                    connection.inputStream.use { input ->
                        FileOutputStream(destination, false).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var received = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                received += count
                                onProgress(received, total)
                            }
                            output.fd.sync()
                            check(total <= 0L || received == total) {
                                "Download incompleto: recebidos $received de $total bytes."
                            }
                        }
                    }
                    return
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                destination.delete()
                lastError = error
                if (attempt < 3) onProgress(0L, 0L)
            }
        }
        throw IllegalStateException(
            "Não foi possível baixar o Ubuntu após 3 tentativas: ${lastError?.message ?: "erro de rede desconhecido"}",
            lastError
        )
    }

    private fun openConnection(address: String): HttpURLConnection {
        return (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Bulator-Android/0.3")
            setRequestProperty("Accept", "*/*")
            connect()
        }
    }

    fun execute(containerId: String, command: String): Process {
        val dirs = storage.prepare(containerId)
        check(File(dirs.rootfs, "bin/sh").exists()) { "Instale o Ubuntu Base primeiro." }
        val proot = prootExecutable()
        return ProcessBuilder(
            proot.absolutePath,
            "-R", dirs.rootfs.absolutePath,
            "/bin/sh", "-lc", command
        ).directory(dirs.rootfs).redirectErrorStream(true).start()
    }

    /**
     * Starts a persistent interactive shell. Android pipes provide command
     * input/output; a real PTY is a later step for full-screen terminal apps.
     */
    fun startShell(containerId: String): Process {
        val dirs = storage.prepare(containerId)
        check(File(dirs.rootfs, "bin/sh").exists()) { "Instale o Ubuntu Base primeiro." }
        val proot = prootExecutable()
        return ProcessBuilder(
            proot.absolutePath,
            "-R", dirs.rootfs.absolutePath,
            "/bin/sh", "-i"
        ).directory(dirs.rootfs)
            .redirectErrorStream(true)
            .apply {
                environment()["HOME"] = "/root"
                environment()["USER"] = "root"
                environment()["LOGNAME"] = "root"
                environment()["TERM"] = "dumb"
                environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                environment()["PS1"] = "root@bulator:~# "
            }
            .start()
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
