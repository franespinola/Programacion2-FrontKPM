package com.example.venta

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/dispositivos/traerDispositivos")
    suspend fun getDispositivos(): List<Dispositivo>

    @GET("api/personalizacions")
    suspend fun getPersonalizaciones(): List<Personalizacion>

    @GET("api/adicionals")
    suspend fun getAdicionales(): List<Adicional>

    @GET("api/caracteristicas")
    suspend fun getCaracteristicas(): List<Caracteristica>

    @GET("api/opcions")
    suspend fun getOpciones(): List<Opcion>

    @POST("api/ventas/vender")
    suspend fun realizarVenta(@Body ventaRequest: VentaRequest): Response<VentaResponse>

}


