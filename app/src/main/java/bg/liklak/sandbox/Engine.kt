package bg.liklak.sandbox

import com.google.gson.*
import java.util.Locale

/** Deterministic algorithms only. Never executes source code or performs external actions. */
object Engine {
 private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
 fun pretty(value: JsonElement): String = gson.toJson(value)
 private fun json(value: Any): JsonElement = gson.toJsonTree(value)
 fun run(id: String, input: JsonElement, en: Boolean = true): JsonElement {
  fun text() = input.asString
  fun field(name: String) = input.asJsonObject.get(name)?.asString ?: ""
  return when(id) {
   "01-routing" -> {
    val text = text().lowercase(Locale.ROOT)
    val rules = linkedMapOf("billing" to listOf("invoice","payment"),"support" to listOf("error","broken"),"hr" to listOf("onboarding","leave"))
    val hits = rules.filterValues { words -> words.any { it in text } }.keys.toList()
    json(mapOf("category" to (if(hits.size==1) hits[0] else if(hits.isEmpty()) "other" else "needs_review"),"action" to "review_by_operator"))
   }
   "02-replies" -> {
    val policy = field("policy"); require(policy.isNotBlank()) { "policy" }
    val formal = field("tone") == "formal"
    val known = field("intent") in listOf("delivery","billing","support")
    val start = tr(en, if(formal) "Thank you for contacting our team." else "Thank you for your request.", if(formal) "Thank you for contacting our team." else "Thank you for your request.")
    val end = if(known) tr(en,"Please specify the required service scope.","Please specify the required service scope.") else tr(en,"Please clarify your question for an operator to review.","Please clarify your question for an operator to review.")
    json(mapOf("draft" to "$start $policy $end", "status" to "awaiting_review"))
   }
   "03-sop" -> {
    val steps = text().split(';').map { it.trim() }.filter { it.isNotEmpty() }.mapIndexed { index, s -> mapOf("order" to index+1, "action" to s) }
    require(steps.isNotEmpty()) { "steps" }
    json(mapOf("title" to tr(en,"Request workflow","Request workflow"),"steps" to steps,"unresolved_questions" to listOf(tr(en,"Who approves the draft?","Who approves the draft?"))))
   }
   "04-knowledge" -> {
    val kb = mapOf("KB-01" to "Documents are organized in folders by process.", "KB-02" to "Customer requests need an assigned status.")
    fun tokens(s: String) = Regex("[\\p{L}\\p{N}_]+").findAll(s.lowercase(Locale.ROOT)).map{it.value}.toSet()
    val query = tokens(text())
    val best = kb.entries.map { Triple(query.intersect(tokens(it.value)).size,it.key,it.value) }.maxWith(compareBy<Triple<Int,String,String>> { it.first }.thenBy { it.second })
    if(best.first==0) json(mapOf("status" to "not_found")) else json(mapOf("source_id" to best.second,"excerpt" to best.third))
   }
   "05-extraction" -> {
    val quantity = Regex("(\\d+) items").find(text())?.groupValues?.get(1)?.toLongOrNull()
    val category = Regex("category: (office|support|data)\\b").find(text())?.groupValues?.get(1)
    json(mapOf("quantity" to quantity,"category" to category,"valid" to (quantity!=null && quantity in 1..10000 && category!=null)))
   }
   "06-cleaning" -> {
    val normalized = input.asJsonArray.map { it.asString.trim().uppercase(Locale.ROOT) }
    val valid = normalized.filter { it.isNotEmpty() }; val unique = valid.distinct()
    json(mapOf("unique" to unique,"duplicates" to valid.size-unique.size,"empty" to normalized.count {it.isEmpty()}))
   }
   "07-tests" -> {
    val limit = input.asJsonObject.get("limit")?.asInt ?: 80
    require(limit in 1..500) { "limit" }
    val tests = listOf("Plan".take(limit),"   ","a".repeat(limit),"a".repeat(limit+1))
    val expected = listOf(true,false,true,false)
    val results = tests.mapIndexed { i,s -> val size = s.codePointCount(0,s.length); mapOf("length" to size,"expected" to expected[i],"actual" to (s.isNotBlank() && size<=limit)) }
    json(mapOf("cases" to results.size,"passed" to results.count{it["expected"]==it["actual"]},"results" to results))
   }
   "08-review" -> {
    require(text() in listOf("value = eval(expression)","value = int(\"12\")","x = 1\nvalue = eval(expression)")) { "snippet" }
    val findings = text().lines().mapIndexedNotNull { i,line -> if(Regex("\\beval\\s*\\(").containsMatchIn(line)) mapOf("line" to i+1,"rule" to "dynamic_eval","severity" to "high") else null }
    json(mapOf("findings" to findings))
   }
   "09-brief" -> {
    require(listOf("process","input","output").all { field(it).isNotBlank() }) { "brief" }
    json(mapOf("scope" to field("process"),"input" to field("input"),"output" to field("output"),"execution" to field("execution").ifEmpty {"local"},"human_approval" to true,"exclusions" to listOf(tr(en,"Automatic external actions","Automatic external actions")),"acceptance" to listOf(tr(en,"Valid category","Valid category"),tr(en,"Reason references input","Reason references input"))))
   }
   "10-evaluation" -> {
    val expected = input.asJsonObject.getAsJsonArray("expected"); val predicted = input.asJsonObject.getAsJsonArray("predicted")
    require(expected.size()==predicted.size() && expected.size()>0) { "arrays" }
    val errors = expected.mapIndexedNotNull { i,e -> if(e!=predicted[i]) mapOf("index" to i,"expected" to e.asString,"predicted" to predicted[i].asString) else null }
    val correct = expected.size()-errors.size
    json(mapOf("correct" to correct,"total" to expected.size(),"accuracy" to correct.toDouble()/expected.size(),"errors" to errors))
   }
   "11-documents" -> {
    val ref = Regex("Reference: ([A-Z0-9-]+)").find(text())?.groupValues?.get(1)
    val total = Regex("Total: (\\d+\\.\\d{2}) (EUR|USD|BGN)\\b").find(text())?.groupValues
    json(mapOf("reference" to ref,"total" to total?.get(1),"currency" to total?.get(2)))
   }
   "12-workflow" -> {
    val approved = input.asJsonObject.get("approved")?.asBoolean ?: false
    val state = if(field("category") !in listOf("billing","support","hr","other")) "invalid" else if(!approved) "awaiting_approval" else "ready_for_manual_export"
    json(mapOf("state" to state,"external_action" to false))
   }
   else -> error("case")
  }
 }
 fun variants(c: LabCase): List<JsonElement> {
  val extra = when(c.id) {
   "01-routing" -> listOf("\"invoice error\"","\"onboarding\"","\"\"")
   "02-replies" -> listOf("""{"intent":"billing","policy":"Payment terms require confirmation.","tone":"formal"}""","""{"intent":"unknown","policy":"An operator will review the request."}""","""{"intent":"delivery","policy":""}""")
   "03-sop" -> listOf("\"Receive;; review; approve\"","\"\"")
   "04-knowledge" -> listOf("\"Customer requests status\"","\"volcano astronomy\"")
   "05-extraction" -> listOf("\"category: office\"","\"Package: 0 items; category: office\"","\"Package: 12 items; category: unknown\"")
   "06-cleaning" -> listOf("""[" A ","a","B","",""]""","[]")
   "07-tests" -> listOf("""{"limit":20}""","""{"limit":0}""")
   "08-review" -> listOf("\"value = int(\\\"12\\\")\"","\"x = 1\\nvalue = eval(expression)\"")
   "09-brief" -> listOf("""{"process":"Request routing","input":"Messages","output":"Category","execution":"server"}""")
   "10-evaluation" -> listOf("""{"expected":["billing","hr"],"predicted":["billing","hr"]}""","""{"expected":["billing"],"predicted":[]}""","""{"expected":[],"predicted":[]}""")
   "11-documents" -> listOf("\"Reference: DOC-043\\nTotal: 90.50 USD\"","\"Total: 10.00 GBP\"")
   "12-workflow" -> listOf("""{"category":"billing","approved":true}""","""{"category":"unknown","approved":true}""")
   else -> emptyList()
  }
  val first = if(c.id=="07-tests") JsonParser.parseString("""{"limit":80}""") else c.input
  return listOf(first)+extra.map { JsonParser.parseString(it) }
 }
}
