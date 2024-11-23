package com.example.venta

import com.google.gson.annotations.SerializedName

data class Opcion(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("precioAdicional") val precioAdicional: Double,
    @SerializedName("personalizacion") val personalizacion: Personalizacion? = null
)
