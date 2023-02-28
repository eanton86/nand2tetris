package nand2tetris.ch11

import nand2tetris.ch11.Segment.*
import nand2tetris.ch11.symbol.SymbolKind
import nand2tetris.ch11.symbol.SymbolTable
import nand2tetris.ch11.token.*
import nand2tetris.ch11.token.Keyword.*
import nand2tetris.ch11.token.Keyword.STATIC
import nand2tetris.ch11.token.Keyword.THIS
import nand2tetris.ch11.token.TokenType.*

class CompilationEngine(private val tokenizer: JackTokenizer,
                        private val vmWriter: VmWriter) {

    private var currentToken: Token? = null
    private var futureNext = false
    private val symbolTable = SymbolTable()
    private var fieldsNumber = 0
    private var inVoidSubroutine = false
    private var className = ""
    private var labelIndex = 0

    fun compileClass() {
        nextKeyword(CLASS)
        val className = nextIdentifier()
        this.className = className.getString()
        nextSymbol('{')
        futureNext()
        while (isKeyword(FIELD, STATIC)) {
            val fieldDeclaration = (peek() as KeywordToken).keyword == FIELD
            val varNumber = compileClassVarDec()
            // get number of fields for the current class
            if (fieldDeclaration) {
                fieldsNumber += varNumber;
            }
            futureNext()
        }
        while (!isSymbol('}')) {   // while not end of the class
            compileSubroutine(this.className)
            futureNext()
        }
        next()  // '}'
    }

    private fun compileClassVarDec(): Int {
        if (!isKeyword(FIELD, STATIC)) {
            throw IllegalStateException("Expected but not found keywords: field, static")
        }
        val variablesNumber = compileVarDecCommon()
        return variablesNumber
    }

    private fun compileSubroutine(className: String) {
        symbolTable.startSubroutine()
        val subroutineType = nextKeyword(CONSTRUCTOR, FUNCTION, METHOD)
        if (subroutineType.keyword == METHOD) {
            symbolTable.define("this", className, SymbolKind.ARG)
        }
        nextKeywordOrIdentifier(VOID, INT, CHAR, BOOLEAN)
        inVoidSubroutine = isKeyword(VOID)
        val subroutineName = nextIdentifier()
        nextSymbol('(')
        compileParameterList()
        nextSymbol(')')
        nextSymbol('{')
        futureNext()
        var variablesNumber = 0
        while (isKeyword(VAR)) {
            variablesNumber += compileVarDec()
            futureNext()
        }
        vmWriter.writeFunction("$className.${subroutineName.getString()}", variablesNumber)
        if (subroutineType.keyword == CONSTRUCTOR) {
            // allocate memory and set THIS to new object base
            vmWriter.writePush(CONSTANT, fieldsNumber)
            vmWriter.writeCall("Memory.alloc", 1)
            vmWriter.writePop(POINTER, 0)
        } else if (subroutineType.keyword == METHOD) {
            // first argument is an object itself, set THIS with it
            vmWriter.writePush(ARGUMENT, 0)
            vmWriter.writePop(POINTER, 0)
        }
        compileStatements()
        inVoidSubroutine = false  // reset flag after returning from function
        nextSymbol('}')
    }

    private fun compileParameterList() {
        futureNext()
        while (!isSymbol(')')) {
            if (isPrimitiveType() || isIdentifier()) {
                val type = next()
                val name = nextIdentifier()
                symbolTable.define(name.getString(), type.getString(), SymbolKind.ARG)
                futureNext()
                if (isSymbol(',')) {
                    next() // move to the future next ','
                    futureNext()  // type of the next parameter
                } else if (!isSymbol(')')) {
                    unexpectedTokenError(peek())
                }
            } else {
                unexpectedTokenError(peek())
            }
        }
    }

    private fun compileVarDec(): Int {
        if (!isKeyword(VAR)) {
            throw IllegalStateException("Expected but not found keywords: var")
        }
        val variablesNumber = compileVarDecCommon()
        return variablesNumber;
    }

    private fun compileVarDecCommon(): Int {
        var count = 0
        val kind = nextKeyword(VAR, FIELD, STATIC)
        val type = nextKeywordOrIdentifier(INT, CHAR, BOOLEAN)
        do {
            val name = nextIdentifier()
            symbolTable.define(name.getString(), type.getString(), SymbolKind.valueOf(kind.keyword.name))
            nextSymbol(';', ',')
            count ++
        } while (isSymbol(','))
        return count
    }

    private fun compileStatements() {
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
    }

    private fun compileDo() {
        nextKeyword(DO)
        compileSubroutineCall(null)
        vmWriter.writePop(TEMP, 0)  // pop and ignore result of void function
        nextSymbol(';')
    }

    private fun compileSubroutineCall(knownIdentifier: Token?) {
        var identifier = knownIdentifier?: nextIdentifier()
        val symbol = nextSymbol('.', '(')
        var name = identifier.getString()
        var methodCall = false
        if (symbol.symbol == '.') {  // Clazz.function(..) or Clazz.new(..) or obj.method(..)
            val clazz = if (symbolTable.contains(identifier.getString())) {
                val obj = symbolTable.getSymbol(identifier.getString())
                vmWriter.writePush(symbolKindToSegment(obj.kind), obj.index)  // push obj itself as first argument
                methodCall = true
                obj.type
            } else {
                identifier.getString()
            }
            identifier = nextIdentifier()
            nextSymbol('(')
            name = "$clazz.${identifier.getString()}"
        } else {
            vmWriter.writePush(POINTER, 0)
            name = "$className.$name"
            methodCall = true
        }
        val argumentsNumber = compileExpressionList() + if (methodCall) 1 else 0
        nextSymbol(')')
        vmWriter.writeCall(name, argumentsNumber)
    }

    private fun compileLet() {
        nextKeyword(LET)
        val name = nextIdentifier()
        checkAssignAllowed(name)
        val variable = symbolTable.getSymbol(name.getString())
        val symbol = nextSymbol('[', '=')
        var array = false
        if (symbol.symbol == '[') {
            compileExpression()
            nextSymbol(']')
            nextSymbol('=')
            array = true
            vmWriter.writePush(symbolKindToSegment(variable.kind), variable.index)
            vmWriter.writeArithmetic(ArithmeticCommand.ADD)  // array + index to find address
            // save array element address in temp variable in case THAT will be used in right part of expression
            vmWriter.writePop(TEMP, 1)
        }
        compileExpression()
        nextSymbol(';')
        if (array) {
            vmWriter.writePush(TEMP, 1)  // restore array element address
            vmWriter.writePop(POINTER, 1)  // make THAT to point to array element
            vmWriter.writePop(THAT, 0)
        } else {
            vmWriter.writePop(symbolKindToSegment(variable.kind), variable.index)
        }
    }

    private fun compileWhile() {
        val whileStartLabel = nextLabel()
        val whileEndLabel = nextLabel()
        vmWriter.writeLabel(whileStartLabel)
        nextKeyword(WHILE)
        nextSymbol('(')
        compileExpression()
        nextSymbol(')')
        vmWriter.writeArithmetic(ArithmeticCommand.NOT)  // negate while condition
        vmWriter.writeIf(whileEndLabel)
        nextSymbol('{')
        compileStatements()
        nextSymbol('}')
        vmWriter.writeGoto(whileStartLabel)
        vmWriter.writeLabel(whileEndLabel)
    }

    private fun compileReturn() {
        nextKeyword(RETURN)
        futureNext()
        if (!isSymbol(';')) {
            compileExpression()
        }
        next() // ';'
        if (inVoidSubroutine) {
            vmWriter.writePush(CONSTANT, 0)  // void returned as 0
        }
        vmWriter.writeReturn()
    }

    private fun compileIf() {
        nextKeyword(IF)
        nextSymbol('(')
        compileExpression()
        nextSymbol(')')
        var ifLabel = nextLabel()
        vmWriter.writeArithmetic(ArithmeticCommand.NOT)  // negate if condition
        vmWriter.writeIf(ifLabel)
        nextSymbol('{')
        compileStatements()
        nextSymbol('}')
        futureNext()
        if (isKeyword(ELSE)) {
            val elseLabel = ifLabel
            ifLabel = nextLabel()
            vmWriter.writeGoto(ifLabel)
            vmWriter.writeLabel(elseLabel)
            next() // move to the future 'else'
            nextSymbol('{')
            compileStatements()
            nextSymbol('}')
        }
        vmWriter.writeLabel(ifLabel)
    }

    private fun nextLabel(): String {
        return "$className.label${labelIndex++}"
    }

    private fun compileExpression() {
        compileTerm()
        futureNext()
        while (isSymbol('+', '-', '*', '/', '&', '|', '<', '>', '=')) {
            val symbol = (peek() as SymbolToken).symbol
            next()  // move to future op symbol
            compileTerm()
            when (symbol) {
                '*' -> vmWriter.writeCall("Math.multiply", 2)
                '/' -> vmWriter.writeCall("Math.divide", 2)
                else -> vmWriter.writeArithmetic(getArithmeticCommandBy(symbol))
            }
            futureNext()
        }
    }

    private fun compileTerm() {
        next()
        if (isInteger()) {
            vmWriter.writePush(CONSTANT, (peek() as IntConstantToken).value)
        } else if (isString()) {
            val str = (peek() as StringConstantToken).value
            vmWriter.writePush(CONSTANT, str.length)
            vmWriter.writeCall("String.new", 1)
            for (c in str) {
                vmWriter.writePush(CONSTANT, c.code)
                vmWriter.writeCall("String.appendChar", 2)
            }
        } else if (isKeyword(TRUE)) {
            // push -1
            vmWriter.writePush(CONSTANT, 1)
            vmWriter.writeArithmetic(ArithmeticCommand.NEG)
        } else if (isKeyword(FALSE, NULL)) {
            vmWriter.writePush(CONSTANT, 0)
        } else if (isKeyword(THIS)) {
            vmWriter.writePush(POINTER, 0)
        } else if (isSymbol('-', '~')) { // unary operation
            val unaryOperation = peek()
            compileTerm()
            if ((unaryOperation as SymbolToken).symbol == '-') {
                vmWriter.writeArithmetic(ArithmeticCommand.NEG)
            } else {
                vmWriter.writeArithmetic(ArithmeticCommand.NOT)
            }
        } else if (isSymbol('(')) {
            compileExpression()
            nextSymbol(')')
        } else if (isIdentifier()) {
            val name = peek()
            futureNext()
            if (isSymbol('.', '(')) { // subroutine call
                compileSubroutineCall(name)
            } else {  // variables
                checkVariableDefined(name)
                val variable = symbolTable.getSymbol(name.getString())
                vmWriter.writePush(symbolKindToSegment(variable.kind), variable.index)
                if (isSymbol('[')) {
                    checkType(name, "Array")
                    next()  // symbol '['
                    compileExpression()
                    nextSymbol(']')
                    vmWriter.writeArithmetic(ArithmeticCommand.ADD)  // arr + index
                    vmWriter.writePop(POINTER, 1)   // make THAT to point to array element
                    vmWriter.writePush(THAT, 0)
                }
            }
        } else {
            unexpectedTokenError(peek())
        }
    }

    private fun compileExpressionList(): Int {
        var listSize = 0
        futureNext()
        while (!isSymbol(')')) {
            compileExpression()
            futureNext()
            if (isSymbol(',')) {
                next()  // ','
            } else if (!isSymbol(')')) {
                unexpectedTokenError(peek())
            }
            listSize ++
        }
        return listSize
    }

    private fun getArithmeticCommandBy(symbol: Char) = when (symbol) {
        '+' -> ArithmeticCommand.ADD
        '-' -> ArithmeticCommand.SUB
        '&' -> ArithmeticCommand.AND
        '|' -> ArithmeticCommand.OR
        '<' -> ArithmeticCommand.LT
        '>' -> ArithmeticCommand.GT
        '=' -> ArithmeticCommand.EQ
        else -> throw IllegalStateException("Unexpected symbol $symbol")
    }

    private fun symbolKindToSegment(symbolKind: SymbolKind): Segment {
        return when (symbolKind) {
            SymbolKind.ARG -> ARGUMENT
            SymbolKind.VAR -> LOCAL
            SymbolKind.FIELD -> Segment.THIS
            SymbolKind.STATIC -> Segment.STATIC
            else -> throw IllegalStateException("Unable to convert symbol kind to segment")
        }
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

    // get and move to the next token
    private fun next(): Token {
        if (futureNext) {
            futureNext = false
            return currentToken!!
        }
        checkMoreTokens()
        currentToken = tokenizer.getNextToken()
        return currentToken!!
    }

    private fun peek() = currentToken!!

    // get next token but not move to it, i.e. next call of next() will move to the same token
    private fun futureNext(): Token {
        val futureToken = next()
        futureNext = true
        return futureToken
    }

    private fun checkMoreTokens() {
        if (!tokenizer.hasMoreTokens()) {
            throw CompilationError("Unexpected end of file")
        }
    }

    private fun checkVariableDefined(name: Token) {
        if (!symbolTable.contains(name.getString())) {
            throw CompilationError("Unknown variable: ${name.getString()}")
        }
    }

    private fun checkType(token: Token, type: String) {
        if (!symbolTable.contains(token.getString())) {
            throw CompilationError("Unknown symbol ${token.getString()}")
        }
        if (symbolTable.getSymbol(token.getString()).type != type) {
            throw CompilationError("${token.getString()} must be of $type type")
        }
    }

    private fun checkAssignAllowed(token: Token) {
        if (!symbolTable.contains(token.getString())) {
            throw CompilationError("Unknown symbol ${token.getString()}")
        }
    }

    private fun unexpectedTokenError(token: Token) {
        throw CompilationError("Unexpected token: ${token.getString()}")
    }

}