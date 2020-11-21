package org.succlz123.ramdisk.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.io.File
import java.math.BigDecimal
import java.util.regex.Pattern

class RAMDiskPlugin : Plugin<Project> {

    companion object {
        const val TAG = "RAM-Disk-Plugin"

        const val NAME = "RAMDiskForGradle"

        const val SYS_LINUX = "Linux"
        const val SYS_WINDOWS = "Windows"
        const val SYS_MAC = "Mac"

        const val FORMAT_APFS = "apfs"
        const val FORMAT_HFS = "hfs"
    }

    override fun apply(project: Project) {
        val osName = System.getProperty("os.name")
        val os = when {
            Pattern.matches("Linux.*", osName) -> {
                SYS_LINUX
            }
            Pattern.matches("Windows.*", osName) -> {
                SYS_WINDOWS
            }
            Pattern.matches("Mac.*", osName) -> {
                SYS_MAC
            }
            else -> {
                null
            }
        }
        val parameter = project.rootProject.properties
        val enable = parameter["RAMDisk.enable"]
        if (enable != "true") {
            project.logger.log(LogLevel.INFO, "$TAG RAMDisk plugin is disable")
            return
        }
        val name = parameter["RAMDisk.name"] as? String ?: NAME
        val size = parameter["RAMDisk.size"] as? String ?: kotlin.run {
            project.logger.log(LogLevel.ERROR, "$TAG -> Please input the RAMDisk Size!!!")
            return
        }
        if (!isNumeric(size)) {
            project.logger.log(LogLevel.ERROR, "$TAG -> Please input the correct RamDisk Size!!!")
            return
        }
        when (os) {
            SYS_LINUX -> {
                val dirStr = "/var/$name"
                val ramFile = File(dirStr)
                if (!ramFile.exists()) {
                    val isSuccess = createLinuxRAMDisk(project, name, size)
                    if (!isSuccess) {
                        return
                    }
                }
                useRAMDisk(project, ramFile, name, size, "")
            }
            SYS_WINDOWS -> {
                val dirStr = "/Volumes/$name"
                val ramFile = File(dirStr)
                if (!ramFile.exists()) {
                    val isSuccess = createWindowsRAMDisk(project, name, size)
                    if (!isSuccess) {
                        return
                    }
                }
                useRAMDisk(project, ramFile, name, size, "")
            }
            SYS_MAC -> {
                val format = parameter["RAMDisk.mac.format"] as? String ?: FORMAT_APFS
                val dirStr = "/Volumes/$name"
                val ramFile = File(dirStr)
                if (!ramFile.exists()) {
                    val isSuccess = createMacRamDisk(project, name, size, format)
                    if (!isSuccess) {
                        return
                    }
                }
                useRAMDisk(project, ramFile, name, size, format)
            }
        }
    }

    private fun createLinuxRAMDisk(project: Project, name: String, size: String): Boolean {
        val mkdirCmd = "mkdir /var/${name}"
        var result = exec(project, mkdirCmd)
        if (!result) {
            return false
        }
        val mountCmd = "mount -t tmpfs none /var/${name} -o size=${size}m"
        result = exec(project, mountCmd)
        return result
    }

    private fun createWindowsRAMDisk(project: Project, name: String, size: String): Boolean {
        val checkImDiskCmd = ""
        var result = exec(project, checkImDiskCmd)
        if (!result) {
            return false
        }
        val execImDiskCmd = ""
        result = exec(project, execImDiskCmd)
        return result
    }

    private fun createMacRamDisk(
        project: Project,
        name: String,
        size: String,
        format: String
    ): Boolean {
        return when (format) {
            FORMAT_APFS -> {
                val cmd =
                    "diskutil partitionDisk \$(hdiutil attach -nomount ram://\$((2048*${size}))) 1 GPTFormat APFS '${name}' '100%'"
                exec(project, cmd)
            }
            FORMAT_HFS -> {
                val cmd =
                    "diskutil erasevolume HFS+ ${name} \$(hdiutil attach -nomount ram://\$((2048*${size})))"
                exec(project, cmd)
            }
            else -> {
                false
            }
        }
    }

    private fun useRAMDisk(
        project: Project,
        ramDirFile: File,
        name: String,
        size: String,
        format: String?
    ) {
        if (ramDirFile.exists()) {
            val projectParent = project.rootProject
            for (curProject in projectParent.allprojects) {
                curProject.buildDir =
                    File("${ramDirFile.absolutePath}/${projectParent.name}/${curProject.name}")
            }
            val fileSizeMB: Double = BigDecimal(ramDirFile.totalSpace / 1024 / 1024).setScale(
                2, BigDecimal.ROUND_HALF_UP
            ).toDouble()
            project.logger.log(
                LogLevel.QUIET,
                "$TAG -> RAMDisk is enable: $name, ExpectationSize: $size MB, ActualSize: $fileSizeMB MB, format: $format"
            )
        }
    }

    private fun isNumeric(input: String): Boolean =
        try {
            input.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }

    private fun exec(project: Project, bashCommand: String): Boolean {
        try {
            val runtime = Runtime.getRuntime()
            val cmd = arrayOf("sh", "-c", bashCommand)
            val process = runtime.exec(cmd)
            val exitCode: Int = process.waitFor()
            if (exitCode != 0) {
                project.logger.log(
                    LogLevel.ERROR, "$TAG -> exitCode\nFailed to call shell's command\n$cmd"
                )
                return false
            }
            val strbr = StringBuffer()
            process.inputStream.reader(Charsets.UTF_8).use {
                for (readLine in it.readLines()) {
                    strbr.append(readLine).append("\n")
                }
            }
            project.logger.log(
                LogLevel.QUIET, "$TAG -> Create RamDisk successful\nConsole Info:\n\n$strbr"
            )
            return true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }
}
