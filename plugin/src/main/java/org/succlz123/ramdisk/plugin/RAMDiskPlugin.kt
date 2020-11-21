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

        const val FORMAT_EXT4 = "ext4"
        const val FORMAT_NTFS = "ntfs"
        const val FORMAT_FAT32 = "fat32"
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
                val format = parameter["RAMDisk.linux.format"] ?: FORMAT_EXT4
                project.logger.log(
                    LogLevel.QUIET,
                    "$TAG -> RAMDisk name: $name, diskSize: $size MB, format: $format"
                )
            }
            SYS_WINDOWS -> {
                val format = parameter["RAMDisk.window.format"] ?: FORMAT_NTFS
                project.logger.log(
                    LogLevel.QUIET,
                    "$TAG -> RAMDisk name: $name, diskSize: $size MB, format: $format"
                )
            }
            SYS_MAC -> {
                val format = parameter["RAMDisk.mac.format"] as? String ?: FORMAT_APFS
                val dirStr = "/Volumes/$name"
                val filePath = File(dirStr)
                if (!filePath.exists()) {
                    val isSuccess = createMacRamDisk(project, name, size, format)
                    if (!isSuccess) {
                        return
                    }
                }
                if (filePath.exists()) {
                    val projectParent = project.rootProject
                    for (curProject in projectParent.allprojects) {
                        curProject.buildDir =
                            File("$dirStr/${projectParent.name}/${curProject.name}")
                    }
                    val fileSizeMB: Double = BigDecimal(filePath.totalSpace / 1024 / 1024).setScale(
                        2, BigDecimal.ROUND_HALF_UP
                    ).toDouble()
                    project.logger.log(
                        LogLevel.QUIET,
                        "$TAG -> RAMDisk is enable: $name, ExpectationSize: $size MB, ActualSize: $fileSizeMB MB, format: $format"
                    )
                }
            }
        }
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
