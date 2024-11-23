package com.example.venta


data class DispositivoWithDetails(
    val dispositivo: Dispositivo,
    val personalizaciones: List<Personalizacion>,
    val adicionales: List<Adicional>,
    val caracteristicas: List<Caracteristica>
)




