package com.example.venta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DataAssembler(private val apiService: ApiService) {

    suspend fun fetchCompleteDispositivos(): List<DispositivoWithDetails> = withContext(Dispatchers.IO) {
        val dispositivos = apiService.getDispositivos() // Lista de dispositivos
        val personalizaciones = apiService.getPersonalizaciones() // Lista de personalizaciones
        val opciones = apiService.getOpciones() // Lista de opciones para personalizaciones
        val adicionales = apiService.getAdicionales() // Lista de adicionales
        val caracteristicas = apiService.getCaracteristicas() // Lista de características

        println("Dispositivos recibidos: ${dispositivos.size}")
        println("Personalizaciones recibidas: ${personalizaciones.size}")
        println("Adicionales recibidos: ${adicionales.size}")
        println("Adicionales Detalles: $adicionales")


        dispositivos.map { dispositivo ->
            // Filtrar personalizaciones para este dispositivo
            val dispositivoPersonalizaciones = personalizaciones
                .filter { it.dispositivo.id == dispositivo.id }
                .map { personalizacion ->
                    // Agregar opciones a cada personalización
                    personalizacion.copy(
                        opciones = opciones.filter { it.personalizacion?.id == personalizacion.id }
                    )
                }

            // Filtrar adicionales y características
            val dispositivoAdicionales = adicionales.filter { it.dispositivo.id == dispositivo.id }
            val dispositivoCaracteristicas = caracteristicas.filter { it.dispositivo.id == dispositivo.id }

            // Crear el objeto `DispositivoWithDetails`
            DispositivoWithDetails(
                dispositivo = dispositivo,
                personalizaciones = dispositivoPersonalizaciones,
                adicionales = dispositivoAdicionales,
                caracteristicas = dispositivoCaracteristicas
            )
        }
    }
}







