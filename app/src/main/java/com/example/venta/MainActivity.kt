package com.example.venta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
                selectedDispositivo.value = null
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
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        items(dispositivos) { dispositivoDetails ->
            DispositivoListItem(
                dispositivo = dispositivoDetails.dispositivo,
                onClick = { onDispositivoSelected(dispositivoDetails) }
            )
        }
    }
}
@Composable
fun DispositivoListItem(
    dispositivo: Dispositivo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = 6.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(100.dp)
        ) {
            // Imagen o ícono del dispositivo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // Si tienes una URL de imagen, puedes usar Coil para cargarla
                // Aquí usamos un ícono de marcador de posición
                Icon(
                    imageVector = Icons.Default.DeviceUnknown,
                    contentDescription = "Imagen del dispositivo",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(40.dp)
                )
                // Para agregar imagen,  usar Image con painterResource
                // Image(
                //     painter = painterResource(id = R.drawable.placeholder),
                //     contentDescription = "Imagen del dispositivo",
                //     contentScale = ContentScale.Crop,
                //     modifier = Modifier.fillMaxSize()
                // )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Detalles del dispositivo
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dispositivo.nombre,
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = dispositivo.descripcion,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Precio: ${dispositivo.precioBase} ${dispositivo.moneda}",
                    style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colors.primary
                )
            }

            // Flecha de navegación
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Ir a detalles",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun PurchaseDetailsScreen(
    dispositivoDetails: DispositivoWithDetails,
    selectedOpciones: Map<String, Opcion>,
    selectedAdicionales: Map<Int, Boolean>,
    totalPrice: Double,
    onDismiss: () -> Unit
) {
    // Obtener la lista de adicionales seleccionados
    val adicionalesSeleccionados = dispositivoDetails.adicionales.filter { adicional ->
        selectedAdicionales[adicional.id] == true
    }

    // Calcular los precios de los adicionales, considerando promociones
    val adicionalesConPrecio = adicionalesSeleccionados.map { adicional ->
        val precioAdicional = if (adicional.precioGratis != -1.0 &&
            dispositivoDetails.dispositivo.precioBase + selectedOpciones.values.sumOf { it.precioAdicional } >= adicional.precioGratis
        ) {
            0.0
        } else {
            adicional.precio
        }
        Pair(adicional.nombre, precioAdicional)
    }

    // Layout principal
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Título
            Text(
                text = "¡Compra Exitosa!",
                style = MaterialTheme.typography.h4.copy(color = MaterialTheme.colors.primary),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Icono de éxito
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Compra Exitosa",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            )

            // Detalles del Dispositivo
            Card(
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = dispositivoDetails.dispositivo.nombre,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dispositivoDetails.dispositivo.descripcion,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Precio Base: ${dispositivoDetails.dispositivo.precioBase} ${dispositivoDetails.dispositivo.moneda}",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Personalizaciones
            Card(
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Personalizaciones",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedOpciones.forEach { (nombrePersonalizacion, opcion) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = nombrePersonalizacion,
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface
                            )
                            Text(
                                text = "${opcion.nombre} (+${opcion.precioAdicional})",
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Adicionales
            Card(
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Adicionales",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (adicionalesConPrecio.isNotEmpty()) {
                        adicionalesConPrecio.forEach { (nombreAdicional, precio) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = nombreAdicional,
                                    style = MaterialTheme.typography.body1,
                                    color = MaterialTheme.colors.onSurface
                                )
                                Text(
                                    text = if (precio == 0.0) "Gratis (Promoción)" else "+$${String.format("%.2f", precio)}",
                                    style = MaterialTheme.typography.body1.copy(
                                        color = if (precio == 0.0) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No se seleccionaron adicionales.",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Precio Total
            Card(
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Precio Total",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "$${String.format("%.2f", totalPrice)}",
                        style = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.primary),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para volver a la lista de dispositivos
            Button(
                onClick = { onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Text(
                    text = "Volver a la lista de dispositivos",
                    style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.onPrimary)
                )
            }
        }
    }
}


@Composable
fun DispositivoDetailsScreen(
    dispositivoDetails: DispositivoWithDetails,
    onBack: () -> Unit,
    onVentaExitosa: () -> Unit,
    onError: (String) -> Unit
) {
    // Estados para opciones seleccionadas y adicionales
    val selectedOpciones = remember { mutableStateMapOf<String, Opcion>() }
    val selectedAdicionales = remember { mutableStateMapOf<Int, Boolean>() }

    // Cálculo de precios
    val basePlusPersonalizations by remember {
        derivedStateOf {
            dispositivoDetails.dispositivo.precioBase + selectedOpciones.values.sumOf { it.precioAdicional }
        }
    }

    val totalPrice by remember {
        derivedStateOf {
            val adicionalPrice = selectedAdicionales.filterValues { it }.keys.sumOf { id ->
                val adicional = dispositivoDetails.adicionales.firstOrNull { it.id == id }
                if (adicional != null && adicional.precioGratis != -1.0 &&
                    basePlusPersonalizations >= adicional.precioGratis
                ) {
                    0.0
                } else {
                    adicional?.precio ?: 0.0
                }
            }
            basePlusPersonalizations + adicionalPrice
        }
    }

    // Estados para diálogos y procesamiento
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showPurchaseDetails by remember { mutableStateOf(false) }
    var isProcessingVenta by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Mostrar PurchaseDetailsScreen si la compra fue exitosa
    if (showPurchaseDetails) {
        PurchaseDetailsScreen(
            dispositivoDetails = dispositivoDetails,
            selectedOpciones = selectedOpciones,
            selectedAdicionales = selectedAdicionales,
            totalPrice = totalPrice,
            onDismiss = {
                showPurchaseDetails = false
                onBack()
            }
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Scaffold(
                bottomBar = {
                    Column {
                        // Precio Total
                        Card(
                            elevation = 4.dp,
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Precio Total",
                                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colors.onSurface
                                )
                                Text(
                                    text = "$${String.format("%.2f", totalPrice)}",
                                    style = MaterialTheme.typography.h6.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botones de acción: Comprar y Cancelar
                        if (isProcessingVenta) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { showConfirmationDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                                ) {
                                    Text("Comprar", color = MaterialTheme.colors.onPrimary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { onBack() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text("Cancelar", color = MaterialTheme.colors.onSecondary)
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Botón para volver a la lista de dispositivos
                    Button(
                        onClick = { onBack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                    ) {
                        Text("Volver a la lista", color = MaterialTheme.colors.onSecondary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información del dispositivo
                    Card(
                        elevation = 8.dp,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = dispositivoDetails.dispositivo.nombre,
                                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dispositivoDetails.dispositivo.descripcion,
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Precio Base: ${dispositivoDetails.dispositivo.precioBase} ${dispositivoDetails.dispositivo.moneda}",
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Características
                    Card(
                        elevation = 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Características",
                                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            dispositivoDetails.caracteristicas.forEach { caracteristica ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "• ",
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                    Text(
                                        text = "${caracteristica.nombre}: ${caracteristica.descripcion}",
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Personalizaciones
                    Card(
                        elevation = 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Personalizaciones",
                                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            dispositivoDetails.personalizaciones.forEach { personalizacion ->
                                PersonalizacionItem(personalizacion, selectedOpciones)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Adicionales
                    Card(
                        elevation = 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Adicionales",
                                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AdicionalesSection(
                                adicionales = dispositivoDetails.adicionales,
                                selectedAdicionales = selectedAdicionales,
                                basePlusPersonalizations = basePlusPersonalizations
                            )
                        }
                    }

                    // Diálogo de confirmación de compra
                    if (showConfirmationDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmationDialog = false },
                            title = { Text("Confirmar Compra") },
                            text = { Text("¿Desea confirmar la compra?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmationDialog = false
                                        isProcessingVenta = true
                                        realizarVenta(
                                            dispositivoDetails,
                                            selectedOpciones,
                                            selectedAdicionales,
                                            totalPrice,
                                            onVentaExitosa = {
                                                isProcessingVenta = false
                                                showPurchaseDetails = true
                                            },
                                            onError = { mensaje ->
                                                isProcessingVenta = false
                                                errorMessage = mensaje
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                                ) {
                                    Text("Confirmar", color = MaterialTheme.colors.onPrimary)
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showConfirmationDialog = false },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text("Cancelar", color = MaterialTheme.colors.onSecondary)
                                }
                            }
                        )
                    }

                    // Mostrar mensaje de error si existe
                    errorMessage?.let { mensaje ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = mensaje,
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
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
fun AdicionalItem(
    adicional: Adicional,
    selectedAdicionales: MutableMap<Int, Boolean>,
    basePlusPersonalizations: Double
) {
    val enPromocion = adicional.precioGratis != -1.0 && basePlusPersonalizations >= adicional.precioGratis

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
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
                text = if (enPromocion) {
                    "Precio: Gratis (Promoción)"
                } else {
                    "Precio: $${String.format("%.2f", adicional.precio)}"
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
    selectedAdicionales: MutableMap<Int, Boolean>,
    basePlusPersonalizations: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "Adicionales:", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(adicionales) { adicional ->
                AdicionalItem(
                    adicional = adicional,
                    selectedAdicionales = selectedAdicionales,
                    basePlusPersonalizations = basePlusPersonalizations
                )
            }
        }
    }
}








