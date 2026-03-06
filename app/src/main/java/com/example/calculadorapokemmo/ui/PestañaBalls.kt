package com.example.calculadorapokemmo.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.calculadorapokemmo.R
import com.example.calculadorapokemmo.data.AppDatabase
import com.example.calculadorapokemmo.data.ObjetoBall
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PestañaBall(db: AppDatabase, lista: SnapshotStateList<ObjetoBall>) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    var totalTrigger by remember { mutableIntStateOf(0) }
    var aplicarImpuesto by remember { mutableStateOf(false) }

    val totalVentaRaw by remember(totalTrigger, lista.size) {
        derivedStateOf { lista.filter { it.visible }.sumOf { (it.precioVenta.toLongOrNull() ?: 0L) * (it.cantidad.toIntOrNull() ?: 0) } }
    }

    val totalVentaConImpuesto by remember(totalVentaRaw, aplicarImpuesto) {
        derivedStateOf {
            if (aplicarImpuesto) totalVentaRaw - (totalVentaRaw / 20) else totalVentaRaw
        }
    }

    val totalFab by remember(totalTrigger, lista.size) {
        derivedStateOf { lista.filter { it.visible }.sumOf { (it.costoFab.toLongOrNull() ?: 0L) * (it.cantidad.toIntOrNull() ?: 0) } }
    }

    val gananciaTotal by remember(totalVentaConImpuesto, totalFab) {
        derivedStateOf { totalVentaConImpuesto - totalFab }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var indexToDelete by remember { mutableIntStateOf(-1) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Eliminar Ball") },
            text = { Text("¿Estás seguro de que quieres borrar esta fila?") },
            confirmButton = {
                TextButton(onClick = {
                    if (indexToDelete != -1 && indexToDelete < lista.size) {
                        lista[indexToDelete].id?.let { db.eliminarBall(it) }
                        lista.removeAt(indexToDelete)
                        totalTrigger++
                    }
                    showConfirmDialog = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mainView = LayoutInflater.from(ctx).inflate(R.layout.fragment_ball, null)
            val swImpuesto = mainView.findViewById<View>(R.id.sw_impuesto_balls) as? CompoundButton

            swImpuesto?.setOnCheckedChangeListener { _, isChecked ->
                aplicarImpuesto = isChecked
            }

            mainView.findViewById<Button>(R.id.btn_add_ball).setOnClickListener {
                val incompleta = lista.any { it.nombre.isBlank() || it.costoFab.isBlank() || it.precioVenta.isBlank() }
                if (incompleta) {
                    Toast.makeText(ctx, "Completa los campos antes de añadir otra", Toast.LENGTH_SHORT).show()
                } else {
                    val nuevaBall = ObjetoBall(nombre = "", costoFab = "0", precioVenta = "0", cantidad = "1", visible = true)
                    val id = db.guardarBall(nuevaBall)
                    lista.add(nuevaBall.copy(id = id))
                    totalTrigger++
                }
            }

            val container = mainView.findViewById<FrameLayout>(R.id.container_lista_balls)
            val composeView = ComposeView(ctx).apply {
                setContent {
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(lista, key = { _, item -> item.id ?: item.hashCode() }) { index, item ->
                            var isDragging by remember { mutableStateOf(false) }
                            var accumulatedDragY by remember { mutableFloatStateOf(0f) }
                            val scale by animateFloatAsState(if (isDragging) 1.06f else 1.0f)
                            val elevation by animateFloatAsState(if (isDragging) 15f else 0f)

                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation
                                }
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    factory = { rowCtx -> LayoutInflater.from(rowCtx).inflate(R.layout.item_ball, null) },
                                    update = { row ->
                                        setupBallRowLogic(row, item, lista, db, onUpdate = { totalTrigger++ }) {
                                            indexToDelete = lista.indexOf(item)
                                            showConfirmDialog = true
                                        }
                                    }
                                )

                                Spacer(
                                    modifier = Modifier
                                        .padding(start = 4.dp, top = 4.dp)
                                        .size(32.dp)
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { isDragging = true; accumulatedDragY = 0f },
                                                onDragEnd = { isDragging = false },
                                                onDragCancel = { isDragging = false },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDragY += dragAmount.y
                                                    val rowHeightPx = 150f
                                                    val actualPos = lista.indexOf(item)
                                                    if (actualPos != -1) {
                                                        if (accumulatedDragY > rowHeightPx && actualPos < lista.size - 1) {
                                                            lista.add(actualPos + 1, lista.removeAt(actualPos))
                                                            accumulatedDragY -= rowHeightPx
                                                            actualizarOrdenBallBD(db, lista)
                                                        } else if (accumulatedDragY < -rowHeightPx && actualPos > 0) {
                                                            lista.add(actualPos - 1, lista.removeAt(actualPos))
                                                            accumulatedDragY += rowHeightPx
                                                            actualizarOrdenBallBD(db, lista)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
            container.addView(composeView)
            mainView
        },
        update = { mainView ->
            mainView.findViewById<TextView>(R.id.txt_ganancia_balls).text = "$${formatter.format(gananciaTotal)}"
            mainView.findViewById<TextView>(R.id.txt_detalle_balls).text =
                "Inversión: $${formatter.format(totalFab)} | Venta: $${formatter.format(totalVentaConImpuesto)}"
        }
    )
}

private fun actualizarOrdenBallBD(db: AppDatabase, lista: List<ObjetoBall>) {
    lista.forEachIndexed { i, o -> o.id?.let { db.actualizarOrdenBall(it, i) } }
}

private fun setupBallRowLogic(view: View, item: ObjetoBall, lista: SnapshotStateList<ObjetoBall>, db: AppDatabase, onUpdate: () -> Unit, onDelete: () -> Unit) {
    val etNombre = view.findViewById<EditText>(R.id.edit_nombre_ball)
    val etCosto = view.findViewById<EditText>(R.id.edit_costo_fab)
    val etVenta = view.findViewById<EditText>(R.id.edit_precio_venta)
    val etCantidad = view.findViewById<EditText>(R.id.edit_cantidad_ball)
    val btnDelete = view.findViewById<ImageButton>(R.id.btn_delete_ball)
    val btnEye = view.findViewById<ImageButton>(R.id.btn_toggle_eye_ball)

    etNombre.tag?.let { etNombre.removeTextChangedListener(it as TextWatcher) }
    etCosto.tag?.let { etCosto.removeTextChangedListener(it as TextWatcher) }
    etVenta.tag?.let { etVenta.removeTextChangedListener(it as TextWatcher) }
    etCantidad.tag?.let { etCantidad.removeTextChangedListener(it as TextWatcher) }

    if (!etNombre.hasFocus()) etNombre.setText(item.nombre)
    if (!etCosto.hasFocus()) etCosto.setText(item.costoFab)
    if (!etVenta.hasFocus()) etVenta.setText(item.precioVenta)
    if (!etCantidad.hasFocus()) etCantidad.setText(item.cantidad)

    actualizarVisualOjoBall(btnEye, item.visible, listOf(etNombre, etCosto, etVenta, etCantidad))

    btnEye.setOnClickListener {
        val idx = lista.indexOf(item)
        if (idx != -1) {
            val nuevoEstado = !item.visible
            val objAct = item.copy(visible = nuevoEstado)
            db.guardarBall(objAct)
            lista[idx] = objAct
            actualizarVisualOjoBall(btnEye, nuevoEstado, listOf(etNombre, etCosto, etVenta, etCantidad))
            onUpdate()
        }
    }

    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val idx = lista.indexOf(item)
            if (idx != -1) {
                val objAct = item.copy(
                    nombre = etNombre.text.toString(),
                    costoFab = etCosto.text.toString(),
                    precioVenta = etVenta.text.toString(),
                    cantidad = etCantidad.text.toString()
                )
                if (objAct != item) {
                    db.guardarBall(objAct)
                    lista[idx] = objAct
                    onUpdate()
                }
            }
        }
    }

    listOf(etNombre, etCosto, etVenta, etCantidad).forEach {
        it.addTextChangedListener(watcher)
        it.tag = watcher
    }

    btnDelete.setOnClickListener { onDelete() }
}

private fun actualizarVisualOjoBall(btn: ImageButton, visible: Boolean, views: List<View>) {
    btn.setImageResource(if (visible) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel)
    views.forEach { it.alpha = if (visible) 1.0f else 0.4f }
}