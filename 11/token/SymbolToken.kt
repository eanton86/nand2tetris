package nand2tetris.ch11.token

import nand2tetris.ch11.token.TokenType

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