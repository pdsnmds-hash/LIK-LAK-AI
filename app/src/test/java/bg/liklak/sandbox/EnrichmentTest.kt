package bg.liklak.sandbox

import com.google.gson.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EnrichmentTest {
 private fun read(path: String)=listOf(File(path),File("../$path")).first{it.exists()}.readText()
 private fun books()=Gson().fromJson(read("app/src/main/assets/handbook.json"),Array<Handbook>::class.java).associateBy{it.id}
 private fun incoming()=JsonParser.parseString(read("content/enrichment.json")).asJsonObject
 @Test fun catalogDistinguishesTwelveRunnersFromTwentyFourReadingScenarios() {
  val cases=Gson().fromJson(read("app/src/main/assets/cases.json"),Array<LabCase>::class.java).toList()
  val catalog=buildCatalog(cases,books(),emptyMap())
  assertEquals(36,catalog.size);assertEquals(36,catalog.map{it.id}.distinct().size)
  assertEquals(cases.map{it.id}.toSet(),catalog.filter{it.interactive}.map{it.id}.toSet())
  assertEquals(24,catalog.count{!it.interactive})
  assertTrue(catalog.any{it.category=="analytics" && !it.interactive})
  assertTrue(catalog.any{it.level==3 && !it.interactive})
  assertTrue(catalog.first{it.id=="13-meeting"}.matches("whisper",true,books()))
 }
 @Test fun allUploadedScenariosAndIllustrationsArePreserved() {
  val books=books()
  incoming().getAsJsonArray("scenarios").forEach {row->
   val s=row.asJsonObject;val b=books.getValue(s["id"].asString)
   assertEquals("scenario",b.type())
   assertEquals(s["title_bg"].asString,b.title(false));assertEquals(s["title_en"].asString,b.title(true))
   assertEquals(s["input"],JsonParser.parseString(b.sections.first{it.id=="input"}.code))
   assertEquals(s["illustrative_output"],JsonParser.parseString(b.sections.first{it.id=="output"}.code))
   assertEquals(s["workflow_bg"].asString,b.sections.first{it.id=="workflow"}.body(false))
   assertEquals(s["acceptance_en"].asString,b.sections.first{it.id=="acceptance"}.body(true))
   assertTrue(b.sections.none{it.runnable});assertTrue(b.sections.all{it.expected.isEmpty()})
   assertEquals(s["tools"].asJsonArray.map{it.asString},b.tool_ids)
  }
 }
 @Test fun allTwelveExtensionsKeepTheirOriginalCaseAndSectionIds() {
  val books=books()
  incoming().getAsJsonArray("extensions").forEach {row->
   val e=row.asJsonObject;val b=books.getValue(e["case_id"].asString)
   val s=b.sections.single{it.id==e["section_id"].asString}
   assertEquals(e["body_bg"].asString,s.body(false));assertEquals(e["body_en"].asString,s.body(true))
   assertEquals(e["solution_bg"].asString,s.solution(false));assertFalse(s.runnable)
  }
 }
 @Test fun toolsPromptsRecipesAndSupplementsAreDiscoverableWithoutDuplication() {
  val books=books()
  assertEquals(110,books.size);assertEquals(503,books.values.sumOf{it.sections.size})
  assertEquals(36,resourceBooks(books,"tool").size)
  assertEquals(16,resourceBooks(books,"recipe").size)
  assertEquals(16,resourceBooks(books,"prompt").size)
  assertEquals(42,resourceBooks(books,"library").size)
  assertEquals(5,resourceBooks(books,"supplement").size)
  assertEquals(32,books.getValue("glossary").sections.size)
  assertEquals(18,books.getValue("diagnostics").sections.size)
  incoming().getAsJsonArray("tools").forEach{row->
   val t=row.asJsonObject;val b=books.getValue("tool-"+t["id"].asString)
   assertEquals(t["purpose_bg"].asString,b.sections.first{it.id=="purpose"}.body(false))
   assertEquals(t["first_task_en"].asString,b.sections.first{it.id=="first-task"}.body(true))
   assertEquals(t["url"].asString,b.references.single().url)
  }
 }
 @Test fun copiedPromptUsesTheSelectedLanguageAndPreservesVariables() {
  val books=books();val variables=Regex("\\{\\{(\\w+)\\}\\}")
  incoming().getAsJsonArray("prompts").forEach{row->
   val p=row.asJsonObject;val c=books.getValue("prompt-"+p["id"].asString).sections.first()
   assertEquals("prompt",c.language);assertFalse(c.runnable)
   assertEquals(p["prompt_bg"].asString,c.code(false));assertEquals(p["prompt_en"].asString,c.code(true))
   val expected=p["variables"].asJsonArray.map{it.asString}.toSet()
   listOf(false,true).forEach{en->assertEquals(expected,variables.findAll(c.code(en)).map{it.groupValues[1]}.toSet())}
  }
 }
 @Test fun allSixteenImportedRecipesRetainExactVerifiedCodeAndStdout() {
  val books=books()
  incoming().getAsJsonArray("recipes").forEach{row->
   val r=row.asJsonObject;val c=books.getValue("recipe-"+r["id"].asString).sections.single()
   assertTrue(c.runnable);assertEquals(r["code"].asString,c.code)
   assertEquals(r["expected"].asString,c.expected)
  }
 }
}
