package com.example.calculadorapokemmo

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.calculadorapokemmo.data.AppDatabase
import com.example.calculadorapokemmo.data.ObjetoBall
import com.example.calculadorapokemmo.data.ObjetoPoke
import com.example.calculadorapokemmo.data.ObjetoBaya
import com.example.calculadorapokemmo.ui.PestañaBall
import com.example.calculadorapokemmo.ui.PestañaObjetos
import com.example.calculadorapokemmo.ui.PestañaBayas
import com.google.android.material.navigation.NavigationView
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val listaObjetos = mutableStateListOf<ObjetoPoke>()
    private val listaBalls = mutableStateListOf<ObjetoBall>()
    private val listaBayas = mutableStateListOf<ObjetoBaya>()
    private val formatter = NumberFormat.getIntegerInstance(Locale.US)

    private var vistaInicio: View? = null
    private var vistaObjetos: ComposeView? = null
    private var vistaBalls: ComposeView? = null
    private var vistaBayas: ComposeView? = null
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- BLOQUEAR MODO OSCURO ---
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        // ----------------------------
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase(this)
        container = findViewById(R.id.fragment_container)

        cargarDatos()
        precargarPantallas()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.navigation_view)

        navView.itemIconTintList = null
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> mostrarPantalla(0)
                R.id.nav_objetos -> mostrarPantalla(1)
                R.id.nav_balls -> mostrarPantalla(2)
                R.id.nav_bayas -> mostrarPantalla(3)
                R.id.nav_delete_all -> mostrarDialogoBorrarTodo()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        mostrarPantalla(0)
    }

    private fun precargarPantallas() {
        vistaInicio = layoutInflater.inflate(R.layout.item_inicio, container, false).apply {
            visibility = View.GONE
            container.addView(this)
        }

        vistaObjetos = ComposeView(this).apply {
            setContent { PestañaObjetos(db, listaObjetos) }
            visibility = View.GONE
            container.addView(this)
        }

        vistaBalls = ComposeView(this).apply {
            setContent { PestañaBall(db, listaBalls) }
            visibility = View.GONE
            container.addView(this)
        }

        vistaBayas = ComposeView(this).apply {
            setContent { PestañaBayas(db, listaBayas) }
            visibility = View.GONE
            container.addView(this)
        }
    }

    private fun mostrarPantalla(index: Int) {
        vistaInicio?.visibility = if (index == 0) {
            actualizarResumenInicio(vistaInicio!!)
            View.VISIBLE
        } else View.GONE

        vistaObjetos?.visibility = if (index == 1) View.VISIBLE else View.GONE
        vistaBalls?.visibility = if (index == 2) View.VISIBLE else View.GONE
        vistaBayas?.visibility = if (index == 3) View.VISIBLE else View.GONE

        val titulos = listOf("Calculadora PokeMMO", "Objetos", "Balls", "Bayas")
        supportActionBar?.title = titulos[index]
    }

    private fun actualizarResumenInicio(view: View) {
        val txtObjetos = view.findViewById<TextView>(R.id.txt_resumen_objetos)
        val txtBalls = view.findViewById<TextView>(R.id.txt_resumen_balls)

        val sumaObj = listaObjetos.sumOf {
            val p = it.precio.toLongOrNull() ?: 0L
            val c = it.cantidad.toIntOrNull() ?: 0
            p * c
        }

        val vBalls = listaBalls.sumOf {
            (it.precioVenta.toLongOrNull() ?: 0L) * (it.cantidad.toIntOrNull() ?: 0)
        }
        val fBalls = listaBalls.sumOf {
            (it.costoFab.toLongOrNull() ?: 0L) * (it.cantidad.toIntOrNull() ?: 0)
        }

        txtObjetos.text = "$${formatter.format(sumaObj)}"
        txtBalls.text = "$${formatter.format(vBalls - fBalls)}"
    }

    private fun cargarDatos() {
        try {
            listaObjetos.clear()
            listaBalls.clear()
            listaBayas.clear()
            listaObjetos.addAll(db.obtenerObjetos())
            listaBalls.addAll(db.obtenerBalls())
            listaBayas.addAll(db.obtenerBayas())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mostrarDialogoBorrarTodo() {
        AlertDialog.Builder(this)
            .setTitle("¿Confirmar eliminación?")
            .setMessage("Se borrarán todos los datos permanentemente.")
            .setPositiveButton("BORRAR TODO") { _, _ ->
                db.borrarTodo()
                listaObjetos.clear()
                listaBalls.clear()
                listaBayas.clear()
                vistaInicio?.let { actualizarResumenInicio(it) }
                Toast.makeText(this, "Datos eliminados", Toast.LENGTH_SHORT).show()
                finish();
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}