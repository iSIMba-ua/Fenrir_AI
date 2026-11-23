package com.fenrir.ai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF080D18)
private val Panel = Color(0xFF121C2D)
private val Blue = Color(0xFF168BFF)
private val Muted = Color(0xFF9AAAC0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FenrirApp() }
    }
}

data class Member(val id: Long, val name: String)
data class Contract(val id: Long, val name: String, val money: Long, val materials: String, val reputation: Int, val experience: Int, val min: Int, val durationHours: Int, val cooldownHours: Int, val lastDone: Long = 0L)
data class Execution(val id: Long, val contract: String, val people: List<String>, val money: Long, val timestamp: Long)

private class Store(context: Context) {
    private val prefs = context.getSharedPreferences("fenrir_local", Context.MODE_PRIVATE)
    fun loadMembers(): List<Member> {
        val a = JSONArray(prefs.getString("members", "[]"))
        return (0 until a.length()).map { Member(a.getJSONObject(it).getLong("id"), a.getJSONObject(it).getString("name")) }
    }
    fun saveMembers(v: List<Member>) { prefs.edit().putString("members", JSONArray().apply { v.forEach { put(JSONObject().put("id",it.id).put("name",it.name)) } }.toString()).apply() }
    fun loadContracts(): List<Contract> {
        val raw = prefs.getString("contracts", null)
        if (raw == null) {
            val defaults = listOf(Contract(1,"Темні води І",11500,"",0,0,2,1,24), Contract(2,"Чисте місто І",12000,"",0,0,2,1,24), Contract(3,"Логістика І",12000,"",0,0,2,1,24))
            saveContracts(defaults); return defaults
        }
        val a = JSONArray(raw)
        return (0 until a.length()).map { val o=a.getJSONObject(it); Contract(o.getLong("id"),o.getString("name"),o.getLong("money"),o.optString("materials"),o.optInt("reputation"),o.optInt("experience"),o.getInt("min"),o.getInt("duration"),o.getInt("cooldown"),o.optLong("lastDone")) }
    }
    fun saveContracts(v: List<Contract>) { prefs.edit().putString("contracts", JSONArray().apply { v.forEach { put(JSONObject().put("id",it.id).put("name",it.name).put("money",it.money).put("materials",it.materials).put("reputation",it.reputation).put("experience",it.experience).put("min",it.min).put("duration",it.durationHours).put("cooldown",it.cooldownHours).put("lastDone",it.lastDone)) } }.toString()).apply() }
    fun loadExecutions(): List<Execution> {
        val a=JSONArray(prefs.getString("executions","[]")); return (0 until a.length()).map { val o=a.getJSONObject(it); val p=o.getJSONArray("people"); Execution(o.getLong("id"),o.getString("contract"),(0 until p.length()).map { i->p.getString(i) },o.getLong("money"),o.getLong("timestamp")) }.sortedByDescending { it.timestamp }
    }
    fun saveExecutions(v: List<Execution>) { prefs.edit().putString("executions",JSONArray().apply { v.forEach { e-> put(JSONObject().put("id",e.id).put("contract",e.contract).put("people",JSONArray(e.people)).put("money",e.money).put("timestamp",e.timestamp)) } }.toString()).apply() }
}

