package nand2tetris.ch07

import nand2tetris.ch07.ArithmeticCommand.*
import nand2tetris.ch07.CommandType.*
import nand2tetris.ch07.Segment.*
import java.io.File
import java.io.FileWriter

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Specify input file or directory as an argument")
        return
    }
    val input = File(args[0])
    val output = createOutputFile(input)
    val codeWriter = CodeWriter(output)
    if (args[0].endsWith(".vm")) {
        translateFile(input, codeWriter)
        println("File $input was translated to $output")
    } else if (input.isDirectory) {
        translateDirectory(input, codeWriter)
        println("Directory $input was translated to $output")
    } else {
        println("Specified file must be a vm file or a directory")
    }
    codeWriter.close()
}

private fun createOutputFile(input: File): File {
    val outputName = input.absolutePath.replace(".vm", "") + ".asm"
    return File(outputName)
}

fun translateDirectory(dir: File, codeWriter: CodeWriter) {
    val vmFiles = dir.listFiles { pathname -> pathname.endsWith(".vm") }
    if (vmFiles!!.isEmpty()) {
        println("The directory is empty")
    } else {
        vmFiles.forEach { translateFile(it, codeWriter) }
    }
}

fun translateFile(file: File, codeWriter: CodeWriter) {
    val parser = Parser(file)
    codeWriter.staticContext = file.nameWithoutExtension
    while (parser.hasMoreCommands()) {
        parser.advance()
        if (parser.commandType == ARITHMETIC) {
            codeWriter.writeArithmetic(parser.arithmeticCommand!!)
        } else if (parser.commandType == POP || parser.commandType == PUSH) {
            codeWriter.writePushPop(parser.commandType!!, Segment.getValueOf(parser.arg1!!), parser.arg2!!)
        }
    }
}

class CodeWriter(file: File): AutoCloseable {
    private val writer = FileWriter(file)
    private var pc = 0
    var staticContext: String = "0"

    fun writeArithmetic(command: ArithmeticCommand) {
        when (command) {
            ADD -> {
                popToD()
                pop()
                push("M+D")
            }
            SUB -> {
                popToD()
                pop()
                push("M-D")
            }
            NEG -> {
                popToD()
                push("-D")
            }
            NOT -> {
                popToD()
                push("!D")
            }
            EQ -> {
                compare("JEQ")
            }
            LT -> {
                compare("JLT")
            }
            GT -> {
                compare("JGT")
            }
            AND -> {
                popToD()
                pop()
                push("M&D")
            }
            OR -> {
                popToD()
                pop()
                push("M|D")

            }
        }
    }

    private fun compare(jump: String) {
        popToD()
        pop()
        append("D=M-D")
        append("@${pc + 8}")
        append("D;$jump")
        pushConstant(0)
        append("@${pc + 6}")
        append("0;JMP")
        pushConstant(-1)
    }

