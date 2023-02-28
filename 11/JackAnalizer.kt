package nand2tetris.ch11

import java.io.File
import java.io.FileWriter

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Specify input file or directory as an argument")
        return
    }
    val input = File(args[0])
    if (isJackFile(input.name)) {
        compileFile(input)
    } else if (input.isDirectory) {
        compileFilesInDirectory(input)
    } else {
        println("Specified file must be a jack file or a directory")
    }
}

private fun isJackFile(fileName: String): Boolean {
    return fileName.endsWith(".jack")
}

fun compileFilesInDirectory(dir: File) {
    val jackFiles = dir.listFiles { file -> isJackFile(file.name) }
    return if (jackFiles!!.isEmpty()) {
        println("jack files not found in $dir")
    } else {
        jackFiles.forEach { compileFile(it) }
    }
}

fun compileFile(input: File) {
    val output = createOutputFile(input)
    val writer = FileWriter(output)
    val tokenizer = JackTokenizer(input)
    writer.use {
        try {
            val vmWriter = VmWriter(writer)
            val compilationEngine = CompilationEngine(tokenizer, vmWriter)
            compilationEngine.compileClass()
            println("Compiled $output")
        } catch (ex: CompilationError) {
            println("Compilation error in file $input: ${ex.message}")
        }
    }
}

private fun createOutputFile(input: File): File {
    val outputName = input.absolutePath.replace(".jack", "") + ".vm"
    return File(outputName)
}

