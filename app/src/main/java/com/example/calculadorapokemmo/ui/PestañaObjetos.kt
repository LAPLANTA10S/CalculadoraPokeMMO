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
import com.example.calculadorapokemmo.data.ObjetoPoke
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PestañaObjetos(db: AppDatabase, lista: SnapshotStateList<ObjetoPoke>) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    var totalTrigger by remember { mutableIntStateOf(0) }
    var aplicarImpuesto by remember { mutableStateOf(false) }

    val total by remember(totalTrigger, lista.size, aplicarImpuesto) {
        derivedStateOf {
            val bruto = lista.filter { it.visible }
                .sumOf { (it.precio.toLongOrNull() ?: 0L) * (it.cantidad.toIntOrNull() ?: 0) }
            if (aplicarImpuesto) {
                val impuesto = bruto / 20
                bruto - impuesto
            } else {
                bruto
            }
        }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var indexToDelete by remember { mutableIntStateOf(-1) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Eliminar Fila") },
            text = { Text("¿Estás seguro de que quieres borrar este objeto?") },
            confirmButton = {
                TextButton(onClick = {
                    if (indexToDelete != -1 && indexToDelete < lista.size) {
                        lista[indexToDelete].id?.let { db.eliminarObjeto(it) }
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
            val mainView = LayoutInflater.from(ctx).inflate(R.layout.fragment_objetos, null)
            val swImpuesto = mainView.findViewById<View>(R.id.sw_impuesto_gtl) as? CompoundButton

            swImpuesto?.setOnCheckedChangeListener { _, isChecked ->
                aplicarImpuesto = isChecked
            }

            mainView.findViewById<Button>(R.id.btn_add_objeto).setOnClickListener {
                val incompleta = lista.any { it.nombre.isBlank() || it.precio.isBlank() || it.cantidad.isBlank() }
                if (incompleta) {
                    Toast.makeText(ctx, "Rellena los campos actuales primero", Toast.LENGTH_SHORT).show()
                } else {
                    val nuevoObjeto = ObjetoPoke(nombre = "", precio = "1", cantidad = "1", visible = true)
                    val id = db.guardarObjeto(nuevoObjeto)
                    lista.add(nuevoObjeto.copy(id = id))
                    totalTrigger++
                }
            }

            val container = mainView.findViewById<FrameLayout>(R.id.container_lista)
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
                                    factory = { rowCtx -> LayoutInflater.from(rowCtx).inflate(R.layout.item_objeto, null) },
                                    update = { row ->
                                        setupRowLogic(row, item, lista, db, onUpdate = { totalTrigger++ }) {
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
                                                            actualizarOrdenBD(db, lista)
                                                        } else if (accumulatedDragY < -rowHeightPx && actualPos > 0) {
                                                            lista.add(actualPos - 1, lista.removeAt(actualPos))
                                                            accumulatedDragY += rowHeightPx
                                                            actualizarOrdenBD(db, lista)
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
            mainView.findViewById<TextView>(R.id.txt_total).text = "$${formatter.format(total)}"
        }
    )
}

private fun actualizarOrdenBD(db: AppDatabase, lista: List<ObjetoPoke>) {
    lista.forEachIndexed { i, o -> o.id?.let { db.actualizarOrdenObjeto(it, i) } }
}

private fun setupRowLogic(view: View, item: ObjetoPoke, lista: SnapshotStateList<ObjetoPoke>, db: AppDatabase, onUpdate: () -> Unit, onDelete: () -> Unit) {
    val etNombre = view.findViewById<EditText>(R.id.edit_nombre)
    val etPrecio = view.findViewById<EditText>(R.id.edit_precio)
    val etCantidad = view.findViewById<EditText>(R.id.edit_cantidad)
    val btnDelete = view.findViewById<ImageButton>(R.id.btn_delete)
    val btnEye = view.findViewById<ImageButton>(R.id.btn_toggle_eye)

    etNombre.tag?.let { etNombre.removeTextChangedListener(it as TextWatcher) }
    etPrecio.tag?.let { etPrecio.removeTextChangedListener(it as TextWatcher) }
    etCantidad.tag?.let { etCantidad.removeTextChangedListener(it as TextWatcher) }

    if (!etNombre.hasFocus()) etNombre.setText(item.nombre)
    if (!etPrecio.hasFocus()) etPrecio.setText(item.precio)
    if (!etCantidad.hasFocus()) etCantidad.setText(item.cantidad)

    actualizarEstadoOjoUI(btnEye, item.visible, listOf(etNombre, etPrecio, etCantidad))

    btnEye.setOnClickListener {
        val idx = lista.indexOf(item)
        if (idx != -1) {
            val nuevoEstado = !item.visible
            val objActualizado = item.copy(visible = nuevoEstado)
            db.guardarObjeto(objActualizado)
            lista[idx] = objActualizado
            actualizarEstadoOjoUI(btnEye, nuevoEstado, listOf(etNombre, etPrecio, etCantidad))
            onUpdate()
        }
    }

    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val idx = lista.indexOf(item)
            if (idx != -1) {
                val nuevoObj = item.copy(
                    nombre = etNombre.text.toString(),
                    precio = etPrecio.text.toString(),
                    cantidad = etCantidad.text.toString()
                )
                if (nuevoObj != item) {
                    db.guardarObjeto(nuevoObj)
                    lista[idx] = nuevoObj
                    onUpdate()
                }
            }
        }
    }

    etNombre.addTextChangedListener(watcher)
    etPrecio.addTextChangedListener(watcher)
    etCantidad.addTextChangedListener(watcher)
    etNombre.tag = watcher
    etPrecio.tag = watcher
    etCantidad.tag = watcher

    btnDelete.setOnClickListener { onDelete() }
}

private fun actualizarEstadoOjoUI(btn: ImageButton, visible: Boolean, views: List<View>) {
    if (visible) {
        btn.setImageResource(android.R.drawable.ic_menu_view)
        views.forEach { it.alpha = 1.0f }
    } else {
        btn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        views.forEach { it.alpha = 0.4f }
    }
}