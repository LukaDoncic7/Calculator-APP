package com.example.calculatorapp.domain

import kotlin.math.*

class ExpressionEvaluator {
    fun evaluate(raw: String): Double {
        val expression = raw.replace("×", "*").replace("÷", "/")
        val parser = Parser(expression)
        return parser.parse()
    }

    private class Parser(private val input: String) {
        private var index = 0

        fun parse(): Double {
            val value = parseExpression()
            skipSpaces()
            if (index < input.length) error("Expresión inválida")
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                value = when {
                    match('+') -> value + parseTerm()
                    match('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                skipSpaces()
                value = when {
                    match('*') -> value * parsePower()
                    match('/') -> value / parsePower()
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double {
            var value = parseUnary()
            skipSpaces()
            if (match('^')) value = value.pow(parsePower())
            return value
        }

        private fun parseUnary(): Double {
            skipSpaces()
            return when {
                match('+') -> parseUnary()
                match('-') -> -parseUnary()
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            if (match('(')) {
                val value = parseExpression()
                require(match(')')) { "Falta cierre de paréntesis" }
                return value
            }

            if (peek()?.isLetter() == true) {
                val name = readWhile { it.isLetter() }
                if (name.equals("pi", true)) return PI
                if (name.equals("e", true)) return E

                require(match('(')) { "Función inválida" }
                val arg = parseExpression()
                require(match(')')) { "Función sin cierre" }
                return applyFunction(name, arg)
            }

            val number = readWhile { it.isDigit() || it == '.' }
            require(number.isNotBlank()) { "Número esperado" }
            return number.toDouble()
        }

        private fun applyFunction(name: String, value: Double): Double = when (name.lowercase()) {
            "sin" -> sin(Math.toRadians(value))
            "cos" -> cos(Math.toRadians(value))
            "tan" -> tan(Math.toRadians(value))
            "log" -> log10(value)
            "ln" -> ln(value)
            "sqrt" -> sqrt(value)
            "abs" -> abs(value)
            else -> error("Función no soportada: $name")
        }

        private fun readWhile(predicate: (Char) -> Boolean): String {
            val start = index
            while (peek()?.let(predicate) == true) index++
            return input.substring(start, index)
        }

        private fun skipSpaces() {
            while (peek() == ' ') index++
        }

        private fun match(ch: Char): Boolean {
            if (peek() == ch) {
                index++
                return true
            }
            return false
        }

        private fun peek(): Char? = input.getOrNull(index)
    }
}
