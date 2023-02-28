package nand2tetris.ch11.token

class KeywordToken(val keyword: Keyword): Token(TokenType.KEYWORD) {

    override fun getString(): String {
        return keyword.toString().lowercase()
    }
}