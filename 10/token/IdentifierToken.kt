package nand2tetris.ch10.token

class IdentifierToken(private val value: String): Token(TokenType.IDENTIFIER) {

    override fun getString(): String {
        return value
    }
}