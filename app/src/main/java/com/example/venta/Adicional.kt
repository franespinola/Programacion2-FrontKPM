package com.example.venta

import com.google.gson.annotations.SerializedName

data class Adicional(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("precioGratis") val precioGratis: Double,
    @SerializedName("dispositivo") val dispositivo: Dispositivo
)

