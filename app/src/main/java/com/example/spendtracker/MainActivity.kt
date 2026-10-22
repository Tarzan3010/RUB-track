package com.example.spendtracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

data class Expense(val id: Long, val day: Long, val amount: Double, val note: String)

// Offline storage: everything stays on the phone (SharedPreferences)
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("spend", Context.MODE_PRIVATE)

    fun load(): List<Expense> = (p.getString("data", "") ?: "").lines().mapNotNull {
        val s = it.split("|", limit = 4)
        try {
            Expense(s[0].toLong(), s[1].toLong(), s[2].toDouble(), s[3])
        } catch (e: Exception) {
            null
        }
    }

    fun save(list: List<Expense>) {
        p.edit().putString(
            "data",
            list.joinToString("\n") { "${it.id}|${it.day}|${it.amount}|${it.note}" }
        ).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent { MaterialTheme { Surface { App(store) } } }
    }
}

@Composable
fun App(store: Store) {
    var items by remember { mutableStateOf(store.load()) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val today = LocalDate.now().toEpochDay()
    fun update(list: List<Expense>) { items = list; store.save(list) }

    val todayTotal = items.filter { it.day == today }.sumOf { it.amount }
    val allTotal = items.sumOf { it.amount }
    val days = (6 downTo 0).map { today - it }
    val sums = days.map { d -> items.filter { it.day == d }.sumOf { it.amount } }
    val max = (sums.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Spend Tracker", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Today: %.2f".format(todayTotal), fontSize = 18.sp)
        Text("Total spent: %.2f".format(allTotal), fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.replace("|", " ").replace("\n", " ") },
                label = { Text("Note") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val a = amount.replace(",", ".").toDoubleOrNull()
            if (a != null && a > 0) {
                update(items + Expense(System.currentTimeMillis(), today, a, note.trim()))
                amount = ""; note = ""
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Add expense") }

        Spacer(Modifier.height(16.dp))
        Text("Last 7 days", fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().height(160.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { i, d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.0f".format(sums[i]), fontSize = 10.sp)
                    Box(
                        Modifier.width(28.dp).height((110 * sums[i] / max).dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(LocalDate.ofEpochDay(d).dayOfWeek.name.take(3), fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("History", fontWeight = FontWeight.Bold)
        LazyColumn {
            items(items.sortedByDescending { it.id }.take(100), key = { it.id }) { e ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${LocalDate.ofEpochDay(e.day)}  ${e.note}",
                        modifier = Modifier.weight(1f)
                    )
                    Text("%.2f".format(e.amount))
                    TextButton(onClick = { update(items.filter { it.id != e.id }) }) { Text("Delete") }
                }
            }
        }
    }
}