@Composable private fun FenrirApp() {
    val context= LocalContext.current
    val store= remember { Store(context) }
    var members by remember { mutableStateOf(store.loadMembers()) }
    var contracts by remember { mutableStateOf(store.loadContracts()) }
    var executions by remember { mutableStateOf(store.loadExecutions()) }
    var tab by remember { mutableStateOf("Головна") }
    var memberName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var notice by remember { mutableStateOf("") }
    MaterialTheme(colorScheme = darkColorScheme(primary=Blue, background=Navy, surface=Panel, onSurface=Color.White)) {
        Column(Modifier.fillMaxSize().background(Navy).padding(16.dp)) {
            Text("ᚠ FENRIR AI", color=Blue, fontSize=25.sp, fontWeight=FontWeight.Black)
            Text("FAMILY MANAGEMENT SYSTEM · 1.2.0", color=Muted, fontSize=10.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Головна","Контракти","Учасники","Статистика").forEach { t ->
                    Box(Modifier.weight(1f).background(if(tab==t) Blue else Panel, RoundedCornerShape(10.dp)).clickable { tab=t }.padding(vertical=10.dp), contentAlignment=Alignment.Center) { Text(t, fontSize=10.sp, color=Color.White, fontWeight=FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(14.dp))
            when(tab) {
                "Головна" -> {
                    Header("ПАНЕЛЬ СІМ'Ї")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Metric("Учасники",members.size.toString(),Modifier.weight(1f)); Metric("Виконань",executions.size.toString(),Modifier.weight(1f)); Metric("Заробіток", "${executions.sumOf { it.money }}$",Modifier.weight(1f)) }
                    Spacer(Modifier.height(14.dp)); Header("ОСТАННІ ВИКОНАННЯ")
                    if(executions.isEmpty()) Empty("Поки що немає виконаних контрактів") else LazyColumn { items(executions.take(5)) { ExecutionCard(it) } }
                }
                "Учасники" -> {
                    Header("УЧАСНИКИ СІМ'Ї")
                    Row(verticalAlignment=Alignment.CenterVertically) { Field(memberName,{memberName=it},"Ім'я учасника",Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Button(onClick={ if(memberName.isNotBlank()){ members=members+Member(System.currentTimeMillis(),memberName.trim()); store.saveMembers(members); memberName="" } }) { Text("Додати") } }
                    Spacer(Modifier.height(8.dp)); LazyColumn { items(members) { m -> Row(Modifier.fillMaxWidth().padding(vertical=6.dp).background(Panel,RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment=Alignment.CenterVertically) { Text(m.name,Modifier.weight(1f),color=Color.White); Text("Видалити",color=Color(0xFFFF6877),modifier=Modifier.clickable { members=members.filterNot { it.id==m.id }; store.saveMembers(members) }) } } }
                }
                "Контракти" -> {
                    Header("АКТИВНІ КОНТРАКТИ")
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        items(contracts) { c ->
                            val remaining=(c.lastDone+c.cooldownHours*3600000L-System.currentTimeMillis()).coerceAtLeast(0L)
                            val locked=remaining>0
                            Column(Modifier.fillMaxWidth().background(if(locked) Color(0xFF252B35) else Panel,RoundedCornerShape(14.dp)).padding(14.dp)) {
                                Text(c.name,color=if(locked) Muted else Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp)
                                Text("Нагорода: ${c.money}$ · ${c.materials.ifBlank { "без матеріалів" }}",color=Muted,fontSize=12.sp)
                                Text("Мінімум виконавців: ${c.min} · Час: ${c.durationHours} год.",color=Muted,fontSize=11.sp)
                                if(locked) Text("ВІДНОВЛЕННЯ: ${remaining/3600000} год. ${(remaining/60000)%60} хв.",color=Muted) else {
                                    Text("ОБЕРИ УЧАСНИКІВ",color=Blue,fontSize=10.sp,fontWeight=FontWeight.Bold)
                                    members.forEach { m -> Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checked=selected.contains("${c.id}:${m.id}"),onCheckedChange={v-> selected=if(v) selected+"${c.id}:${m.id}" else selected-"${c.id}:${m.id}" }); Text(m.name,color=Color.White,fontSize=13.sp) } }
                                    Button(onClick={ val names=members.filter { selected.contains("${c.id}:${it.id}") }.map { it.name }; if(names.size<c.min) notice="Потрібно щонайменше ${c.min} виконавців" else { val now=System.currentTimeMillis(); executions=listOf(Execution(now,c.name,names,c.money,now))+executions; store.saveExecutions(executions); contracts=contracts.map { if(it.id==c.id) it.copy(lastDone=now) else it }; store.saveContracts(contracts); selected=emptySet(); notice="Контракт зараховано!" } },enabled=members.isNotEmpty()) { Text("ВИКОНАНО") }
                                }
                            }
                        }
                    }
                    if(notice.isNotBlank()) Text(notice,color=Blue,modifier=Modifier.clickable { notice="" }.padding(6.dp))
                }
                else -> {
                    Header("СТАТИСТИКА")
                    val now=System.currentTimeMillis(); val day=86400000L
                    val today=executions.filter { now-it.timestamp<day }; val week=executions.filter { now-it.timestamp<7*day }; val month=executions.filter { now-it.timestamp<30*day }
                    Metric("Сьогодні", "${today.sumOf { it.money }}$ · ${today.size} виконань",Modifier.fillMaxWidth())
                    Metric("7 днів", "${week.sumOf { it.money }}$ · ${week.size} виконань",Modifier.fillMaxWidth())
                    Metric("30 днів", "${month.sumOf { it.money }}$ · ${month.size} виконань",Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp)); Header("РЕЙТИНГ УЧАСНИКІВ")
                    val ranking=executions.flatMap { e->e.people }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
                    if(ranking.isEmpty()) Empty("Дані з'являться після перших виконань") else ranking.forEachIndexed { i,p-> Text("${i+1}. ${p.first} — ${p.second} участі",color=Color.White,modifier=Modifier.padding(6.dp)) }
                    Spacer(Modifier.height(12.dp)); Header("ЖУРНАЛ ВИКОНАНЬ")
                    LazyColumn { items(executions) { e -> Row(Modifier.fillMaxWidth().padding(vertical=4.dp).background(Panel,RoundedCornerShape(8.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(e.contract,color=Color.White,fontWeight=FontWeight.Bold); Text("${e.people.joinToString()} · ${SimpleDateFormat("dd.MM.yy HH:mm",Locale.getDefault()).format(Date(e.timestamp))}",color=Muted,fontSize=10.sp) }; Text("${e.money}$",color=Blue); Text(" ✕",color=Color(0xFFFF6877),modifier=Modifier.clickable { executions=executions.filterNot { it.id==e.id }; store.saveExecutions(executions) }) } } }
                }
            }
        }
    }
}

@Composable private fun Header(s:String){ Text(s,color=Blue,fontSize=12.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(bottom=8.dp)) }
@Composable private fun Metric(label:String,value:String,modifier:Modifier=Modifier){ Column(modifier.background(Panel,RoundedCornerShape(12.dp)).padding(12.dp)) { Text(label,color=Muted,fontSize=10.sp); Text(value,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold) } }
@Composable private fun Empty(s:String){ Text(s,color=Muted,modifier=Modifier.padding(12.dp)) }
@Composable private fun Field(value:String,onValue:(String)->Unit,hint:String,modifier:Modifier=Modifier){ Box(modifier.background(Panel,RoundedCornerShape(10.dp)).padding(12.dp)){ if(value.isEmpty()) Text(hint,color=Muted); BasicTextField(value,onValue, textStyle=androidx.compose.ui.text.TextStyle(color=Color.White,fontSize=14.sp), singleLine=true) } }
@Composable private fun ExecutionCard(e:Execution){ Row(Modifier.fillMaxWidth().padding(vertical=4.dp).background(Panel,RoundedCornerShape(10.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){ Column(Modifier.weight(1f)){ Text(e.contract,color=Color.White,fontWeight=FontWeight.Bold); Text(e.people.joinToString(),color=Muted,fontSize=11.sp) }; Text("${e.money}$",color=Blue,fontWeight=FontWeight.Bold) } }
