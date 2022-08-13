package com.kraskaska.runner

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.util.*
import kotlin.concurrent.thread

val parser = ArgParser("kraskaska-runner")
val commandToRun by parser.option(ArgType.String, "command", "c", "Command to keep running while runner is alive")
    .required()
val restartIfGrace by parser.option(
    ArgType.Int,
    "restartIfGracefulExit",
    "r",
    "Restart even if command ended with 0 code (1 = true, anything else = false)"
).default(1)
val webhook by parser.option(ArgType.String, "webhookUrl", "w", "Webhook url to post restart warnings")
val strikesAmount by parser.option(
    ArgType.Int,
    "strikesAmount",
    "s",
    "Strikes (non-graceful, non-zero exits) needed to exit from runner. Set to 0 to disable"
).default(3)
val strikesInterval by parser.option(
    ArgType.Int,
    "strikesInterval",
    "i",
    "Seconds need to pass since last strike before resetting strike count to 0"
).default(60 * 15)
val treatZeroAsError by parser.option(
    ArgType.Boolean,
    "treatZeroAsError",
    null,
    "Zero will act like it's crashed"
)

val WEBHOOK_WARN_COLOR = 16318296
val WEBHOOK_ERROR_COLOR = 16734296
var strikes = 0
var timeOfLastStrike = (Date().time / 1000).toInt()
var isWindows = System.getProperty("os.name")
    .lowercase(Locale.getDefault()).startsWith("windows")

fun main(args: Array<String>) {
    println("Kraskaska's Runner")
    println("Program arguments: ${args.joinToString(", ")}")
    parser.parse(args)
    if (webhook == "null") println("[!] Webhook is not provided, runner will not send status to discord")
    webhook(
        "Kraskaska's Runner",
        "Runner has started and will now post status messages with this webhook in this channel!",
        WEBHOOK_WARN_COLOR
    )
    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        webhook("Shutting down!", "Runner has been instructed to exit!", WEBHOOK_ERROR_COLOR)
        println("[!] Shutting down! Runner has been instructed to exit!")
    })
    run(commandToRun)
}

fun webhook(title: String, content: String?, color: Int) {
    webhook?.let {
        val content = JSONObject(
            mapOf(
                "embeds" to arrayOf(
                    mapOf(
                        "title" to title,
                        "description" to content,
                        "color" to color
                    )
                )
            )
        )
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(webhook))
            .POST(HttpRequest.BodyPublishers.ofString(content.toString()))
            .header("Content-Type", "application/json")
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}

fun run(command: String) {
    var lastCode = 0
    while (lastCode != 0 || restartIfGrace == 1) {
        val processBuilder = ProcessBuilder()
        if (isWindows) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
        val process = processBuilder
            .directory(File(Paths.get("").toAbsolutePath().toString()))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        lastCode = process.waitFor()
        if (lastCode != 0 || treatZeroAsError == true) {
            println("[!] Process exited with $lastCode")
            if ((Date().time / 1000).toInt() - timeOfLastStrike >= strikesInterval) {
                strikes = 0
                webhook("Strikes have been reset!", null, WEBHOOK_WARN_COLOR)
                println("[i] Strikes have been reset!")
            }
            timeOfLastStrike = (Date().time / 1000).toInt()
            strikes++
            if (strikes >= strikesAmount && strikesAmount > 0) {
                webhook(
                    "Strikes exceed or at max strike amount! (${strikes}/${strikesAmount})",
                    "Exit code: `${lastCode}`\nRunner will shut down! Please contact administrators about this problem.",
                    WEBHOOK_ERROR_COLOR
                )
                println("[!] Strikes exceed or at max strike amount! (${strikes}/${strikesAmount})")
                break
            } else {
                webhook(
                    "Command has crashed${if (strikesAmount > 0) " (${strikes}/${strikesAmount})" else ""}",
                    "Exit code: `${lastCode}`",
                    WEBHOOK_WARN_COLOR
                )
                println("[!] Command has crashed${if (strikesAmount > 0) " (${strikes}/${strikesAmount})" else ""}")
            }
        } else {
            webhook(
                "Command has exited${if (strikesAmount > 0) " (${strikes}/${strikesAmount})" else ""}",
                "Exit code: `${lastCode}`\nThis will not be counted as strike.",
                WEBHOOK_WARN_COLOR
            )
            println("[i] Command has exited${if (strikesAmount > 0) " (${strikes}/${strikesAmount})" else ""}")
        }
    }
}