package io.github.gaming32.yarntomojmap.main

import io.github.gaming32.yarntomojmap.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.annotation.Arg
import net.sourceforge.argparse4j.inf.ArgumentParserException
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

fun main(vararg args: String): Unit = runBlocking {
    val parser = ArgumentParsers.newFor("yarn-to-mojmap")
        .fromFilePrefix("@")
        .build()
        .description("生成 Yarn 到 Mojmap 映射文件。非常适合在源重新映射中使用。")
    parser.addArgument("-m", "--minecraft")
        .help("目标 Minecraft 版本。默认为最新。")
    parser.addArgument("-y", "--yarn")
        .help("源 Yarn 构建。默认为最新。")
        .type(Int::class.java)
    parser.addArgument("-f", "--format")
        .help("导出映射所使用的格式。指定扩展或映射 io ID。默认为 Tiny v2。")
        .type(MappingFormatArgumentType).default = MappingFormat.TINY_2_FILE

    val parsedArgs = object {
        @set:Arg(dest = "minecraft")
        var minecraft: String? = null

        @set:Arg(dest = "yarn")
        var yarn: Int? = null

        @set:Arg(dest = "format")
        lateinit var format: MappingFormat
    }

    try {
        parser.parseArgs(args, parsedArgs)
    } catch (e: ArgumentParserException) {
        parser.handleError(e)
        exitProcess(1)
    }

    try {
        val mappings = createHttpClient().use { http ->
            val (minecraftVersion, clientJsonUrl) = lookupMinecraftVersion(http, parsedArgs.minecraft)
            logger.info { "使用 Minecraft 版本 $minecraftVersion" }

            val mojmap = async {
                val minecraftDownloads = lookupMinecraftFileDownloads(http, clientJsonUrl)
                logger.info { "加载了 ${minecraftDownloads.size} 个下载 URL" }

                val mojmap = downloadMojmap(http, minecraftDownloads)
                logger.info { "从 Mojmap 加载了 ${mojmap.classes.size} 个类" }
                mojmap
            }

            val yarnBuild = parsedArgs.yarn ?: lookupLatestYarn(http, minecraftVersion)
            if (yarnBuild == null) {
                logger.error { "未找到适用于 Minecraft $minecraftVersion 的 Yarn 版本" }
                exitProcess(1)
            }
            val yarnVersion = "$minecraftVersion+build.$yarnBuild"
            logger.info { "使用 Yarn 版本 $yarnVersion" }

            val intermediaryMappings = async {
                val result = downloadIntermediaryMappings(http, minecraftVersion)
                logger.info { "从 Intermediary 加载了 ${result.classes.size} 个类" }
                result
            }

            val yarnMappings = async {
                val result = downloadYarnMappings(http, yarnVersion)
                logger.info { "从 Yarn 加载了 ${result.classes.size} 个类" }
                result
            }

            MappingsTriple(mojmap.await(), intermediaryMappings.await(), yarnMappings.await())
        }

        logger.info {
            "总共加载了 ${mappings.mojmap.classes.size + mappings.intermediary.classes.size + mappings.yarn.classes.size} 个类映射"
        }
        logger.info { "开始构建映射过程" }

        val timing = measureTime {
            val file = File("mappings.txt")
            file.writeText("")
            val streamWriter = file.bufferedWriter()
            val mappingWriter = MappingWriter.create(streamWriter, parsedArgs.format)
                ?: error("不支持的输出格式 ${parsedArgs.format}")
            buildMappings(mappings, mappingWriter)
            streamWriter.flush()
        }

        logger.info { "在 $timing 内完成映射构建" }
    } catch (e: IllegalStateException) {
        // 由 kotlin.error() 抛出
        logger.error { e.message }
        exitProcess(1)
    } catch (e: Exception) {
        logger.error(e) { "发生了一个意外错误！" }
        exitProcess(1)
    }
}