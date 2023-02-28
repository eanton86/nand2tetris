package nand2tetris.ch10.token

class StringConstantToken(private val value: String): Token(TokenType.STRING_CONST) {

    override fun getString(): String {
        return value
    }
}