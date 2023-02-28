package nand2tetris.ch11.token

class IntConstantToken(val value: Int): Token(TokenType.INT_CONST) {

    override fun getString(): String {
        return value.toString()
    }
}