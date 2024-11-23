package com.example.venta

import com.google.gson.annotations.SerializedName

data class VentaRequest(
    @SerializedName("idDispositivo") val idDispositivo: Int,
    @SerializedName("personalizaciones") val personalizaciones: List<PersonalizacionVenta>,
    @SerializedName("adicionales") val adicionales: List<AdicionalVenta>,
    @SerializedName("precioFinal") val precioFinal: Double,
    @SerializedName("fechaVenta") val fechaVenta: String
)

data class PersonalizacionVenta(
    @SerializedName("id") val id: Int,
    @SerializedName("precio") val precio: Double,
    @SerializedName("opcion") val opcion: OpcionVenta
)

data class OpcionVenta(
    @SerializedName("id") val id: Int
)

data class AdicionalVenta(
    @SerializedName("id") val id: Int,
    @SerializedName("precio") val precio: Double
)

data class VentaResponse( // Para manejar la respuesta del backend
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)
