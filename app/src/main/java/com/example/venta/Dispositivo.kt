package com.example.venta

import com.google.gson.annotations.SerializedName

data class Dispositivo(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("precioBase") val precioBase: Double,
    @SerializedName("moneda") val moneda: String
)
