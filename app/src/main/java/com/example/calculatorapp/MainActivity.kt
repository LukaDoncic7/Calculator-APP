package com.example.calculatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calculatorapp.domain.UnitCategory
import com.example.calculatorapp.domain.UnitConverters
import com.example.calculatorapp.domain.UnitDefinition
import com.example.calculatorapp.ui.AngleMode
import com.example.calculatorapp.ui.AppTheme
import com.example.calculatorapp.ui.AppSection
import com.example.calculatorapp.ui.CalculatorMode
import com.example.calculatorapp.ui.CalculatorUiState
import com.example.calculatorapp.ui.CalculatorViewModel
import com.example.calculatorapp.ui.LayoutMode
import com.example.calculatorapp.ui.ThemePreset

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CalculatorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsState()
            AppTheme(state.themePreset) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent(state = state, onAction = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppContent(state: CalculatorUiState, onAction: CalculatorViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora Pro") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) { Text("⋮", style = MaterialTheme.typography.headlineSmall) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Modo") }, onClick = { dialog = "mode"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Diseño") }, onClick = { dialog = "layout"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Tema") }, onClick = { dialog = "theme"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Ajustes") }, onClick = { dialog = "settings"; menuExpanded = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Historial") }, onClick = { dialog = "history"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Portapapeles") }, onClick = { dialog = "clipboard"; menuExpanded = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Ayuda") }, onClick = { dialog = "help"; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Acerca de") }, onClick = { dialog = "about"; menuExpanded = false })
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = state.section == section,
                        onClick = { onAction.setSection(section) },
                        label = { Text(section.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        icon = { Text(if (state.section == section) "●" else "○") }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (state.layoutMode == LayoutMode.POCKET) 8.dp else if (state.layoutMode == LayoutMode.COMPACT) 12.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.section) {
                AppSection.CALCULATOR -> CalculatorSection(state, onAction)
                AppSection.UNITS -> UnitConverterSection(state, onAction)
                AppSection.CURRENCY -> CurrencySection(state, onAction)
            }
        }

        when (dialog) {
            "mode" -> ModeDialog(state, onAction) { dialog = "" }
            "layout" -> LayoutDialog(state, onAction) { dialog = "" }
            "theme" -> ThemeDialog(state, onAction) { dialog = "" }
            "settings" -> SettingsDialog(state, onAction) { dialog = "" }
            "history" -> HistoryDialog(state, onAction) {
                clipboard.setText(AnnotatedString(it))
                onAction.addToClipboard(it)
            } { dialog = "" }
            "clipboard" -> ClipboardDialog(state, onAction) { clipboard.setText(AnnotatedString(it)) } { dialog = "" }
            "help" -> HelpDialog { dialog = "" }
            "about" -> AboutDialog { dialog = "" }
        }

        if (state.showOnboarding) {
            OnboardingDialog(onAction)
        }
    }
}

@Composable
private fun ModeDialog(state: CalculatorUiState, vm: CalculatorViewModel, onClose: () -> Unit) =
    SelectionDialog("Modo", CalculatorMode.entries, state.mode, { vm.setMode(it) }, { it.name }) { onClose() }

@Composable
private fun LayoutDialog(state: CalculatorUiState, vm: CalculatorViewModel, onClose: () -> Unit) =
    SelectionDialog("Diseño", LayoutMode.entries, state.layoutMode, { vm.setLayout(it) }, {
        when (it) {
            LayoutMode.POCKET -> "Bolsillo"
            LayoutMode.COMPACT -> "Compacto"
            LayoutMode.EXPANDED -> "Expandido"
        }
    }) { onClose() }

@Composable
private fun ThemeDialog(state: CalculatorUiState, vm: CalculatorViewModel, onClose: () -> Unit) =
    SelectionDialog("Tema", ThemePreset.entries, state.themePreset, { vm.setTheme(it) }, {
        when (it) {
            ThemePreset.SYSTEM -> "Sistema"
            ThemePreset.LIGHT -> "Claro"
            ThemePreset.DARK -> "Oscuro"
            ThemePreset.METALLIC -> "Metálico"
            ThemePreset.OCEAN -> "Océano"
        }
    }) { onClose() }

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach {
                    TextButton(onClick = { onSelect(it); onClose() }) {
                        Text(if (it == selected) "✓ ${label(it)}" else label(it))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } }
    )
}

@Composable
private fun SettingsDialog(state: CalculatorUiState, vm: CalculatorViewModel, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Ajustes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Precisión: ${state.settings.precision}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setPrecision(state.settings.precision - 1) }) { Text("-") }
                    Button(onClick = { vm.setPrecision(state.settings.precision + 1) }) { Text("+") }
                }
                SettingSwitch("Separador de miles", state.settings.useThousandsSeparator, vm::toggleThousands)
                SettingSwitch("Respuesta táctil", state.settings.hapticFeedback, vm::toggleHaptic)
                Text("Ángulo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setAngleMode(AngleMode.DEG) }) { Text("DEG") }
                    Button(onClick = { vm.setAngleMode(AngleMode.RAD) }) { Text("RAD") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Guardar") } }
    )
}

