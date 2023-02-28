package nand2tetris.ch11.token

class StringConstantToken(val value: String): Token(TokenType.STRING_CONST) {

    override fun getString(): String {
        return value
    }
}