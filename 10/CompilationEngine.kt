package nand2tetris.ch10

import nand2tetris.ch10.token.*
import nand2tetris.ch10.token.Keyword.*
import nand2tetris.ch10.token.TokenType.*
import java.io.FileWriter

class CompilationEngine(private val tokenizer: JackTokenizer, private val writer: FileWriter) {

    private var indents: Int = 0
    private var currentToken: Token? = null
    private var futureNext = false

    fun compileClass() {
        startTag("class")
        writeToken(nextKeyword(CLASS))
        writeToken(nextIdentifier())
        writeToken(nextSymbol('{'))
        futureNext()
        while (isKeyword(FIELD, STATIC)) {
            compileClassVarDec()
            futureNext()
        }
        while (!isSymbol('}')) {
            compileSubroutine()
            futureNext()
        }
        writeToken(next())  // '}'
        endTag("class")
    }

    private fun compileClassVarDec() {
        startTag("classVarDec")
        if (!isKeyword(FIELD, STATIC)) {
            throw IllegalStateException("Expected but not found keywords: field, static")
        }
        compileVarDecCommon()
        endTag("classVarDec")
    }

    private fun compileSubroutine() {
        startTag("subroutineDec")
        writeToken(nextKeyword(CONSTRUCTOR, FUNCTION, METHOD))
        writeToken(nextKeywordOrIdentifier(VOID, INT, CHAR, BOOLEAN))
        writeToken(nextIdentifier())
        writeToken(nextSymbol('('))
        compileParameterList()
        writeToken(nextSymbol(')'))
        startTag("subroutineBody")
        writeToken(nextSymbol('{'))
        futureNext()
        while (isKeyword(VAR)) {
            compileVarDec()
            futureNext()
        }
        compileStatements()
        writeToken(nextSymbol('}'))
        endTag("subroutineBody")
        endTag("subroutineDec")
    }

    private fun compileParameterList() {
        startTag("parameterList")
        futureNext()
        while (!isSymbol(')')) {
            if (isPrimitiveType() || isIdentifier()) {
                writeToken(next())  // type
                writeToken(nextIdentifier())  // parameter name
                futureNext()
                if (isSymbol(',')) {
                    writeToken(next()) // move to the future next ','
                    futureNext()  // type of the next parameter
                } else if (!isSymbol(')')) {
                    unexpectedTokenError(peek())
                }
            } else {
                unexpectedTokenError(peek())
            }
        }
        endTag("parameterList")
    }

    private fun compileVarDec() {
        startTag("varDec")
        if (!isKeyword(VAR)) {
            throw IllegalStateException("Expected but not found keywords: var")
        }
        compileVarDecCommon()
        endTag("varDec")
    }

    private fun compileVarDecCommon() {
        writeToken(next())  // var, field or static
        writeToken(nextKeywordOrIdentifier(INT, CHAR, BOOLEAN))
        do {
            writeToken(nextIdentifier())
            val token = nextSymbol(';', ',')
            writeToken(token)
        } while (isSymbol(','))
    }

    private fun compileStatements() {
        startTag("statements")
        futureNext()
        while (isKeyword(LET, IF, WHILE, DO, RETURN)) {
            when ((peek() as KeywordToken).keyword) {
                LET -> compileLet()
                IF -> compileIf()
                WHILE -> compileWhile()
                DO -> compileDo()
                RETURN -> compileReturn()
                else -> throw IllegalStateException("Wrong statement ${peek().getString()}")
            }
            futureNext()
        }
        endTag("statements")
    }

    private fun compileDo() {
        startTag("doStatement")
        writeToken(nextKeyword(DO))
        compileSubroutineCall(false)
        writeToken(nextSymbol(';'))
        endTag("doStatement")
    }

    private fun compileSubroutineCall(skipFirstIdentifier: Boolean) {
        if (!skipFirstIdentifier) {
            writeToken(nextIdentifier())
        }
        val symbol = nextSymbol('.', '(')
        writeToken(symbol)
        if (symbol.symbol == '.') {  // class method call
            writeToken(nextIdentifier())
            writeToken(nextSymbol('('))
        }
        compileExpressionList()
        writeToken(nextSymbol(')'))
    }

    private fun compileLet() {
        startTag("letStatement")
        writeToken(nextKeyword(LET))
        writeToken(nextIdentifier())
        val symbol = nextSymbol('[', '=')
        if (symbol.symbol == '[') {
            writeToken(peek()) // '['
            compileExpression()
            writeToken(nextSymbol(']'))
            nextSymbol('=')
        }
        writeToken(peek())  // '='
        compileExpression()
        writeToken(nextSymbol(';'))
        endTag("letStatement")
    }

    private fun compileWhile() {
        startTag("whileStatement")
        writeToken(nextKeyword(WHILE))
        writeToken(nextSymbol('('))
        compileExpression()
        writeToken(nextSymbol(')'))
        writeToken(nextSymbol('{'))
        compileStatements()
        writeToken(nextSymbol('}'))
        endTag("whileStatement")
    }

    private fun compileReturn() {
        startTag("returnStatement")
        writeToken(nextKeyword(RETURN))
        futureNext()
        if (!isSymbol(';')) {
            compileExpression()
        }
        writeToken(next()) // ';'
        endTag("returnStatement")
    }

    private fun compileIf() {
        startTag("ifStatement")
        writeToken(nextKeyword(IF))
        writeToken(nextSymbol('('))
        compileExpression()
        writeToken(nextSymbol(')'))
        writeToken(nextSymbol('{'))
        compileStatements()
        writeToken(nextSymbol('}'))
        futureNext()
        if (isKeyword(ELSE)) {
            writeToken(next()) // move to the future 'else'
            writeToken(nextSymbol('{'))
            compileStatements()
            writeToken(nextSymbol('}'))
        }
        endTag("ifStatement")
    }

