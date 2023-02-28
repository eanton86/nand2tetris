package nand2tetris.ch08

import java.io.File

class VmCommandIterator(file: File) : AbstractIterator<String>() {

    private val linesIterator = file.bufferedReader().lineSequence().iterator()

    override fun computeNext() {
        if (!linesIterator.hasNext()) {
            done()
        } else {
            var str: String
            do {
                str = linesIterator.next()
                str = str.replaceAfter("//", "").replace("//", "").trim()
            } while (!isCommand(str) && linesIterator.hasNext())
            if (!isCommand(str)) {
                done()
            }
            setNext(str)
        }
    }

    fun isCommand(str: String): Boolean {
        return str.isNotBlank()
    }
}