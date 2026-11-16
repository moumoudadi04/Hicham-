package com.hicham.autoparts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = InventoryDb(this)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF53D6C7), secondary = Color(0xFFFFB45C), background = Color(0xFF0B1220), surface = Color(0xFF172235))) { InventoryApp(db) } }
    }
}

@Composable
private fun InventoryApp(db: InventoryDb) {
    var brand by remember { mutableStateOf<String?>(null) }
    var selectedEngine by remember { mutableStateOf<Engine?>(null) }
    var showSales by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<Engine?>(null) }
    var partTarget by remember { mutableStateOf<Part?>(null) }
    var engineName by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var partName by remember { mutableStateOf("") }
    var partCategory by remember { mutableStateOf("قطع إضافية") }
    var amount by remember { mutableStateOf("1") }
    val engines = remember(brand, refresh) { brand?.let { db.engines(it) } ?: emptyList() }
    val parts = remember(selectedEngine?.id, refresh) { selectedEngine?.let { db.parts(it.id) } ?: emptyList() }
    val sales = remember(refresh, showSales) { db.sales() }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (brand != null || showSales || selectedEngine != null) {
                IconButton(onClick = {
                    when { selectedEngine != null -> selectedEngine = null; showSales -> showSales = false; else -> brand = null }
                }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            }
            Column(Modifier.weight(1f)) {
                Text("HICHAM • AUTO PARTS", color = Color(0xFF53D6C7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(when { showSales -> "سجل المبيعات"; selectedEngine != null -> selectedEngine!!.name; brand != null -> brand!!; else -> "مخزون المحركات" }, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            if (brand == null && selectedEngine == null && !showSales) {
                IconButton(onClick = { showSales = true }) { Icon(Icons.Default.History, "المبيعات", tint = Color(0xFFFFB45C)) }
            }
        }

        when {
            showSales -> {
                if (sales.isEmpty()) EmptyState("لا توجد عمليات بيع حتى الآن")
                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    sales.forEach { sale ->
                        CardRow {
                            Text(sale.partName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("المحرك: ${sale.engineName}  •  الكمية: ${sale.qty}", color = Color(0xFFB7C4D8))
                            Text(sale.soldAt, color = Color(0xFF7F91AA), fontSize = 12.sp)
                        }
                    }
                }
            }
            selectedEngine != null -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { dialog = "part" }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("إضافة قطعة") }
                    OutlinedButton(onClick = { refresh++ }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Inventory2, null); Spacer(Modifier.width(6.dp)); Text("تحديث") }
                }
                if (parts.isEmpty()) EmptyState("لا توجد قطع")
                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("كل قطعة تخص هذا المحرك وحده. المحركات المتشابهة لها مخزون منفصل.", color = Color(0xFF53D6C7), fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    parts.groupBy { it.category }.forEach { (category, list) ->
                        Text(category, color = Color(0xFFFFB45C), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                        list.forEach { part ->
                            CardRow {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(part.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(if (part.qty > 0) "المتوفر: ${part.qty}" else "نفدت الكمية", color = if (part.qty > 0) Color(0xFF53D6C7) else Color(0xFFFF7777), fontSize = 12.sp)
                                    }
                                    Button(onClick = { partTarget = part; amount = "1"; dialog = "sell" }, enabled = part.qty > 0) { Icon(Icons.Default.ShoppingCart, null); Text("بيع") }
                                }
                            }
                        }
                    }
                }
            }
            brand != null -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { engineName = ""; serial = ""; dialog = "engine" }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("إضافة محرك") }
                    OutlinedButton(onClick = { refresh++ }, modifier = Modifier.weight(1f)) { Text("تحديث القائمة") }
                }
                if (engines.isEmpty()) EmptyState("لا توجد محركات لهذه الماركة. أضف أول محرك.")
                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    engines.forEach { engine ->
                        CardRow {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).clickable { selectedEngine = engine }) {
                                    Text(engine.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(if (engine.serial.isBlank()) "رقم/ملاحظة غير محددة" else engine.serial, color = Color(0xFF9EADC2), fontSize = 12.sp)
                                    Text("اضغط لعرض القطع والمخزون", color = Color(0xFF53D6C7), fontSize = 12.sp)
                                }
                                IconButton(onClick = { editTarget = engine; engineName = engine.name; serial = engine.serial; dialog = "edit" }) { Icon(Icons.Default.Edit, "تعديل", tint = Color(0xFFFFB45C)) }
                                IconButton(onClick = { editTarget = engine; dialog = "delete" }) { Icon(Icons.Default.Delete, "حذف", tint = Color(0xFFFF7777)) }
                            }
                        }
                    }
                }
            }
            else -> {
                Text("اختر ماركة السيارة لإدارة محركاتها وقطعها", color = Color(0xFF9EADC2), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    BrandTile("RENAULT", "رونو", "🚘", listOf(Color(0xFF173B59), Color(0xFF247E91))) { brand = "رونو" }
                    BrandTile("PEUGEOT", "بيجو", "🚗", listOf(Color(0xFF35265E), Color(0xFF6652A6))) { brand = "بيجو" }
                    BrandTile("VOLKSWAGEN", "فولسفاكن", "🚙", listOf(Color(0xFF174B3B), Color(0xFF23856A))) { brand = "فولسفاكن" }
                    CardRow {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, tint = Color(0xFFFFB45C))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("سجل المبيعات", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("راجع كل القطع التي تم بيعها", color = Color(0xFF9EADC2), fontSize = 12.sp)
                            }
                            TextButton(onClick = { showSales = true }) { Text("فتح") }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "engine" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("إضافة محرك") }, text = {
            Column { OutlinedTextField(engineName, { engineName = it }, label = { Text("اسم المحرك (مثال 2.3 dCi)") }); OutlinedTextField(serial, { serial = it }, label = { Text("رقم المحرك أو ملاحظة (اختياري)") }) }
        }, confirmButton = { TextButton(onClick = { if (engineName.isNotBlank()) { db.addEngine(brand!!, engineName.trim(), serial.trim()); refresh++; dialog = "" } }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        "edit" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("تعديل المحرك") }, text = {
            Column { OutlinedTextField(engineName, { engineName = it }, label = { Text("اسم المحرك") }); OutlinedTextField(serial, { serial = it }, label = { Text("رقم/ملاحظة") }) }
        }, confirmButton = { TextButton(onClick = { editTarget?.let { db.renameEngine(it.id, engineName.trim()) }; refresh++; dialog = "" }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        "delete" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("حذف المحرك؟") }, text = { Text("سيتم حذف المحرك وقطع مخزونه وسجل مبيعاته.") }, confirmButton = { TextButton(onClick = { editTarget?.let { db.deleteEngine(it.id) }; refresh++; dialog = "" }) { Text("حذف") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        "part" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("إضافة قطعة للمحرك") }, text = {
            Column { OutlinedTextField(partName, { partName = it }, label = { Text("اسم القطعة") }); OutlinedTextField(partCategory, { partCategory = it }, label = { Text("القسم") }); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("الكمية") }) }
        }, confirmButton = { TextButton(onClick = { if (partName.isNotBlank()) { db.addPart(selectedEngine!!.id, partName.trim(), partCategory.trim().ifBlank { "قطع إضافية" }, amount.toIntOrNull() ?: 1); partName = ""; amount = "1"; refresh++; dialog = "" } }) { Text("إضافة") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        "sell" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("تأكيد بيع القطعة") }, text = {
            Column { Text(partTarget?.name ?: ""); Text("المتوفر: ${partTarget?.qty ?: 0}"); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("الكمية المباعة") }) }
        }, confirmButton = { TextButton(onClick = {
            val p = partTarget; val q = amount.toIntOrNull() ?: 0
            if (p != null && db.sell(p.engineId, p.id, q)) { refresh++; dialog = "" } else { amount = "0" }
        }) { Text("تسجيل البيع") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
    }
}

@Composable
private fun BrandTile(english: String, arabic: String, symbol: String, colors: List<Color>, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(145.dp).shadow(14.dp, RoundedCornerShape(24.dp)).background(Brush.linearGradient(colors), RoundedCornerShape(24.dp)).clickable { onClick() }.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 42.sp)
            Spacer(Modifier.weight(1f))
            Text("3D", color = Color.White.copy(alpha = .65f), fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Column {
            Text(arabic, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(english, color = Color.White.copy(alpha = .72f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun CardRow(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 9.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF172235)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color(0xFF9EADC2), fontSize = 16.sp)
    }
}
