package nand2tetris.ch10

import nand2tetris.ch10.token.*
import java.io.File
import java.util.LinkedList

class JackTokenizer(input: File) {

    private val reader = input.bufferedReader()
    private var currentToken: Token? = null
    private var lineNumber = 0;

    fun hasMoreTokens(): Boolean {
        if (currentToken != null) {
            return true
        }
        currentToken = readNextToken()
        return currentToken != null
    }

    fun getNextToken(): Token {
        if (currentToken == null) {
            currentToken = readNextToken()
        }
        if (currentToken == null) {
            throw RuntimeException("There no tokens, check with hasMoreTokens method before obtaining a token")
        }
        val result = currentToken!!
        currentToken = readNextToken()
        return result
    }

    private val tokenBuffer = LinkedList<Token>()
    private val keywords = Keyword.values().map { value -> value.name.lowercase() }

    private fun readNextToken(): Token? {
        if (tokenBuffer.isNotEmpty()) {
            return tokenBuffer.remove()
        }
        var nextLine: String?
        do {
            nextLine = readNextLine()
            if (nextLine == null) {
                return null
            }
        } while (nextLine!!.isBlank())

        var chIndex = 0
        while (chIndex < nextLine.length) {
            val ch = nextLine[chIndex]
            if (ch == '"') {
                val value = parseStringConstant(nextLine, chIndex)
                tokenBuffer.add(StringConstantToken(value))
                chIndex += value.length + 2
            } else if (Character.isDigit(ch)) {
                val value = parseIntConstant(nextLine, chIndex)
                tokenBuffer.add(IntConstantToken(value.toInt()))
                chIndex += value.length
            } else if (Character.isLetter(ch) || ch == '_') {
                val value = parseIdentifier(nextLine, chIndex)
                if (keywords.contains(value)) {
                    tokenBuffer.add(KeywordToken(Keyword.valueOf(value.uppercase())))
                } else {
                    tokenBuffer.add(IdentifierToken(value))
                }
                chIndex += value.length
            } else if (!Character.isWhitespace(ch)) {
                tokenBuffer.add(SymbolToken(ch))
                chIndex ++
            } else {
                chIndex ++
            }
        }

        return if (tokenBuffer.isNotEmpty()) tokenBuffer.remove() else null
    }

    private fun parseStringConstant(line: String, start: Int): String {
        var chIndex = start + 1
        val stringBuilder = StringBuilder()
        while (chIndex < line.length && line[chIndex] != '"') {
            stringBuilder.append(line[chIndex])
            chIndex ++
        }
        if (chIndex == line.length) {
            throw CompilationError("Missing double quote at line $lineNumber")
        }
        return stringBuilder.toString()
    }

    private fun parseIdentifier(line: String, start: Int): String {
        var chIndex = start
        val stringBuilder = StringBuilder()
        while (chIndex < line.length && (Character.isLetterOrDigit(line[chIndex]) || line[chIndex] == '_')) {
            stringBuilder.append(line[chIndex])
            chIndex ++
        }
        return stringBuilder.toString()
    }

    private fun parseIntConstant(line: String, start: Int): String {
        var chIndex = start
        val stringBuilder = StringBuilder()
        while (chIndex < line.length && Character.isDigit(line[chIndex])) {
            stringBuilder.append(line[chIndex])
            chIndex ++
        }
        if (chIndex < line.length && Character.isLetter(line[chIndex])) {
            throw CompilationError("Incorrect identifier at line $lineNumber")
        }
        return stringBuilder.toString()
    }

    var commentWasOpen = false

    private fun readNextLine(): String? {
        val line = reader.readLine() ?: return null
        val removedComments = removeComments(line, commentWasOpen)
        commentWasOpen = removedComments.second
        lineNumber ++
        return removedComments.first.trim()
    }

    private fun removeComments(line: String, commentWasOpen: Boolean): Pair<String, Boolean> {
        if (commentWasOpen) {  // multiline comment was open in previous lines
            val endIndex = line.indexOf("*/")
            if (endIndex == -1) {
                return Pair("", true)
            }
            val tail = line.substring(endIndex + 2, line.length)
            return removeComments(tail, false)
        }
        val startIndex = line.indexOf("/*")
        val startOneLineComment = line.indexOf("//")
        if (startOneLineComment != -1 && (startOneLineComment < startIndex || startIndex == -1)) {
            return Pair(line.substring(0, startOneLineComment), false)
        }
        if (startIndex == -1) {
            return Pair(line, false)
        }
        val endIndex = line.indexOf("*/", startIndex + 2)
        if (endIndex == -1) {  // multiline comment doesn't end in this line
            return Pair(line.substring(0, startIndex), true)
        }
        val leftPart = line.substring(0, startIndex)
        val tail = removeComments(line.substring(endIndex + 2, line.length), false)
        return Pair(leftPart + " " + tail.first, tail.second)
    }

}
