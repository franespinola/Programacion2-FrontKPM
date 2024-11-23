package com.example.venta

import java.time.ZonedDateTime

data class Venta(
    val id: Int,
    val dispositivo: Dispositivo,
    val adicionales: List<Adicional>,
    val personalizaciones: List<Personalizacion>,
    val precioFinal: Double,
    val fechaVenta: ZonedDateTime
)
