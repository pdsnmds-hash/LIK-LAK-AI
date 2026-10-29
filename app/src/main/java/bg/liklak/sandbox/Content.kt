package bg.liklak.sandbox

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

data class Guide(val problem: String, val steps: List<String>, val acceptance: List<String>, val limitations: String, val help: String)
data class LabCase(val id: String, val title_bg: String, val title_en: String, val category: String, val level: Int, val input: JsonElement, val code: String, val prompt: String, val prompt_bg: String, val refs: List<String>, val bg: Guide, val en: Guide) {
 fun title(english: Boolean) = if (english) title_en else title_bg
 fun guide(english: Boolean) = if (english) en else bg
}
data class Source(val title: String, val url: String, val bg: String, val en: String, val pricing: String)
class Content(context: Context) {
 val handbooks: Map<String,Handbook> = Gson().fromJson(context.assets.open("handbook.json").bufferedReader().use { it.readText() }, Array<Handbook>::class.java).associateBy {it.id}
 val cases: List<LabCase> = Gson().fromJson(context.assets.open("cases.json").bufferedReader().use { it.readText() }, object: TypeToken<List<LabCase>>(){}.type)
 val sources: Map<String, Source> = Gson().fromJson(context.assets.open("sources.json").bufferedReader().use { it.readText() }, object: TypeToken<Map<String, Source>>(){}.type)
}
fun tr(en: Boolean, bg: String, eng: String) = if(en) eng else bg
fun categoryLabel(c: String, en: Boolean): String = if(en) c.replaceFirstChar { it.uppercase() } else when(c) {
 "analytics" -> "Анализи"; "content" -> "Съдържание"; "operations" -> "Операции"; "customer care" -> "Клиенти"; "administration" -> "Администрация"; "knowledge" -> "Знания"; "data" -> "Данни"; "development" -> "Разработка"; "outsourcing" -> "Външни услуги"; "quality" -> "Качество"; "documents" -> "Документи"; "automation" -> "Автоматизация"; else -> c
}
