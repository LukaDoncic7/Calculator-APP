# Calculadora Pro para Android

Aplicación Android en Kotlin + Jetpack Compose con enfoque robusto, personalizable y lista para ampliar.

## Incluye

- Calculadora **básica/científica** con historial y funciones avanzadas.
- Conversor de **unidades** (longitud, masa, temperatura).
- Conversor de **monedas** con actualización manual y automática diaria (WorkManager).
- **Menú completo** tipo calculadora profesional:
  - Modo
  - Diseño (Bolsillo / Compacto / Expandido)
  - Tema (Sistema / Claro / Oscuro / Metálico / Océano)
  - Ajustes
  - Historial
  - Portapapeles
  - Ayuda
  - Acerca de
- **Onboarding inicial** en el primer arranque para explicar dónde está cada función.

## Arquitectura

```text
app/src/main/java/com/example/calculatorapp/
  MainActivity.kt                # UI principal + menú + diálogos
  data/
    CurrencyApi.kt               # API de tasas de cambio
    CurrencyRepository.kt        # cache local de divisas
  domain/
    ExpressionEvaluator.kt       # parser matemático
    Converters.kt                # conversor de unidades
  ui/
    AppTheme.kt                  # temas visuales
    CalculatorViewModel.kt       # estado global y acciones
  worker/
    CurrencySyncWorker.kt        # sync diaria de monedas
```

## Requisitos

- Android Studio Iguana o superior.
- JDK 17.
- SDK 34.
- Android 8.0+ (API 26).

## Ejecutar

1. Abrir el proyecto en Android Studio.
2. Sincronizar Gradle.
3. Ejecutar en emulador o dispositivo.

## Notas

- Las tasas de divisa se obtienen desde `open.er-api.com`.
- Si falla internet, se usa caché local cuando está disponible.
