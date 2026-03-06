package com.example.calculadorapokemmo.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import com.example.calculadorapokemmo.R
import com.example.calculadorapokemmo.data.AppDatabase
import com.example.calculadorapokemmo.data.ObjetoBaya
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PestañaBayas(db: AppDatabase, lista: SnapshotStateList<ObjetoBaya>) {
    val scope = rememberCoroutineScope()
    var totalTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val deBaseDatos = db.obtenerBayas()
        val zanama = deBaseDatos.find { it.nombre == "Zanama" }
        lista.clear()
        if (zanama == null) {
            val nueva = ObjetoBaya(nombre = "Zanama", espacios = "156")
            val id = db.guardarBaya(nueva)
            lista.add(nueva.copy(id = id))
        } else {
            lista.add(zanama)
        }
    }

    val resumenGlobal by remember(totalTrigger, lista.size) {
        derivedStateOf {
            val item = lista.firstOrNull() ?: return@derivedStateOf "Cargando..."
            val esp = item.espacios.toIntOrNull() ?: 156
            val dS = item.dulceSimple.toIntOrNull() ?: 0
            val pS = item.picanteSimple.toIntOrNull() ?: 0
            val pM = item.picanteMuy.toIntOrNull() ?: 0

            val finalD = (dS - esp) - esp
            val muySobraP = pM - esp
            val finalP = (pS - muySobraP) - ((esp - muySobraP) * 3)

            var faltanteS = 0L
            if (finalD < 0) faltanteS += abs(finalD.toLong())
            if (finalP < 0) faltanteS += abs(finalP.toLong())

            if (faltanteS == 0L) "¡Semillas listas!" else "Faltan: $faltanteS Simples"
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mainView = LayoutInflater.from(ctx).inflate(R.layout.fragment_bayas, null)
            val container = mainView.findViewById<FrameLayout>(R.id.container_lista)
            val composeView = ComposeView(ctx).apply {
                setContent {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (lista.isNotEmpty()) {
                            val itemEstatico = remember { lista[0] }
                            AndroidView(
                                modifier = Modifier.fillMaxWidth(),
                                factory = { rowCtx ->
                                    LayoutInflater.from(rowCtx).inflate(R.layout.item_baya, null)
                                },
                                update = { rowView ->
                                    setupBayaRowLogicOptimized(rowView, itemEstatico, db, scope) { nuevaBaya ->
                                        if (lista.isNotEmpty()) {
                                            lista[0] = nuevaBaya
                                            totalTrigger++
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            container.addView(composeView)
            mainView
        },
    )
}

private fun setupBayaRowLogicOptimized(
    view: View,
    item: ObjetoBaya,
    db: AppDatabase,
    scope: kotlinx.coroutines.CoroutineScope,
    onDataChanged: (ObjetoBaya) -> Unit
) {
    val etEspacios = view.findViewById<EditText>(R.id.edit_espacios)
    val etDS = view.findViewById<EditText>(R.id.edit_dulce_simple)
    val etDM = view.findViewById<EditText>(R.id.edit_dulce_muy)
    val etPS = view.findViewById<EditText>(R.id.edit_picante_simple)
    val etPM = view.findViewById<EditText>(R.id.edit_picante_muy)
    val txtSobraD = view.findViewById<TextView>(R.id.txt_sobrante_dulce)
    val txtSobraP = view.findViewById<TextView>(R.id.txt_sobrante_picante)
    val etCantBayas = view.findViewById<EditText>(R.id.edit_cantidad_bayas)
    val txtCostoTotal = view.findViewById<TextView>(R.id.txt_costo_total_extractores)

    if (etEspacios.text.isEmpty()) {
        etEspacios.setText(item.espacios)
        etDS.setText(item.dulceSimple)
        etDM.setText(item.dulceMuy)
        etPS.setText(item.picanteSimple)
        etPM.setText(item.picanteMuy)
    }

    var saveJob: Job? = null

    fun calcularTodo() {
        val espStr = etEspacios.text.toString()
        val dSStr = etDS.text.toString()
        val dMStr = etDM.text.toString()
        val pSStr = etPS.text.toString()
        val pMStr = etPM.text.toString()

        val esp = espStr.toIntOrNull() ?: 156
        val dS = dSStr.toIntOrNull() ?: 0
        val dM = dMStr.toIntOrNull() ?: 0
        val pS = pSStr.toIntOrNull() ?: 0
        val pM = pMStr.toIntOrNull() ?: 0

        val finalD = (dS - esp) - esp
        val muySobraD = dM - esp
        txtSobraD.text = "Sobrante Simple: ${finalD} | Sobrante Muy: ${muySobraD}"
        txtSobraD.setTextColor(if (finalD >= 0 && muySobraD >= 0) 0xFF81C784.toInt() else 0xFFE57373.toInt())

        val muySobraP = pM - esp
        val finalP = (pS - muySobraP) - ((esp - muySobraP) * 3)
        txtSobraP.text = "Sobrante Simple: ${finalP}"
        txtSobraP.setTextColor(if (finalP >= 0) 0xFF81C784.toInt() else 0xFFE57373.toInt())

        val cant = etCantBayas.text.toString().toLongOrNull() ?: 0L
        txtCostoTotal.text = "Costo Total: $${String.format("%,d", cant * 350)}"

        val nuevaBaya = item.copy(
            espacios = espStr, dulceSimple = dSStr, dulceMuy = dMStr,
            picanteSimple = pSStr, picanteMuy = pMStr
        )
        onDataChanged(nuevaBaya)

        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            db.guardarBaya(nuevaBaya)
        }
    }

    val genericWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { calcularTodo() }
    }

    val allFields = listOf(etEspacios, etDS, etDM, etPS, etPM, etCantBayas)
    allFields.forEach {
        it.tag?.let { old -> it.removeTextChangedListener(old as TextWatcher) }
        it.addTextChangedListener(genericWatcher)
        it.tag = genericWatcher
    }

    calcularTodo()
}