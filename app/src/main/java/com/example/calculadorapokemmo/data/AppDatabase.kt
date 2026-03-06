package com.example.calculadorapokemmo.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context: Context) : SQLiteOpenHelper(context, "PokeMMO.db", null, 8) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE precios (
                id INTEGER PRIMARY KEY AUTOINCREMENT, 
                nombre TEXT, 
                valor INTEGER, 
                cantidad INTEGER, 
                posicion INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE balls (
                id INTEGER PRIMARY KEY AUTOINCREMENT, 
                nombre TEXT, 
                costo_fab INTEGER, 
                precio_venta INTEGER, 
                cantidad INTEGER, 
                posicion INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE bayas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT,
                espacios INTEGER,
                dulce_simple INTEGER,
                dulce_muy INTEGER,
                picante_simple INTEGER,
                picante_muy INTEGER,
                posicion INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS precios")
        db.execSQL("DROP TABLE IF EXISTS balls")
        db.execSQL("DROP TABLE IF EXISTS bayas")
        onCreate(db)
    }

    fun guardarObjeto(obj: ObjetoPoke): Int {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put("nombre", obj.nombre)
            put("valor", obj.precio.toIntOrNull() ?: 0)
            put("cantidad", obj.cantidad.toIntOrNull() ?: 0)
        }
        return if (obj.id == null) {
            val c = db.rawQuery("SELECT MAX(posicion) FROM precios", null)
            val pos = if (c.moveToFirst()) c.getInt(0) + 1 else 0
            c.close()
            v.put("posicion", pos)
            db.insert("precios", null, v).toInt()
        } else {
            db.update("precios", v, "id=?", arrayOf(obj.id.toString()))
            obj.id
        }
    }

    fun obtenerObjetos(): List<ObjetoPoke> {
        val lista = mutableListOf<ObjetoPoke>()
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT * FROM precios ORDER BY posicion ASC", null)
        if (c.moveToFirst()) {
            do {
                lista.add(ObjetoPoke(
                    c.getInt(0), c.getString(1), c.getInt(2).toString(), c.getInt(3).toString()
                ))
            } while (c.moveToNext())
        }
        c.close()
        return lista
    }

    fun eliminarObjeto(id: Int) {
        this.writableDatabase.delete("precios", "id=?", arrayOf(id.toString()))
    }

    fun actualizarOrdenObjeto(id: Int, pos: Int) {
        val v = ContentValues().apply { put("posicion", pos) }
        this.writableDatabase.update("precios", v, "id=?", arrayOf(id.toString()))
    }

    fun guardarBall(obj: ObjetoBall): Int {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put("nombre", obj.nombre)
            put("costo_fab", obj.costoFab.toIntOrNull() ?: 0)
            put("precio_venta", obj.precioVenta.toIntOrNull() ?: 0)
            put("cantidad", obj.cantidad.toIntOrNull() ?: 0)
        }
        return if (obj.id == null) {
            val c = db.rawQuery("SELECT MAX(posicion) FROM balls", null)
            val pos = if (c.moveToFirst()) c.getInt(0) + 1 else 0
            c.close()
            v.put("posicion", pos)
            db.insert("balls", null, v).toInt()
        } else {
            db.update("balls", v, "id=?", arrayOf(obj.id.toString()))
            obj.id
        }
    }

    fun obtenerBalls(): List<ObjetoBall> {
        val lista = mutableListOf<ObjetoBall>()
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT * FROM balls ORDER BY posicion ASC", null)
        if (c.moveToFirst()) {
            do {
                lista.add(ObjetoBall(
                    c.getInt(0), c.getString(1), c.getInt(2).toString(), c.getInt(3).toString(), c.getInt(4).toString()
                ))
            } while (c.moveToNext())
        }
        c.close()
        return lista
    }

    fun eliminarBall(id: Int) {
        this.writableDatabase.delete("balls", "id=?", arrayOf(id.toString()))
    }

    fun actualizarOrdenBall(id: Int, pos: Int) {
        val v = ContentValues().apply { put("posicion", pos) }
        this.writableDatabase.update("balls", v, "id=?", arrayOf(id.toString()))
    }

    fun guardarBaya(obj: ObjetoBaya): Long {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put("nombre", obj.nombre)
            put("espacios", obj.espacios.toIntOrNull() ?: 0)
            put("dulce_simple", obj.dulceSimple.toIntOrNull() ?: 0)
            put("dulce_muy", obj.dulceMuy.toIntOrNull() ?: 0)
            put("picante_simple", obj.picanteSimple.toIntOrNull() ?: 0)
            put("picante_muy", obj.picanteMuy.toIntOrNull() ?: 0)
        }
        return if (obj.id == null) {
            val c = db.rawQuery("SELECT MAX(posicion) FROM bayas", null)
            val pos = if (c.moveToFirst()) c.getInt(0) + 1 else 0
            c.close()
            v.put("posicion", pos)
            db.insert("bayas", null, v)
        } else {
            db.update("bayas", v, "id=?", arrayOf(obj.id.toString()))
            obj.id
        }
    }

    fun obtenerBayas(): List<ObjetoBaya> {
        val lista = mutableListOf<ObjetoBaya>()
        val db = this.readableDatabase
        val c = db.rawQuery("SELECT * FROM bayas ORDER BY posicion ASC", null)
        if (c.moveToFirst()) {
            do {
                lista.add(ObjetoBaya(
                    id = c.getLong(0),
                    nombre = c.getString(1),
                    espacios = c.getInt(2).toString(),
                    dulceSimple = c.getInt(3).toString(),
                    dulceMuy = c.getInt(4).toString(),
                    picanteSimple = c.getInt(5).toString(),
                    picanteMuy = c.getInt(6).toString(),
                    orden = c.getInt(7)
                ))
            } while (c.moveToNext())
        }
        c.close()
        return lista
    }

    fun eliminarBaya(id: Long) {
        this.writableDatabase.delete("bayas", "id=?", arrayOf(id.toString()))
    }

    fun actualizarOrdenBaya(id: Long, pos: Int) {
        val v = ContentValues().apply { put("posicion", pos) }
        this.writableDatabase.update("bayas", v, "id=?", arrayOf(id.toString()))
    }

    fun borrarTodo() {
        val db = this.writableDatabase
        db.delete("precios", null, null)
        db.delete("balls", null, null)
        db.delete("bayas", null, null)
    }
}