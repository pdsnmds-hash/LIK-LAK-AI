package bg.liklak.sandbox

import com.google.gson.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EngineTest {
 private val gson=Gson()
 private fun run(id: String,value: Any)=Engine.run(id,gson.toJsonTree(value)).asJsonObject
 @Test fun routingAbstainsAndNeverInventsConfidence() {
  assertEquals("needs_review",run("01-routing","invoice error")["category"].asString)
  assertEquals("other",run("01-routing","")["category"].asString)
  assertEquals("hr",run("01-routing","ONBOARDING")["category"].asString)
  assertFalse(run("01-routing","invoice").has("confidence"))
 }
 @Test fun supportRequiresPolicyAndUsesChangedPolicy() {
  assertThrows(IllegalArgumentException::class.java){run("02-replies",mapOf("policy" to ""))}
  val result=run("02-replies",mapOf("policy" to "Review required.","intent" to "delivery"))
  assertTrue(result["draft"].asString.contains("Review required."))
  assertEquals("awaiting_review",result["status"].asString)
 }
 @Test fun sopDropsBlankSeparatorsPreservesOrder() {
  val steps=run("03-sop","Receive;; approve;")["steps"].asJsonArray
  assertEquals(2,steps.size());assertEquals("approve",steps[1].asJsonObject["action"].asString)
 }
 @Test fun searchHasNoInventedSource() {
  assertEquals("KB-01",run("04-knowledge","documents")["source_id"].asString)
  val missing=run("04-knowledge","astronomy volcano")
  assertEquals("not_found",missing["status"].asString);assertFalse(missing.has("source_id"))
 }
 @Test fun extractionPreservesNullAndRejectsInvalidValues() {
  val missing=run("05-extraction","category: office")
  assertTrue(missing["quantity"].isJsonNull);assertFalse(missing["valid"].asBoolean)
  assertFalse(run("05-extraction","0 items; category: office")["valid"].asBoolean)
  assertFalse(run("05-extraction","12 items; category: unknown")["valid"].asBoolean)
  assertEquals(12,run("05-extraction","12 items; category: office")["quantity"].asInt)
 }
 @Test fun cleaningCountsDuplicatesAndEmpties() {
  val result=run("06-cleaning",listOf(" A ","a","", "B", ""))
  assertEquals(1,result["duplicates"].asInt);assertEquals(2,result["empty"].asInt)
  assertEquals(listOf("A","B"),result["unique"].asJsonArray.map{it.asString})
 }
 @Test fun boundaryCasesUseSelectedLimit() {
  val result=run("07-tests",mapOf("limit" to 20))
  assertEquals(4,result["passed"].asInt)
  assertEquals(21,result["results"].asJsonArray[3].asJsonObject["length"].asInt)
 }
 @Test fun staticReviewUsesOnlyAllowlistedSnippets() {
  assertEquals(1,run("08-review","value = eval(expression)")["findings"].asJsonArray.size())
  assertEquals(0,run("08-review","value = int(\"12\")")["findings"].asJsonArray.size())
  assertThrows(IllegalArgumentException::class.java){run("08-review","import os; os.remove('x')")}
 }
 @Test fun briefContainsExplicitApprovalAndExclusions() {
  val result=run("09-brief",mapOf("process" to "Routing","input" to "Messages","output" to "Category"))
  assertTrue(result["human_approval"].asBoolean);assertTrue(result["exclusions"].asJsonArray.size()>0)
 }
 @Test fun evaluationShowsRealNumeratorAndRejectsMismatchedOrEmptyArrays() {
  val result=run("10-evaluation",mapOf("expected" to listOf("a","b","c","d"),"predicted" to listOf("a","b","x","d")))
  assertEquals(3,result["correct"].asInt);assertEquals(4,result["total"].asInt);assertEquals(0.75,result["accuracy"].asDouble,0.0)
  assertEquals(1,result["errors"].asJsonArray.size())
  assertThrows(IllegalArgumentException::class.java){run("10-evaluation",mapOf("expected" to listOf("a"),"predicted" to emptyList<String>()))}
  assertThrows(IllegalArgumentException::class.java){run("10-evaluation",mapOf("expected" to emptyList<String>(),"predicted" to emptyList<String>()))}
 }
 @Test fun documentExtractionKeepsDecimalAndDoesNotGuessCurrency() {
  assertEquals("125.00",run("11-documents","Total: 125.00 EUR")["total"].asString)
  assertTrue(run("11-documents","Total: 10.00 GBP")["currency"].isJsonNull)
 }
 @Test fun workflowNeverPerformsExternalAction() {
  for(approved in listOf(false,true)){
   val result=run("12-workflow",mapOf("category" to "billing","approved" to approved))
   assertFalse(result["external_action"].asBoolean)
   assertEquals(if(approved)"ready_for_manual_export" else "awaiting_approval",result["state"].asString)
  }
  assertEquals("invalid",run("12-workflow",mapOf("category" to "unknown","approved" to true))["state"].asString)
 }
 @Test fun everyCaseHasBothGuidesAndAllVariantsParse() {
  val path=listOf(File("src/main/assets/cases.json"),File("app/src/main/assets/cases.json")).first{it.exists()}
  val cases=gson.fromJson(path.readText(),Array<LabCase>::class.java).toList()
  assertEquals(12,cases.size);assertEquals(12,cases.map{it.id}.toSet().size)
  cases.forEach {c->
   listOf(c.bg,c.en).forEach {g->assertTrue(g.problem.isNotBlank());assertEquals(5,g.steps.size);assertEquals(3,g.acceptance.size);assertTrue(g.help.isNotBlank());assertTrue(g.limitations.isNotBlank())}
   assertTrue(c.code.contains("import"));assertTrue(c.prompt.isNotBlank());assertTrue(Engine.variants(c).size>=2)
  }
 }
 @Test fun defaultAlgorithmsMatchReferenceOutputs() {
  val path=listOf(File("src/main/assets/cases.json"),File("app/src/main/assets/cases.json")).first{it.exists()}
  val cases=JsonParser.parseString(path.readText()).asJsonArray
  cases.forEach {element->
   val c=element.asJsonObject;val id=c["id"].asString
   val input=if(id=="07-tests")JsonParser.parseString("""{"limit":80}""") else c["input"]
   val actual=Engine.run(id,input).asJsonObject
   c["actual_output"].asJsonObject.entrySet().forEach{(key,value)->assertEquals("$id/$key",value,actual[key])}
  }
 }
}
