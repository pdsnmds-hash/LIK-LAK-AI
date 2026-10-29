package bg.liklak.sandbox

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UiContentTest {
 private fun asset(name: String)=listOf(File("src/main/assets/$name"),File("app/src/main/assets/$name")).first{it.exists()}.readText()
 @Test fun everyVariantHasAShortBilingualExplanation() {
  val cases=Gson().fromJson(asset("cases.json"),Array<LabCase>::class.java)
  cases.forEach{c->Engine.variants(c).indices.forEach{i->listOf(false,true).forEach{en->
   val note=variantNote(c.id,i,en)
   assertTrue("${c.id}/$i",note.isNotBlank() && note.length<=180)
  }}}
  assertTrue(variantNote("01-routing",2,true).contains("onboarding"))
  assertTrue(variantNote("11-documents",0,true).contains("not OCR"))
 }
 @Test fun eachLearningPathResolvesToItsActualLessonsInOrder() {
  val books=Gson().fromJson(asset("handbook.json"),Array<Handbook>::class.java).associateBy{it.id}
  val expected=mapOf(
   "python" to listOf("01-routing","06-cleaning","05-extraction","recipe-money","recipe-csv"),
   "ai" to listOf("02-replies","04-knowledge","10-evaluation","12-workflow","33-approval"),
   "developer" to listOf("07-tests","08-review","18-debugging","19-api-contract","21-ci-triage","recipe-idempotency"),
   "operations" to listOf("13-meeting","14-crm","15-invoice","36-pilot"),
   "documents" to listOf("11-documents","23-ocr-review","24-policy-diff","32-rag-access","recipe-retrieval"))
  books.getValue("learning-paths").sections.forEach{c->
   val ids=learningPathIds(c,books)
   assertEquals(expected.getValue(c.id),ids)
   assertTrue(ids.all{books.getValue(it).sections.isNotEmpty()})
  }
 }
}
