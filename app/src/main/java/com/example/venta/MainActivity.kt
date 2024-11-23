package com.example.venta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.venta.ui.theme.VentaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VentaTheme {
                // Pantalla principal
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DispositivoListScreen()
                }
            }
        }
    }
}

@Composable
fun DispositivoListScreen() {
    val assembler = remember { DataAssembler(RetrofitClient.apiService) }
    val dispositivosWithDetails = remember { mutableStateOf<List<DispositivoWithDetails>>(emptyList()) }
    val selectedDispositivo = remember { mutableStateOf<DispositivoWithDetails?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val fetchedDispositivos = assembler.fetchCompleteDispositivos()
            dispositivosWithDetails.value = fetchedDispositivos
        } catch (e: Exception) {
            errorMessage.value = "Error al cargar los dispositivos: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    if (selectedDispositivo.value != null) {
        DispositivoDetailsScreen(
            dispositivoDetails = selectedDispositivo.value!!,
            onBack = { selectedDispositivo.value = null },
            onVentaExitosa = {
                println("Compra realizada con éxito")
                selectedDispositivo.value = null // Vuelve a la lista tras la compra
            },
            onError = { mensaje ->
                println("Error durante la compra: $mensaje")
            }
        )
    } else if (isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMessage.value != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage.value ?: "Error desconocido", color = MaterialTheme.colors.error)
        }
    } else {
        DispositivoList(dispositivosWithDetails.value) { dispositivo ->
            selectedDispositivo.value = dispositivo
        }
    }
}



@Composable
fun DispositivoList(
    dispositivos: List<DispositivoWithDetails>,
    onDispositivoSelected: (DispositivoWithDetails) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(dispositivos) { dispositivoDetails ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onDispositivoSelected(dispositivoDetails) },
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = dispositivoDetails.dispositivo.nombre, style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = dispositivoDetails.dispositivo.descripcion, style = MaterialTheme.typography.body2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Precio: ${dispositivoDetails.dispositivo.precioBase} ${dispositivoDetails.dispositivo.moneda}",
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}


