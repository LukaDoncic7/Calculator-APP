package com.example.calculatorapp.domain

enum class UnitCategory { LENGTH, MASS, TEMPERATURE }

data class UnitDefinition(
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

object UnitConverters {
    val categories: Map<UnitCategory, List<UnitDefinition>> = mapOf(
        UnitCategory.LENGTH to listOf(
            UnitDefinition("m", { it }, { it }),
            UnitDefinition("km", { it * 1_000 }, { it / 1_000 }),
            UnitDefinition("cm", { it / 100 }, { it * 100 }),
            UnitDefinition("in", { it * 0.0254 }, { it / 0.0254 }),
            UnitDefinition("ft", { it * 0.3048 }, { it / 0.3048 })
        ),
        UnitCategory.MASS to listOf(
            UnitDefinition("kg", { it }, { it }),
            UnitDefinition("g", { it / 1000 }, { it * 1000 }),
            UnitDefinition("lb", { it * 0.45359237 }, { it / 0.45359237 }),
            UnitDefinition("oz", { it * 0.0283495 }, { it / 0.0283495 })
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitDefinition("°C", { it }, { it }),
            UnitDefinition("°F", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
            UnitDefinition("K", { it - 273.15 }, { it + 273.15 })
        )
    )

    fun convert(value: Double, from: UnitDefinition, to: UnitDefinition): Double {
        val base = from.toBase(value)
        return to.fromBase(base)
    }
}
