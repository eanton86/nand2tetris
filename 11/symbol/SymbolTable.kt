package nand2tetris.ch11.symbol

import nand2tetris.ch11.symbol.SymbolKind.*

class SymbolTable {
    private val classSymbols = mutableMapOf<String, Symbol>()
    private val subroutineSymbols = mutableMapOf<String, Symbol>()

    public fun startSubroutine() {
        subroutineSymbols.clear()
    }

    public fun define(name: String, type: String, kind: SymbolKind) {
        validateKind(kind)
        val symbol = Symbol(name, type, kind, varCount(kind))
        if (kind == FIELD || kind == STATIC) {
            classSymbols[name] = symbol
        } else {
            subroutineSymbols[name] = symbol
        }
    }

    public fun getSymbol(name: String): Symbol {
        if (subroutineSymbols.contains(name)) {
            return subroutineSymbols[name]!!
        }
        if (classSymbols.contains(name)) {
            return classSymbols[name]!!
        }
        throw IllegalArgumentException("Symbol not found")
    }

    public fun contains(name: String): Boolean {
        if (subroutineSymbols.contains(name)) {
            return true
        }
        if (classSymbols.contains(name)) {
            return true
        }
        return false
     }

    public fun varCount(kind: SymbolKind): Int {
        validateKind(kind)
        if (kind == FIELD || kind == STATIC) {
            return classSymbols.values.count { it.kind == kind }
        }
        return subroutineSymbols.values.count { it.kind == kind }
    }

    private fun validateKind(kind: SymbolKind) {
        if (kind == NONE) {
            throw IllegalArgumentException("Symbol must not be NONE")
        }
    }


}