package nand2tetris.ch11

import java.io.FileWriter

class VmWriter(private val writer: FileWriter) {

    fun writePush(segment: Segment, index: Int) {
        append("push ${segment.name.lowercase()} $index", true)
    }

    fun writePop(segment: Segment, index: Int) {
        append("pop ${segment.name.lowercase()} $index", true)
    }

    fun writeArithmetic(command: ArithmeticCommand) {
        append(command.name.lowercase(), true)
    }

    fun writeLabel(label: String) {
        append("label $label", false)
    }

    fun writeGoto(label: String) {
        append("goto $label", true)
    }

    fun writeIf(label: String) {
        append("if-goto $label", true)
    }

    fun writeCall(name: String, nArgs: Int) {
        append("call $name $nArgs", true)
    }

    fun writeFunction(name: String, nLocals: Int) {
        append("function $name $nLocals", false)
    }

    fun writeReturn() {
        append("return", true)
    }

    private fun append(str: String, tab: Boolean) {
        val line = if (tab) {
            "  $str"
        } else {
            str
        }
        writer.appendLine(line)
    }
}