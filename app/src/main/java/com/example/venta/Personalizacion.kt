package com.example.venta

import com.google.gson.annotations.SerializedName

data class Personalizacion(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("opciones") val opciones: List<Opcion> = emptyList(), // Relación agregada manualmente
    @SerializedName("dispositivo") val dispositivo: Dispositivo
)

