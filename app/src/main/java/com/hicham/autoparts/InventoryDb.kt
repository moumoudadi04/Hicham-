package com.hicham.autoparts

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Engine(val id: Long, val brand: String, val name: String, val serial: String, val createdAt: String)
data class Part(val id: Long, val engineId: Long, val name: String, val category: String, val qty: Int)
data class Sale(val id: Long, val engineName: String, val partName: String, val qty: Int, val soldAt: String)

class InventoryDb(context: Context) : SQLiteOpenHelper(context, "hicham_parts.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE engines(id INTEGER PRIMARY KEY AUTOINCREMENT, brand TEXT NOT NULL, name TEXT NOT NULL, serial TEXT NOT NULL, created_at TEXT NOT NULL)")
        db.execSQL("CREATE TABLE parts(id INTEGER PRIMARY KEY AUTOINCREMENT, engine_id INTEGER NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, qty INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT, engine_id INTEGER NOT NULL, engine_name TEXT NOT NULL, part_name TEXT NOT NULL, qty INTEGER NOT NULL, sold_at TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    fun engines(brand: String): List<Engine> {
        val out = mutableListOf<Engine>()
        readableDatabase.rawQuery("SELECT id,brand,name,serial,created_at FROM engines WHERE brand=? ORDER BY id DESC", arrayOf(brand)).use { c ->
            while (c.moveToNext()) out += Engine(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))
        }
        return out
    }

    fun addEngine(brand: String, name: String, serial: String): Long {
        val v = ContentValues().apply { put("brand", brand); put("name", name); put("serial", serial); put("created_at", now()) }
        val id = writableDatabase.insertOrThrow("engines", null, v)
        seedParts(id)
        return id
    }

    fun renameEngine(id: Long, name: String) {
        writableDatabase.update("engines", ContentValues().apply { put("name", name) }, "id=?", arrayOf(id.toString()))
    }

    fun deleteEngine(id: Long) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("parts", "engine_id=?", arrayOf(id.toString()))
            writableDatabase.delete("sales", "engine_id=?", arrayOf(id.toString()))
            writableDatabase.delete("engines", "id=?", arrayOf(id.toString()))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    fun parts(engineId: Long): List<Part> {
        val out = mutableListOf<Part>()
        readableDatabase.rawQuery("SELECT id,engine_id,name,category,qty FROM parts WHERE engine_id=? ORDER BY category,name", arrayOf(engineId.toString())).use { c ->
            while (c.moveToNext()) out += Part(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getInt(4))
        }
        return out
    }

    fun addPart(engineId: Long, name: String, category: String, qty: Int) {
        writableDatabase.insertOrThrow("parts", null, ContentValues().apply {
            put("engine_id", engineId); put("name", name); put("category", category); put("qty", qty.coerceAtLeast(0))
        })
    }

    fun setQty(partId: Long, qty: Int) {
        writableDatabase.update("parts", ContentValues().apply { put("qty", qty.coerceAtLeast(0)) }, "id=?", arrayOf(partId.toString()))
    }

    fun sell(engineId: Long, partId: Long, amount: Int): Boolean {
        if (amount <= 0) return false
        val db = writableDatabase
        db.beginTransaction()
        try {
            var partName = ""
            var available = 0
            db.rawQuery("SELECT name,qty FROM parts WHERE id=? AND engine_id=?", arrayOf(partId.toString(), engineId.toString())).use { c ->
                if (!c.moveToFirst()) return false
                partName = c.getString(0); available = c.getInt(1)
            }
            if (available < amount) return false
            db.update("parts", ContentValues().apply { put("qty", available - amount) }, "id=?", arrayOf(partId.toString()))
            var engineName = ""
            db.rawQuery("SELECT name FROM engines WHERE id=?", arrayOf(engineId.toString())).use { c -> if (c.moveToFirst()) engineName = c.getString(0) }
            db.insertOrThrow("sales", null, ContentValues().apply {
                put("engine_id", engineId); put("engine_name", engineName); put("part_name", partName); put("qty", amount); put("sold_at", now())
            })
            db.setTransactionSuccessful()
            return true
        } finally { db.endTransaction() }
    }

    fun sales(): List<Sale> {
        val out = mutableListOf<Sale>()
        readableDatabase.rawQuery("SELECT id,engine_name,part_name,qty,sold_at FROM sales ORDER BY id DESC LIMIT 300", null).use { c ->
            while (c.moveToNext()) out += Sale(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getString(4))
        }
        return out
    }

    private fun seedParts(engineId: Long) {
        val catalog = listOf(
            "الكتلة والعمود المرفقي" to listOf("كتلة المحرك","عمود المرفق (الكرنك)","سبائك عمود المرفق","المكابس","شنابر المكابس","أذرع التوصيل","بنوز المكابس","حوض الزيت","سدادة حوض الزيت","حدافة المحرك","ترس الكرنك"),
            "رأس المحرك والصمامات" to listOf("رأس المحرك","غطاء رأس المحرك","عمود الكامات","صمامات السحب","صمامات العادم","يايات الصمامات","أدلة الصمامات","كراسي عمود الكامات","رافعات الصمامات","روكرات الصمامات","جوان رأس المحرك"),
            "التوقيت" to listOf("سير التوقيت","سلسلة التوقيت","شداد السير","بكرات التوجيه","تروس الكامات","غطاء التوقيت"),
            "الوقود والحقن" to listOf("مضخة الوقود","مضخة الحقن عالية الضغط","سكة الوقود","بخاخات الوقود","منظم ضغط الوقود","فلتر الوقود","أنابيب الوقود","حساس ضغط الوقود"),
            "السحب والعادم والتيربو" to listOf("الشاحن التوربيني (Turbo)","مبرد الهواء (Intercooler)","مجمع السحب","مجمع العادم","صمام EGR","بوابة الهواء","خرطوم السحب","فلتر الهواء","حساس MAF","حساس MAP"),
            "التبريد والتزييت" to listOf("مضخة الماء","مضخة الزيت","مبرد الزيت","ثرموستات","بيت الثرموستات","مروحة التبريد","أنابيب الماء","خزان التمدد","مبرد المحرك"),
            "الكهرباء والحساسات" to listOf("الدينامو","محرك التشغيل (السلف)","حساس عمود الكرنك","حساس عمود الكامات","حساس حرارة سائل التبريد","حساس ضغط الزيت","حساس الأكسجين","حساس الطرق","حساس دواسة الوقود","حساس سرعة المحرك","حساس مستوى الزيت","حساس حرارة الهواء","حساس ضغط الشحن","وحدة التحكم ECU","ضفيرة المحرك","شمعات التسخين","ملفات الإشعال"),
            "الملحقات والجوانات" to listOf("قواعد المحرك","بكرة الملحقات","شداد السير الخارجي","سير الملحقات","غطاء الصمامات","جوان غطاء الصمامات","جوان مجمع السحب","جوان مجمع العادم","صوفة الكرنك الأمامية","صوفة الكرنك الخلفية","مقياس الزيت","غطاء تعبئة الزيت","أنابيب التهوية")
        )
        val db = writableDatabase
        db.beginTransaction()
        try {
            catalog.forEach { (category, names) ->
                names.forEach { name ->
                    val qty = when {
                        name.contains("بخاخ") || name.contains("مكبس") || name.contains("أذرع") -> 4
                        name.contains("صمامات") -> 8
                        name.contains("شمعات") || name.contains("ملفات") -> 4
                        else -> 1
                    }
                    db.insertOrThrow("parts", null, ContentValues().apply {
                        put("engine_id", engineId); put("name", name); put("category", category); put("qty", qty)
                    })
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
