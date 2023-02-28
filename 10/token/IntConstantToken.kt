package nand2tetris.ch10.token

class IntConstantToken(private val value: Int): Token(TokenType.INT_CONST) {

    override fun getString(): String {
        return value.toString()
    }
}