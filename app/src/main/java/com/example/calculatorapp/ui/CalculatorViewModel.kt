package com.example.calculatorapp.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.calculatorapp.data.CurrencyRepository
import com.example.calculatorapp.data.CurrencyState
import com.example.calculatorapp.domain.ExpressionEvaluator
import com.example.calculatorapp.domain.UnitCategory
import com.example.calculatorapp.domain.UnitConverters
import com.example.calculatorapp.domain.UnitDefinition
import com.example.calculatorapp.worker.CurrencySyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class AppSection { CALCULATOR, UNITS, CURRENCY }
enum class CalculatorMode { BASIC, SCIENTIFIC }
enum class LayoutMode { POCKET, COMPACT, EXPANDED }
enum class ThemePreset { SYSTEM, LIGHT, DARK, METALLIC, OCEAN }
enum class AngleMode { DEG, RAD }

data class AppSettings(
    val precision: Int = 6,
    val useThousandsSeparator: Boolean = true,
    val hapticFeedback: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG
)

data class TutorialPage(val title: String, val description: String)

data class CalculatorUiState(
    val mode: CalculatorMode = CalculatorMode.BASIC,
    val expression: String = "",
    val result: String = "0",
    val history: List<String> = emptyList(),
    val clipboard: List<String> = emptyList(),
    val section: AppSection = AppSection.CALCULATOR,
    val layoutMode: LayoutMode = LayoutMode.COMPACT,
    val themePreset: ThemePreset = ThemePreset.SYSTEM,
    val settings: AppSettings = AppSettings(),
    val showOnboarding: Boolean = true,
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val fromUnit: UnitDefinition = UnitConverters.categories[UnitCategory.LENGTH]!!.first(),
    val toUnit: UnitDefinition = UnitConverters.categories[UnitCategory.LENGTH]!![1],
    val unitInput: String = "1",
    val unitOutput: String = "1000",
    val currencyState: CurrencyState = CurrencyState(),
    val fromCurrency: String = "USD",
    val toCurrency: String = "EUR",
    val currencyAmount: String = "1",
    val currencyConverted: String = "0"
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val evaluator = ExpressionEvaluator()
    private val repository = CurrencyRepository(application)
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        CalculatorUiState(
            currencyState = repository.loadCachedRates(),
            mode = enumPref("mode", CalculatorMode.BASIC),
            layoutMode = enumPref("layout", LayoutMode.COMPACT),
            themePreset = enumPref("theme", ThemePreset.SYSTEM),
            settings = AppSettings(
                precision = prefs.getInt("precision", 6),
                useThousandsSeparator = prefs.getBoolean("thousands", true),
                hapticFeedback = prefs.getBoolean("haptic", false),
                angleMode = enumPref("angle", AngleMode.DEG)
            ),
            showOnboarding = !prefs.getBoolean("onboarding_seen", false)
        )
    )
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    val tutorialPages = listOf(
        TutorialPage("Bienvenido", "Esta app incluye calculadora, conversor de unidades y de monedas."),
        TutorialPage("Modo", "Desde el menú puedes cambiar entre calculadora básica y científica."),
        TutorialPage("Diseño y tema", "Ajusta tamaño (bolsillo/compacto/expandido) y tema visual."),
        TutorialPage("Monedas", "El conversor de divisas se actualiza automáticamente una vez al día."),
        TutorialPage("Más opciones", "En menú encontrarás ajustes, historial, portapapeles, ayuda y acerca de.")
    )

    init {
        scheduleDailyRatesSync()
        refreshRates()
        recalculateUnits()
    }

    fun setSection(section: AppSection) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    fun setMode(mode: CalculatorMode) {
        prefs.edit().putString("mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun setLayout(layoutMode: LayoutMode) {
        prefs.edit().putString("layout", layoutMode.name).apply()
        _uiState.value = _uiState.value.copy(layoutMode = layoutMode)
    }

    fun setTheme(themePreset: ThemePreset) {
        prefs.edit().putString("theme", themePreset.name).apply()
        _uiState.value = _uiState.value.copy(themePreset = themePreset)
    }

    fun setPrecision(precision: Int) {
        val bounded = precision.coerceIn(2, 12)
        prefs.edit().putInt("precision", bounded).apply()
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(precision = bounded))
        recalculateUnits()
        recalculateCurrency()
    }

    fun toggleThousands(enabled: Boolean) {
        prefs.edit().putBoolean("thousands", enabled).apply()
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(useThousandsSeparator = enabled))
        recalculateUnits()
        recalculateCurrency()
    }

    fun toggleHaptic(enabled: Boolean) {
        prefs.edit().putBoolean("haptic", enabled).apply()
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(hapticFeedback = enabled))
    }

    fun setAngleMode(mode: AngleMode) {
        prefs.edit().putString("angle", mode.name).apply()
        _uiState.value = _uiState.value.copy(settings = _uiState.value.settings.copy(angleMode = mode))
    }

    fun appendInput(token: String) {
        _uiState.value = _uiState.value.copy(expression = _uiState.value.expression + token)
    }

    fun clearExpression() {
        _uiState.value = _uiState.value.copy(expression = "", result = "0")
    }

    fun backspace() {
        _uiState.value = _uiState.value.copy(expression = _uiState.value.expression.dropLast(1))
    }

    fun evaluateExpression() {
        val expression = _uiState.value.expression
        if (expression.isBlank()) return
        val computedValue = runCatching { evaluator.evaluate(expression) }.getOrNull()
        val computed = computedValue?.toSmartString() ?: "Error"

        val updatedHistory = listOf("$expression = $computed") + _uiState.value.history
        _uiState.value = _uiState.value.copy(result = computed, history = updatedHistory.take(30))
        addToClipboard("$expression = $computed")
    }

    fun addToClipboard(value: String) {
        val updated = (listOf(value) + _uiState.value.clipboard).distinct().take(50)
        _uiState.value = _uiState.value.copy(clipboard = updated)
    }

    fun clearClipboard() {
        _uiState.value = _uiState.value.copy(clipboard = emptyList())
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(history = emptyList())
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_seen", true).apply()
        _uiState.value = _uiState.value.copy(showOnboarding = false)
    }

    fun selectUnitCategory(category: UnitCategory) {
        val units = UnitConverters.categories.getValue(category)
        _uiState.value = _uiState.value.copy(selectedCategory = category, fromUnit = units.first(), toUnit = units.last())
        recalculateUnits()
    }

    fun setUnitInput(value: String) {
        _uiState.value = _uiState.value.copy(unitInput = value)
        recalculateUnits()
    }

    fun swapUnits() {
        _uiState.value = _uiState.value.copy(fromUnit = _uiState.value.toUnit, toUnit = _uiState.value.fromUnit)
        recalculateUnits()
    }

    fun selectFromUnit(unit: UnitDefinition) {
        _uiState.value = _uiState.value.copy(fromUnit = unit)
        recalculateUnits()
    }

    fun selectToUnit(unit: UnitDefinition) {
        _uiState.value = _uiState.value.copy(toUnit = unit)
        recalculateUnits()
    }

    private fun recalculateUnits() {
        val value = _uiState.value.unitInput.toDoubleOrNull() ?: 0.0
        val output = UnitConverters.convert(value, _uiState.value.fromUnit, _uiState.value.toUnit)
        _uiState.value = _uiState.value.copy(unitOutput = output.toSmartString())
    }

    fun setCurrencyAmount(amount: String) {
        _uiState.value = _uiState.value.copy(currencyAmount = amount)
        recalculateCurrency()
    }

    fun selectFromCurrency(code: String) {
        _uiState.value = _uiState.value.copy(fromCurrency = code)
        if (_uiState.value.currencyState.base != code) refreshRates(code) else recalculateCurrency()
    }

    fun selectToCurrency(code: String) {
        _uiState.value = _uiState.value.copy(toCurrency = code)
        recalculateCurrency()
    }

    fun refreshRates(base: String = _uiState.value.fromCurrency) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = runCatching { repository.fetchAndPersist(base) }
                .getOrElse { repository.loadCachedRates().copy(error = it.message ?: "Sin conexión") }
            _uiState.value = _uiState.value.copy(currencyState = state, fromCurrency = state.base)
            recalculateCurrency()
        }
    }

    private fun recalculateCurrency() {
        val amount = _uiState.value.currencyAmount.toDoubleOrNull() ?: 0.0
        val rates = _uiState.value.currencyState.rates
        val base = _uiState.value.currencyState.base
        val fromRate = if (_uiState.value.fromCurrency == base) 1.0 else rates[_uiState.value.fromCurrency]
        val toRate = if (_uiState.value.toCurrency == base) 1.0 else rates[_uiState.value.toCurrency]
        val converted = if (fromRate == null || toRate == null || fromRate == 0.0) 0.0 else (amount / fromRate) * toRate
        _uiState.value = _uiState.value.copy(currencyConverted = converted.toSmartString())
    }

    fun lastUpdateText(): String {
        val updated = _uiState.value.currencyState.updatedAtMillis
        if (updated <= 0) return "Nunca"
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(Date(updated))
    }

    private fun scheduleDailyRatesSync() {
        val work = PeriodicWorkRequestBuilder<CurrencySyncWorker>(24, TimeUnit.HOURS)
            .setInputData(workDataOf("base" to "USD"))
            .build()
        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork("currency_daily_sync", ExistingPeriodicWorkPolicy.KEEP, work)
    }


    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T {
        val value = prefs.getString(key, default.name) ?: default.name
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }

    private fun Double.toSmartString(): String {
        val settings = _uiState.value.settings
        val pattern = buildString {
            append(if (settings.useThousandsSeparator) "#,##0" else "0")
            if (settings.precision > 0) append(".").append("#".repeat(settings.precision))
        }
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        return DecimalFormat(pattern, symbols).format(this)
    }
}
