package nand2tetris.ch10.token

abstract class Token(val tokenType: TokenType) {

    abstract fun getString(): String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (tokenType != other.tokenType) return false

        return getString() == getString()
    }

    override fun hashCode(): Int {
        return (tokenType.toString() + getString()).hashCode()
    }


}


