package nand2tetris.ch06

import nand2tetris.ch06.CommandType.*
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Specify *.asm file as argument")
        return
    }
    initSymbols()
    val parser = Parser(File(args[0]))


    var pc = 0;
    while (parser.hasMoreCommands()) {
        parser.advance()
        if (parser.commandType == L_COMMAND) {
            symbols[parser.symbol] = pc
        } else {
            pc++
        }
    }

    var memoryCounter = 16
    parser.reset()
    while (parser.hasMoreCommands()) {
        parser.advance()
        if (parser.commandType == C_COMMAND) {
            printCCommand(parser)
        } else if (parser.commandType == A_COMMAND) {
            if (parser.symbol.matches(Regex("\\d+"))) {
                val value = parser.symbol.toInt()
                printACommand(value)
            } else {
                if (!symbols.contains(parser.symbol)) {
                    symbols[parser.symbol] = memoryCounter ++
                }
                printACommand(symbols[parser.symbol]!!)
            }
        }
    }
}

open class Parser(val file: File) {

    private val COMMAND_REGEX = "^(.+=)?(.+?)(;J.+)?$"

    var symbol: String = ""
        private set
    var commandType: CommandType = A_COMMAND
        private set
    var dest: String? = null
        private set
    var comp: String? = null
        private set
    var jump: String? = null
        private set

    private var commandIterator = AsmCommandIterator(file)

    fun reset() {
        commandIterator = AsmCommandIterator(file)
    }

    fun advance() {
        parse(commandIterator.next())
    }

    fun hasMoreCommands(): Boolean {
        return commandIterator.hasNext()
    }

    private fun parse(command: String) {
        if (command.matches(Regex("^\\(.+\\)$"))) {
            parseLabel(command)
        } else if (command.matches(Regex("^@.+"))) {
            parseSymbol(command)
        } else if (command.matches(Regex(COMMAND_REGEX))) {
            parseCommand(command)
        } else {
            throw IllegalStateException("Wrong command format: $command")
        }
    }

    private fun parseLabel(labelCommand: String) {
        commandType = L_COMMAND
        symbol = labelCommand.removePrefix("(").removeSuffix(")").trim()
        if (symbol.isEmpty()) {
            throw IllegalStateException("Wrong symbol: $labelCommand")
        }
        dest = null
        comp = null
        jump = null
    }

    private fun parseSymbol(symbolCommand: String) {
        commandType = A_COMMAND
        symbol = symbolCommand.removePrefix("@").trim()
        if (symbol.isEmpty()) {
            throw IllegalStateException("Wrong symbol: $symbolCommand")
        }
        dest = null
        comp = null
        jump = null
    }

    private fun parseCommand(command: String) {
        commandType = C_COMMAND
        dest = parseDest(command)
        comp = parseComp(command)
        jump = parseJump(command)
    }

    private fun parseComp(command: String): String {
        val matchResult = Regex(COMMAND_REGEX).find(command)
            ?: throw IllegalStateException("Wring command format: $command")
        val operation = matchResult.groupValues[2]
        if (operation == "0") {
            return "0101010"
        } else if (operation == "1") {
            return "0111111"
        } else if (operation == "-1") {
            return "0111010"
        } else if (operation == "D") {
            return "0001100"
        } else if (Regex("^[MA]$").matches(operation)) {
            return aBit(operation) + "110000"
        } else if (operation == "!D") {
            return "0001101"
        } else if (Regex("^![MA]$").matches(operation)) {
            return aBit(operation) + "110001"
        } else if (operation == "-D") {
            return "0001111"
        } else if (Regex("^-[MA]$").matches(operation)) {
            return aBit(operation) + "110011"
        } else if (operation == "D+1") {
            return "0011111"
        } else if (Regex("^[MA]\\+1$").matches(operation)) {
            return aBit(operation) + "110111"
        } else if (Regex("^-[MA]$").matches(operation)) {
            return aBit(operation) + "110011"
        } else if (operation == "D-1") {
            return "0001110"
        } else if (Regex("^[MA]-1$").matches(operation)) {
            return aBit(operation) + "110010"
        } else if (Regex("^D\\+[MA]$").matches(operation)) {
            return aBit(operation) + "000010"
        } else if (Regex("^D-[MA]$").matches(operation)) {
            return aBit(operation) + "010011"
        } else if (Regex("^[MA]-D$").matches(operation)) {
            return aBit(operation) + "000111"
        } else if (Regex("^D&[MA]$").matches(operation)) {
            return aBit(operation) + "000000"
        } else if (Regex("^D\\|[MA]$").matches(operation)) {
            return aBit(operation) + "010101"
        }
        throw IllegalStateException("Wrong comp component: $operation")
    }

