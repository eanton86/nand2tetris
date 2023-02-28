package nand2tetris.ch08

import java.io.File
import java.io.FileWriter

class CodeWriter(outputFile: File): AutoCloseable {
    private val writer = FileWriter(outputFile)
    private var pc = 0
    private var currentFunction: String = ""
    var staticContext: String = "0"


    fun writeInit() {
        // SP = 261
        append("@261")
        append("D=A")
        append("@SP")
        append("M=D")
        // call Sys.init
        append("@Sys.init")
        append("0;JMP")
    }

    fun writeLabel(label: String) {
        append("($currentFunction$$label)")
    }

    fun writeGoto(label: String) {
        append("@$currentFunction$$label")
        append("0;JMP")
    }

    fun writeIf(label: String) {
        popToD()
        append("@$currentFunction$$label")
        append("D;JNE")
    }

    fun writeCall(functionName: String, numArgs: Int) {
        // push return address
        val returnLabel = "$currentFunction\$return$pc"
        append("@$returnLabel")
        append("D=A")
        pushFromD()

        // push LCL
        loadToD("LCL", 0)
        pushFromD()

        // push ARG
        loadToD("ARG", 0)
        pushFromD()

        // push THIS
        loadToD("THIS", 0)
        pushFromD()

        // push THAT
        loadToD("THAT", 0)
        pushFromD()

        // reposition ARG to the args pushed before the call = SP - 5 - numArgs
        append("@SP")
        append("D=M")
        append("@5")
        append("D=D-A")
        append("@$numArgs")
        append("D=D-A")
        append("@ARG")
        append("M=D")

        // reposition local to the start of the called function
        moveTo("SP", 0, "LCL")

        // goto called function
        append("@$functionName")
        append("0;JMP")
        append("($returnLabel)")
    }

    fun writeReturn() {
        // save return position to temp variable
        moveByPointerTo("LCL", -5, "R13")

        // pop returned value, put it into the top of the calling function stack (ARGs of the called function)
        popToD()
        append("@ARG")
        append("A=M")
        append("M=D")

        // restore SP
        moveTo("ARG", 1, "SP")

        // restore that
        moveByPointerTo("LCL", -1, "THAT")

        // restore this
        moveByPointerTo("LCL", -2, "THIS")

        // restore arg
        moveByPointerTo("LCL", -3, "ARG")

        // restore lcl
        moveByPointerTo("LCL", -4, "LCL")

        // continue the calling function
        append("@R13")
        append("A=M")
        append("0;JMP")
    }

    fun writeFunction(functionName: String, numLocals: Int) {
        append("($functionName)")
        for (i in 1..numLocals) {
            pushConstant(0)
        }
        currentFunction = functionName
    }

    fun writeArithmetic(command: ArithmeticCommand) {
        when (command) {
            ArithmeticCommand.ADD -> {
                popToD()
                pop()
                push("M+D")
            }
            ArithmeticCommand.SUB -> {
                popToD()
                pop()
                push("M-D")
            }
            ArithmeticCommand.NEG -> {
                popToD()
                push("-D")
            }
            ArithmeticCommand.NOT -> {
                popToD()
                push("!D")
            }
            ArithmeticCommand.EQ -> {
                compare("JEQ")
            }
            ArithmeticCommand.LT -> {
                compare("JLT")
            }
            ArithmeticCommand.GT -> {
                compare("JGT")
            }
            ArithmeticCommand.AND -> {
                popToD()
                pop()
                push("M&D")
            }
            ArithmeticCommand.OR -> {
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
            CommandType.POP -> {
                when (segment) {
                    Segment.CONSTANT -> {
                        throw IllegalStateException("Invalid pop location: constant")
                    }
                    Segment.LOCAL -> {
                        // load address (local + index) to general purpose register
                        moveTo("LCL", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    Segment.ARGUMENT -> {
                        // load address (arg + index) to general purpose register
                        moveTo("ARG", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    Segment.THIS -> {
                        // load address (this + index) to general purpose register
                        moveTo("THIS", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    Segment.THAT -> {
                        // load address (that + index) to general purpose register
                        moveTo("THAT", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    Segment.POINTER -> {
                        // R3 - address of pointer
                        moveAddressTo("R3", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    Segment.TEMP -> {
                        // R5 - address of temp
                        moveAddressTo("R5", index, "R13")
                        popToD()
                        append("@R13")
                        append("A=M")
                        append("M=D")
                    }
                    Segment.STATIC -> {
                        popToD()
                        append("@$staticContext.$index")
                        append("M=D")
                    }
                }
            }

            CommandType.PUSH -> {
                when (segment) {
                    Segment.CONSTANT -> {
                        append("@$index")
                        append("D=A")
                        pushFromD()
                    }
                    Segment.LOCAL -> {
                        loadToDByPointer("LCL", index)
                        pushFromD()
                    }
                    Segment.ARGUMENT -> {
                        loadToDByPointer("ARG", index)
                        pushFromD()
                    }
                    Segment.THIS -> {
                        loadToDByPointer("THIS", index)
                        pushFromD()
                    }
                    Segment.THAT -> {
                        loadToDByPointer("THAT", index)
                        pushFromD()
                    }
                    Segment.POINTER -> {
                        append("@THIS")
                        append("D=A")
                        append("@$index")
                        append("A=D+A")
                        append("D=M")
                        pushFromD()
                    }
                    Segment.TEMP -> {
                        append("@R5")
                        append("D=A")
                        append("@$index")
                        append("A=D+A")
                        append("D=M")
                        pushFromD()
                    }
                    Segment.STATIC -> {
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
        append("D=M")
        if (index > 0) {
            append("@$index")
            append("D=D+A")
        } else if (index < 0) {
            append("@${-index}")
            append("D=D-A")
        }
    }

    private fun loadToDByPointer(mem: String, index: Int) {
        append("@$mem")
        if (index == 0) {
            append("A=M")
        } else if (index > 0) {
            append("D=M")
            append("@$index")
            append("A=D+A")
        } else {
            append("D=M")
            append("@${-index}")
            append("A=D-A")
        }
        append("D=M")
    }

    private fun moveTo(mem: String, index: Int, target: String) {
        loadToD(mem, index)
        append("@$target")
        append("M=D")
    }

    private fun moveByPointerTo(mem: String, index: Int, target: String) {
        loadToDByPointer(mem, index)
        append("@$target")
        append("M=D")
    }

    private fun moveAddressTo(mem: String, index: Int, target: String) {
        append("@$mem")
        append("D=A")
        if (index > 0) {
            append("@$index")
            append("D=D+A")
        } else if (index < 0) {
            append("@${-index}")
            append("D=D-A")
        }
        append("@$target")
        append("M=D")
    }

    private fun pushFromD() {
        append("@SP")
        append("A=M")
        push("D")
    }

    private fun append(str: String) {
        writer.appendLine(str)
        pc++
    }

    override fun close() {
        writer.close()
    }
}