@Composable
fun DispositivoDetailsScreen(
    dispositivoDetails: DispositivoWithDetails,
    onBack: () -> Unit,
    onVentaExitosa: () -> Unit, // Callback para manejar el éxito
    onError: (String) -> Unit // Callback para manejar errores
) {
    val selectedOpciones = remember { mutableStateMapOf<String, Opcion>() }
    val selectedAdicionales = remember { mutableStateMapOf<Int, Boolean>() }

    val totalPrice by remember {
        derivedStateOf {
            val personalizacionPrice = selectedOpciones.values.sumOf { it.precioAdicional }
            val adicionalPrice = selectedAdicionales.filterValues { it }.keys.sumOf { id ->
                val adicional = dispositivoDetails.adicionales.firstOrNull { it.id == id }
                if (adicional != null && adicional.precioGratis != -1.0 &&
                    dispositivoDetails.dispositivo.precioBase + personalizacionPrice >= adicional.precioGratis
                ) {
                    0.0 // Si entra en promoción, no suma el precio
                } else {
                    adicional?.precio ?: 0.0
                }
            }
            dispositivoDetails.dispositivo.precioBase + personalizacionPrice + adicionalPrice
        }
    }

    var isProcessingVenta by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { onBack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a la lista")
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(text = dispositivoDetails.dispositivo.nombre, style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dispositivoDetails.dispositivo.descripcion, style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(16.dp))

                // Características
                Text(text = "Características:", style = MaterialTheme.typography.subtitle1)
                dispositivoDetails.caracteristicas.forEach { caracteristica ->
                    Text(text = "- ${caracteristica.nombre}: ${caracteristica.descripcion}")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Personalizaciones
                Text(text = "Personalizaciones:", style = MaterialTheme.typography.subtitle1)
                dispositivoDetails.personalizaciones.forEach { personalizacion ->
                    PersonalizacionItem(personalizacion, selectedOpciones)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Adicionales
            item {
                AdicionalesSection(
                    adicionales = dispositivoDetails.adicionales,
                    selectedAdicionales = selectedAdicionales
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Precio Total: $${String.format("%.2f", totalPrice)}",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isProcessingVenta) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    isProcessingVenta = true
                    realizarVenta(
                        dispositivoDetails,
                        selectedOpciones,
                        selectedAdicionales,
                        totalPrice,
                        onVentaExitosa = {
                            isProcessingVenta = false
                            onVentaExitosa()
                        },
                        onError = { mensaje ->
                            isProcessingVenta = false
                            onError(mensaje)
                        }
                    )
                }, modifier = Modifier.weight(1f)) {
                    Text("Comprar")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { onBack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
            }
        }
    }
}

fun realizarVenta(
    dispositivoDetails: DispositivoWithDetails,
    selectedOpciones: Map<String, Opcion>,
    selectedAdicionales: Map<Int, Boolean>,
    totalPrice: Double,
    onVentaExitosa: () -> Unit,
    onError: (String) -> Unit
) {
    val apiService = RetrofitClient.apiService
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val fechaVenta = dateFormat.format(Date())

    val ventaRequest = VentaRequest(
        idDispositivo = dispositivoDetails.dispositivo.id,
        personalizaciones = dispositivoDetails.personalizaciones.map { personalizacion ->
            val opcionSeleccionada = selectedOpciones[personalizacion.nombre]
                ?: personalizacion.opciones.first() // Selecciona la primera opción si no hay una seleccionada
            PersonalizacionVenta(
                id = personalizacion.id, // ID de la personalización
                precio = opcionSeleccionada.precioAdicional,
                opcion = OpcionVenta(id = opcionSeleccionada.id) // ID de la opción seleccionada
            )
        },
        adicionales = selectedAdicionales.filterValues { it }.keys.map { id ->
            val adicional = dispositivoDetails.adicionales.first { it.id == id }
            AdicionalVenta(
                id = adicional.id,
                precio = adicional.precio
            )
        },
        precioFinal = totalPrice,
        fechaVenta = fechaVenta
    )


    kotlinx.coroutines.GlobalScope.launch {
        try {
            val response = apiService.realizarVenta(ventaRequest)
            // Manejo de respuesta directa
            withContext(Dispatchers.Main) {
                onVentaExitosa()
            }
        } catch (e: Exception) {
            // Manejo de errores
            withContext(Dispatchers.Main) {
                onError("Error al realizar la venta: ${e.message}")
            }
        }
    }
}



@Composable
fun PersonalizacionItem(
    personalizacion: Personalizacion,
    selectedOpciones: MutableMap<String, Opcion>
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = personalizacion.nombre, style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(4.dp))
        personalizacion.opciones.forEach { opcion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedOpciones[personalizacion.nombre] = opcion
                    }
                    .padding(8.dp)
            ) {
                RadioButton(
                    selected = selectedOpciones[personalizacion.nombre] == opcion,
                    onClick = {
                        selectedOpciones[personalizacion.nombre] = opcion
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${opcion.nombre} (+$${opcion.precioAdicional})")
            }
        }
    }
}



@Composable
fun AdicionalItem(adicional: Adicional, selectedAdicionales: MutableMap<Int, Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp) // Espaciado entre cada ítem
            .clickable {
                // Alternar la selección del adicional
                val currentSelection = selectedAdicionales[adicional.id] ?: false
                selectedAdicionales[adicional.id] = !currentSelection
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selectedAdicionales[adicional.id] ?: false,
            onCheckedChange = {
                selectedAdicionales[adicional.id] = it
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = adicional.nombre,
                style = MaterialTheme.typography.body1
            )
            Text(
                text = if (adicional.precioGratis > 0) {
                    "Precio: $${String.format("%.2f", adicional.precio)} (Promoción > $${adicional.precioGratis})"
                } else {
                    "Precio: $${String.format("%.2f", adicional.precio)} (Sin promoción)"
                },
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

@Composable
fun AdicionalesSection(
    adicionales: List<Adicional>,
    selectedAdicionales: MutableMap<Int, Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Espaciado para toda la sección
    ) {
        Text(text = "Adicionales:", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Limita la altura para que no ocupe toda la pantalla
        ) {
            items(adicionales) { adicional ->
                AdicionalItem(adicional, selectedAdicionales)
            }
        }
    }
}






