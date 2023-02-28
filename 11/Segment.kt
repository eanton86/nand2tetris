package nand2tetris.ch11

enum class Segment {
    CONSTANT,
    LOCAL,
    ARGUMENT,
    STATIC,
    THIS,
    THAT,
    POINTER,
    TEMP;

    companion object {

        fun getValueOf(str: String): Segment {
            return Segment.valueOf(str.uppercase())
        }
    }
}