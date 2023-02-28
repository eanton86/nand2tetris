package nand2tetris.ch08

enum class CommandType {
    ARITHMETIC,
    PUSH, POP,
    LABEL,
    GOTO, IF,
    FUNCTION,
    RETURN,
    CALL
}