    private fun compileExpression() {
        startTag("expression")
        compileTerm()
        futureNext()
        while (isSymbol('+', '-', '*', '/', '&', '|', '<', '>', '=')) {
            writeToken(next())  // move to future op symbol
            compileTerm()
            futureNext()
        }
        endTag("expression")
    }

    private fun compileTerm() {
        startTag("term")
        next()
        if (isInteger() || isString() || isKeyword(TRUE, FALSE, NULL, THIS)) {
            writeToken(peek())
        } else if (isSymbol('-', '~')) { // unary operation
            writeToken(peek())  // unary operation
            compileTerm()
        } else if (isSymbol('(')) {
            writeToken(peek())  // symbol '('
            compileExpression()
            writeToken(nextSymbol(')'))
        } else if (isIdentifier()) {
            writeToken(peek())
            futureNext()
            if (isSymbol('[')) {  // array
                writeToken(next())  // symbol '['
                compileExpression()
                writeToken(nextSymbol(']'))
            } else if (isSymbol('.', '(')) {
                compileSubroutineCall(true)
            }
        } else {
            unexpectedTokenError(peek())
        }
        endTag("term")
    }

    private fun compileExpressionList() {
        startTag("expressionList")
        futureNext()
        while (!isSymbol(')')) {
            compileExpression()
            futureNext()
            if (isSymbol(',')) {
                writeToken(next())  // ','
            } else if (!isSymbol(')')) {
                unexpectedTokenError(peek())
            }
        }
        endTag("expressionList")
    }

    private fun isKeyword(vararg keywords: Keyword): Boolean {
        return peek().tokenType == KEYWORD && keywords.contains((peek() as KeywordToken).keyword)
    }

    private fun isSymbol(vararg symbols: Char): Boolean {
        return peek().tokenType == SYMBOL && symbols.contains((peek() as SymbolToken).symbol)
    }

    private fun isIdentifier(): Boolean {
        return peek().tokenType == IDENTIFIER
    }

    private fun isInteger(): Boolean {
        return peek().tokenType == INT_CONST
    }

    private fun isString(): Boolean {
        return peek().tokenType == STRING_CONST
    }

    private fun isPrimitiveType(): Boolean {
        val token = peek()
        return token.tokenType == KEYWORD &&
                ((token as KeywordToken).keyword == INT
                        || token.keyword == CHAR
                        || token.keyword == BOOLEAN)
    }

    private fun nextSymbol(vararg symbols: Char): SymbolToken {
        val token = next()
        if (token.tokenType == SYMBOL) {
            if (!symbols.contains((token as SymbolToken).symbol)) {
                throw CompilationError("Unexpected symbol ${token.getString()}")
            }
        } else {
            unexpectedTokenError(token)
        }
        currentToken = token
        return token as SymbolToken
    }

    private fun nextKeyword(vararg keywords: Keyword): KeywordToken {
        val token = next()
        if (token.tokenType == KEYWORD) {
            if (!keywords.contains((token as KeywordToken).keyword)) {
                throw CompilationError("Unexpected keyword: ${token.getString()}")
            }
        } else {
            unexpectedTokenError(token)
        }
        currentToken = token
        return token as KeywordToken
    }

    private fun nextKeywordOrIdentifier(vararg keywords: Keyword): Token {
        val token = next()
        if (token.tokenType == KEYWORD) {
            if (!keywords.contains((token as KeywordToken).keyword)) {
                unexpectedTokenError(token)
            }
        } else if (token.tokenType != TokenType.IDENTIFIER) {
            unexpectedTokenError(token)
        }
        currentToken = token
        return token
    }

    private fun nextIdentifier(): Token {
        val token = next()
        if (token.tokenType != IDENTIFIER) {
            unexpectedTokenError(token)
        }
        currentToken = token
        return token
    }

    private fun next(): Token {
        if (futureNext) {
            futureNext = false
            return currentToken!!
        }
        assertMoreTokens()
        currentToken = tokenizer.getNextToken()
        return currentToken!!
    }

    private fun peek() = currentToken!!

    private fun futureNext(): Token {
        val futureToken = next()
        futureNext = true
        return futureToken
    }

    private fun assertMoreTokens() {
        if (!tokenizer.hasMoreTokens()) {
            throw CompilationError("Unexpected end of file")
        }
    }

    private fun writeToken(token: Token) {
        when (token.tokenType) {
            KEYWORD -> completeTag("keyword", token.getString())
            IDENTIFIER -> completeTag("identifier", token.getString())
            SYMBOL -> completeTag("symbol", token.getString())
            INT_CONST -> completeTag("integerConstant", token.getString())
            STRING_CONST -> completeTag("stringConstant", token.getString())
        }
    }

    private fun unexpectedTokenError(token: Token) {
        throw CompilationError("Unexpected token: ${token.getString()}")
    }

    private fun completeTag(tag: String, value: String) {
        append("<$tag>$value</$tag>")
    }

    private fun startTag(tag: String) {
        append("<$tag>")
        indents ++
    }

    private fun endTag(tag: String) {
        indents --
        append("</$tag>")
    }

    private fun append(str: String) {
        append(str, indents)
    }

    private fun append(str: String, spaces: Int) {
        val builder = StringBuilder()
        for (i in 1..spaces) builder.append("  ")
        builder.append(str)
        writer.appendLine(builder.toString())
    }
}