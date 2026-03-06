package com.example.calculadorapokemmo.data

data class ObjetoPoke(
    val id: Int? = null,
    val nombre: String = "",
    val precio: String = "",
    val cantidad: String = "1",
    val visible: Boolean = true
)

data class ObjetoBall(
    val id: Int? = null,
    val nombre: String = "",
    val costoFab: String = "0",
    val precioVenta: String = "0",
    val cantidad: String = "1",
    val visible: Boolean = true
)

// data/ObjetoBaya.kt
data class ObjetoBaya(
    val id: Long? = null,
    val nombre: String = "",
    val espacios: String = "156",
    val dulceSimple: String = "0",
    val dulceMuy: String = "0",
    val picanteSimple: String = "0",
    val picanteMuy: String = "0",
    val orden: Int = 0
)