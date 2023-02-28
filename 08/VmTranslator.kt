package nand2tetris.ch08

import nand2tetris.ch08.CommandType.*
import java.io.File

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
        if (translateDirectory(input, codeWriter)) {
            println("Directory $input was translated to $output")
        }
    } else {
        println("Specified file must be a vm file or a directory")
    }
    codeWriter.close()
}

fun translateDirectory(dir: File, codeWriter: CodeWriter): Boolean {
    val vmFiles = dir.listFiles { file -> file.name.endsWith(".vm") }
    return if (vmFiles!!.isEmpty()) {
        println("The directory is empty")
        false
    } else {
        vmFiles.forEach { translateFile(it, codeWriter) }
        true
    }
}

fun translateFile(file: File, codeWriter: CodeWriter) {
    val parser = Parser(file)
    codeWriter.staticContext = file.nameWithoutExtension
    codeWriter.writeInit()
    while (parser.hasMoreCommands()) {
        parser.advance()
        when (parser.commandType) {
            POP, PUSH -> codeWriter.writePushPop(parser.commandType!!, Segment.getValueOf(parser.arg1!!), parser.arg2!!)
            ARITHMETIC -> codeWriter.writeArithmetic(parser.arithmeticCommand!!)
            LABEL -> codeWriter.writeLabel(parser.arg1!!)
            GOTO -> codeWriter.writeGoto(parser.arg1!!)
            IF -> codeWriter.writeIf(parser.arg1!!)
            FUNCTION -> codeWriter.writeFunction(parser.arg1!!, parser.arg2!!)
            CALL -> codeWriter.writeCall(parser.arg1!!, parser.arg2!!)
            RETURN -> codeWriter.writeReturn()
            else -> throw IllegalStateException("Unsupported command")
        }
    }
}

private fun createOutputFile(input: File): File {
    val outputName = if (input.isDirectory) {
        input.absolutePath + "/" + input.name + ".asm"
    } else {
        input.absolutePath.replace(".vm", "") + ".asm"
    }
    return File(outputName)
}

