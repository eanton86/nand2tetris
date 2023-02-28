package nand2tetris.ch08

import java.io.File

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
        val matchResult = Regex("^(\\w+[-\\w]*)\\s?(\\w+[.$\\w]*)?\\s?(\\d+)?$").find(command)
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
            "label" -> CommandType.LABEL
            "goto" -> CommandType.GOTO
            "if-goto" -> CommandType.IF
            "function" -> CommandType.FUNCTION
            "call" -> CommandType.CALL
            "return" -> CommandType.RETURN
            "pop" -> CommandType.POP
            "push" -> CommandType.PUSH
            else -> CommandType.ARITHMETIC
        }
        arithmeticCommand = if (commandType == CommandType.ARITHMETIC) {
            ArithmeticCommand.values().firstOrNull { command == it.name.lowercase() }
                ?: throw IllegalArgumentException("Illegal command: $command")
        } else {
            null
        }
    }
}