    private fun parseJump(command: String): String {
        val matchResult = Regex(COMMAND_REGEX).find(command)
        if (matchResult == null) {
            throw IllegalStateException("Wrong command format: $command")
        }
        if (matchResult.groupValues[3].isEmpty()) {
            return "000"
        }
        val jmpCode = matchResult.groupValues[3].replaceFirstChar { "" }
        return when (jmpCode) {
            "JGT" -> "001"
            "JEQ" -> "010"
            "JGE" -> "011"
            "JLT" -> "100"
            "JNE" -> "101"
            "JLE" -> "110"
            "JMP" -> "111"
            else -> throw IllegalStateException("Wrong jump instruction: $jmpCode")
        }
    }

    private fun parseDest(command: String): String {
        val matchResult = Regex(COMMAND_REGEX).find(command)
        if (matchResult == null) {
            throw IllegalStateException("Wring command format: $command")
        }
        if (matchResult.groupValues[1].isEmpty()) {
            return "000"
        }
        val dest = matchResult.groupValues[1].replace("=", "")
        return if (dest == "M") "001"
        else if (dest == "D") "010"
        else if (Regex("^MD|DM$").matches(dest)) "011"
        else if (dest == "A") "100"
        else if (Regex("^MA|AM$").matches(dest)) "101"
        else if (Regex("^AD|DA$").matches(dest)) "110"
        else if (Regex("^AMD|DMA|DAM|MAD|MDA|ADM$").matches(dest)) "111"
        else throw IllegalStateException("Wrong dest instruction: $dest")
    }

    private fun aBit(operation: String): String = if (operation.contains("M")) "1" else "0"

}

enum class CommandType {
    A_COMMAND,
    C_COMMAND,
    L_COMMAND
}

class AsmCommandIterator(file: File): AbstractIterator<String>() {

    private val linesIterator = file.bufferedReader().lineSequence().iterator()

    override fun computeNext() {
        if (!linesIterator.hasNext()) {
            done()
        } else {
            var str: String
            do {
                str = linesIterator.next()
                str = str.replaceAfter("//", "").replace("//", "").trim()
            } while (!isCommand(str) && linesIterator.hasNext())
            if (!isCommand(str)) {
                done()
            }
            setNext(str)
        }
    }

    fun isCommand(str: String): Boolean {
        return str.isNotBlank()
    }
}

private var symbols: MutableMap<String, Int> = mutableMapOf()

fun initSymbols() {
    symbols = mutableMapOf(
        "SP" to 0x0,
        "LCL" to 0x1,
        "ARG" to 0x2,
        "THIS" to 0x3,
        "THAT" to 0x4,
        "SCREEN" to 0x4000,
        "KBD" to 0x6000,
    )
    for (i in 0..15) {
        symbols["R" + i] = i
    }
}

fun printCCommand(parser: Parser) {
    println("111" + parser.comp + parser.dest + parser.jump)
}

fun printACommand(value: Int) {
    val str = value.toString(2)
    val zeros = 16 - str.length
    var result = ""
    for (i in 1..zeros) {
        result += "0"
    }
    result += str
    println(result)
}