@Composable
private fun SettingSwitch(title: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title)
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun HistoryDialog(state: CalculatorUiState, vm: CalculatorViewModel, onCopy: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Historial") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.history) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onCopy(it) }) { Text("Copiar") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::clearHistory) { Text("Limpiar") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cerrar") } }
    )
}

@Composable
private fun ClipboardDialog(state: CalculatorUiState, vm: CalculatorViewModel, onPasteToSystem: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Portapapeles") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.clipboard) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onPasteToSystem(it) }) { Text("Copiar") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::clearClipboard) { Text("Vaciar") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cerrar") } }
    )
}

@Composable
private fun HelpDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Ayuda") },
        text = {
            Text("• Menú (⋮): cambia modo, diseño, tema y ajustes.\n• Barra inferior: Calculadora, Unidades y Monedas.\n• Monedas: usa 'Actualizar' para refrescar manualmente tasas.")
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Entendido") } }
    )
}

@Composable
private fun AboutDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Acerca de") },
        text = { Text("Calculadora Pro v1.1\nCreada con Kotlin + Compose\nIncluye calculadora científica, unidades y divisas.") },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } }
    )
}

@Composable
private fun OnboardingDialog(vm: CalculatorViewModel) {
    val pages = vm.tutorialPages
    var index by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(pages[index].title) },
        text = { Text(pages[index].description) },
        confirmButton = {
            TextButton(onClick = {
                if (index == pages.lastIndex) vm.completeOnboarding() else index++
            }) { Text(if (index == pages.lastIndex) "Comenzar" else "Siguiente") }
        },
        dismissButton = {
            TextButton(onClick = { if (index > 0) index-- else vm.completeOnboarding() }) {
                Text(if (index > 0) "Anterior" else "Saltar")
            }
        }
    )
}

@Composable
private fun CalculatorSection(state: CalculatorUiState, vm: CalculatorViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Modo ${if (state.mode == CalculatorMode.SCIENTIFIC) "científico" else "básico"}", fontWeight = FontWeight.Bold)
            Text(state.expression.ifBlank { "0" }, style = MaterialTheme.typography.headlineMedium)
            Text("= ${state.result}", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.setMode(if (state.mode == CalculatorMode.BASIC) CalculatorMode.SCIENTIFIC else CalculatorMode.BASIC) }) {
                    Text(if (state.mode == CalculatorMode.SCIENTIFIC) "Básica" else "Científica")
                }
                TextButton(onClick = vm::clearExpression) { Text("AC") }
                TextButton(onClick = vm::backspace) { Text("⌫") }
            }
        }
    }

    Keypad(state) { vm.appendInput(it) }
    Button(onClick = vm::evaluateExpression, modifier = Modifier.fillMaxWidth()) { Text("Calcular") }
}

@Composable
private fun Keypad(state: CalculatorUiState, onClick: (String) -> Unit) {
    val scientific = state.mode == CalculatorMode.SCIENTIFIC
    val basicRows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "(", ")", "+")
    )
    val scientificRows = listOf(
        listOf("sin(", "cos(", "tan(", if (state.settings.angleMode == AngleMode.DEG) "pi" else "e"),
        listOf("ln(", "sqrt(", "abs(", "^")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (scientific) {
            scientificRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key -> Button(onClick = { onClick(key) }, modifier = Modifier.weight(1f)) { Text(key) } }
                }
            }
        }
        basicRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key -> Button(onClick = { onClick(key) }, modifier = Modifier.weight(1f)) { Text(key) } }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitConverterSection(state: CalculatorUiState, vm: CalculatorViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conversor de unidades", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnitCategory.entries.forEach { category ->
                    TextButton(onClick = { vm.selectUnitCategory(category) }) { Text(category.name) }
                }
            }
            OutlinedTextField(value = state.unitInput, onValueChange = vm::setUnitInput, label = { Text("Cantidad") }, modifier = Modifier.fillMaxWidth())
            UnitDropdown("Desde", UnitConverters.categories.getValue(state.selectedCategory), state.fromUnit, vm::selectFromUnit)
            UnitDropdown("Hacia", UnitConverters.categories.getValue(state.selectedCategory), state.toUnit, vm::selectToUnit)
            Button(onClick = vm::swapUnits, modifier = Modifier.fillMaxWidth()) { Text("Intercambiar") }
            Text("Resultado: ${state.unitOutput}")
        }
    }
}

@Composable
private fun CurrencySection(state: CalculatorUiState, vm: CalculatorViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conversor de monedas", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = state.currencyAmount, onValueChange = vm::setCurrencyAmount, modifier = Modifier.fillMaxWidth(), label = { Text("Monto") })
            val codes = state.currencyState.rates.keys.sorted().ifEmpty { listOf("USD", "EUR", "MXN") }
            CurrencyDropdown("Desde", codes, state.fromCurrency, vm::selectFromCurrency)
            CurrencyDropdown("Hacia", codes, state.toCurrency, vm::selectToCurrency)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { vm.refreshRates(state.fromCurrency) }) { Text("Actualizar") }
                Text("Última sync: ${vm.lastUpdateText()}")
            }
            state.currencyState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            Text("Resultado: ${state.currencyConverted} ${state.toCurrency}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(label: String, options: List<UnitDefinition>, selected: UnitDefinition, onSelect: (UnitDefinition) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = selected.symbol,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.symbol) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
