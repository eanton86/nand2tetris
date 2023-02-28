package nand2tetris.ch10

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
    val tokenizer = JackTokenizer(input)
    val writer = FileWriter(output)
    writer.use {
        val compilationEngine = CompilationEngine(tokenizer, it)
        compilationEngine.compileClass()
        println("Compiled $output")
    }
}

private fun createOutputFile(input: File): File {
    val outputName = if (input.isDirectory) {
        input.absolutePath + "/" + input.name + ".xml"
    } else {
        input.absolutePath.replace(".jack", "") + "Compiled.xml"
    }
    return File(outputName)
}