    fun writePushPop(command: CommandType, segment: Segment, index: Int) {
        when (command) {
            POP -> {
                when (segment) {
                    CONSTANT -> {
                        throw IllegalStateException("Invalid pop location: constant")
                    }
                    LOCAL -> {
                        // load address (local + index) to general purpose register
                        moveValueTo("LCL", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    ARGUMENT -> {
                        // load address (arg + index) to general purpose register
                        moveValueTo("ARG", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    THIS -> {
                        // load address (this + index) to general purpose register
                        moveValueTo("THIS", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    THAT -> {
                        // load address (that + index) to general purpose register
                        moveValueTo("THAT", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    POINTER -> {
                        // R3 - address of pointer
                        moveAddressTo("R3", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    TEMP -> {
                        // R5 - address of temp
                        moveAddressTo("R5", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    STATIC -> {
                        popToD()
                        append("@$staticContext.$index")
                        append("M=D")
                    }
                }
            }

            PUSH -> {
                when (segment) {
                    CONSTANT -> {
                        append("@$index")
                        append("D=A")
                        pushFromD()
                    }
                    LOCAL -> {
                        loadToDByPointer("LCL", index)
                        pushFromD()
                    }
                    ARGUMENT -> {
                        loadToDByPointer("ARG", index)
                        pushFromD()
                    }
                    THIS -> {
                        loadToDByPointer("THIS", index)
                        pushFromD()
                    }
                    THAT -> {
                        loadToDByPointer("THAT", index)
                        pushFromD()
                    }
                    POINTER -> {
                        append("@THIS")
                        append("D=A")
                        append("@$index")
                        append("A=D+A")
                        append("D=M")
                        pushFromD()
                    }
                    TEMP -> {
                        append("@R5")
                        append("D=A")
                        append("@$index")
                        append("A=D+A")
                        append("D=M")
                        pushFromD()
                    }
                    STATIC -> {
                        append("@$staticContext.$index")
                        append("D=M")
                        pushFromD()
                    }

                }
            }
            else -> throw IllegalStateException("Wrong command type, expected push or pop")
        }
    }

    private fun popToD() {
        pop()
        append("D=M")
    }

    private fun pop() {
        append("@SP")
        append("AM=M-1")
    }

    private fun pushConstant(value: Int) {
        append("@SP")
        append("A=M")
        push(value.toString())
    }

    private fun push(value: String) {
        append("M=$value")
        append("@SP")
        append("M=M+1")
    }

    private fun loadToD(mem: String, index: Int) {
        append("@$mem")
        if (index == 0) {
            append("D=M")
        } else {
            append("D=M")
            append("@$index")
            append("D=D+A")
        }
    }

    private fun loadToDByPointer(mem: String, index: Int) {
        append("@$mem")
        if (index == 0) {
            append("A=M")
        } else {
            append("D=M")
            append("@$index")
            append("A=D+A")
        }
        append("D=M")
    }

    private fun moveValueTo(mem: String, index: Int, target: String) {
        loadToD(mem, index)
        append("@$target")
        append("M=D")
    }

    private fun moveAddressTo(mem: String, index: Int, target: String) {
        append("@$mem")
        append("D=A")
        if (index > 0) {
            append("@$index")
            append("D=D+A")
        }
        append("@$target")
        append("M=D")
    }

    private fun pushFromD() {
        append("@SP")
        append("A=M")
        append("M=D")
        append("@SP")
        append("M=M+1")
    }

    private fun append(str: String) {
        writer.appendLine(str)
        pc++
    }

    override fun close() {
        writer.close()
    }
}

open class Parser(private val file: File) {

    private var commandIterator = VmCommandIterator(file)

    var commandType: CommandType? = null
        private set

    var arithmeticCommand: ArithmeticCommand? = null
        private set

    var arg1: String? = null
        private set

    var arg2: Int? = null
        private set

    fun reset() {
        commandIterator = VmCommandIterator(file)
    }

    fun advance() {
        parse(commandIterator.next())
    }

    fun hasMoreCommands(): Boolean {
        return commandIterator.hasNext()
    }

    private fun parse(command: String) {
        val matchResult = Regex("^(\\w+[-\\w]*)\\s?(\\w+)?\\s?(\\d+)?$").find(command)
            ?: throw IllegalArgumentException("Illegal command format: $command")

        val commandAndArgs = matchResult.groupValues
        parseCommandType(commandAndArgs[1])
        if (commandAndArgs.size > 2 && commandAndArgs[2].isNotBlank()) {
            parseArgument1(commandAndArgs[2])
        }
        if (commandAndArgs.size > 3 && commandAndArgs[3].isNotBlank()) {
            parseArgument2(commandAndArgs[3])
        }
    }

    private fun parseArgument1(argument: String) {
        arg1 = argument
    }

    private fun parseArgument2(argument: String) {
        arg2 = argument.toInt()
    }

    private fun parseCommandType(command: String) {
        commandType = when (command) {
            "label" -> LABEL
            "goto" -> GOTO
            "if-goto" -> IF
            "function" -> FUNCTION
            "call" -> CALL
            "return" -> RETURN
            "pop" -> POP
            "push" -> PUSH
            else -> ARITHMETIC
        }
        arithmeticCommand = if (commandType == ARITHMETIC) {
            ArithmeticCommand.values().filter { command == it.name.lowercase() }.firstOrNull()
                ?: throw IllegalArgumentException("Illegal command: $command")
        } else {
            null
        }
    }
}

class VmCommandIterator(file: File) : AbstractIterator<String>() {

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

enum class Segment {
    CONSTANT,
    LOCAL,
    ARGUMENT,
    STATIC,
    THIS,
    THAT,
    POINTER,
    TEMP;

    companion object {

        fun getValueOf(str: String): Segment {
            return Segment.valueOf(str.uppercase())
        }
    }
}

enum class CommandType {
    ARITHMETIC,
    PUSH, POP,
    LABEL,
    GOTO, IF,
    FUNCTION,
    RETURN,
    CALL
}

enum class ArithmeticCommand {
    ADD,
    SUB,
    NEG,
    EQ,
    GT,
    LT,
    AND,
    OR,
    NOT
}
