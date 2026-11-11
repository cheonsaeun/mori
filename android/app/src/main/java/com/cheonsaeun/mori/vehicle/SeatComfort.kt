package com.cheonsaeun.mori.vehicle

import com.cheonsaeun.mori.model.Signal

/** Pleos emulator combines heating (+) and ventilation (-) on signed seat-temperature areas. */
internal fun normalizeSeatComfort(temperature: Signal<Int>, ventilation: Signal<Int>, combined: Boolean): Pair<Signal<Int>,Signal<Int>> {
    val value=temperature.value ?: return temperature to ventilation
    if(!combined && value>=0) return temperature to ventilation
    return temperature.copy(value=maxOf(value,0)) to Signal(maxOf(-value,0),temperature.availability)
}
