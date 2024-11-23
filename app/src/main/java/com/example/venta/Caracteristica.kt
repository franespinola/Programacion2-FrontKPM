package com.example.venta

import com.google.gson.annotations.SerializedName

data class Caracteristica(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("dispositivo") val dispositivo: Dispositivo
)
