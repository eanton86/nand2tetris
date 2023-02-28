package nand2tetris.ch10.token

class SymbolToken(val symbol: Char): Token(TokenType.SYMBOL) {

    override fun getString(): String {
        return when (symbol) {
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '&' -> "&amp;"
            else -> symbol.toString()
        }
    }
}