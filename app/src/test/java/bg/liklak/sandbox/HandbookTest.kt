package bg.liklak.sandbox

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URI

class HandbookTest {
 private fun asset(name: String) = listOf(File("src/main/assets/$name"),File("app/src/main/assets/$name")).first{it.exists()}.readText()
 private fun books() = Gson().fromJson(asset("handbook.json"),Array<Handbook>::class.java).toList()
 @Test fun everyCaseHasASubstantialBilingualGuideAndSolvedExercise() {
  val cases=Gson().fromJson(asset("cases.json"),Array<LabCase>::class.java)
  val books=books()
  val originalIds=cases.map{it.id}.toSet()+"foundations"
  assertTrue(books.map{it.id}.containsAll(originalIds))
  assertEquals(books.size,books.map{it.id}.distinct().size)
  books.filter{it.id in originalIds}.forEach {book->
   assertTrue(book.sections.size>=8)
   assertEquals(book.sections.size,book.sections.map{it.id}.distinct().size)
   listOf(false,true).forEach {en->
    assertTrue(book.title(en).isNotBlank())
    assertTrue(book.intro(en).isNotBlank())
    book.sections.forEach{assertTrue(it.title(en).isNotBlank());assertTrue(it.body(en).length>100)}
    val wordCount=book.sections.sumOf{it.body(en).split(Regex("\\s+")).size}
    assertTrue("${book.id} substantial ${if(en)"EN" else "BG"} prose",wordCount>=400)
    assertTrue(book.sections.any{it.solution(en).length>100})
   }
  }
 }
 @Test fun recipeOutputsArePresentOnlyForExecutedLocalRecipes() {
  val recipes=books().flatMap{it.sections}.filter{it.runnable}
  assertEquals(29,recipes.size)
  recipes.forEach{assertEquals("python",it.language);assertTrue(it.code.contains("assert "));assertTrue(it.expected.isNotBlank())}
  val originals=books().filter{it.type() in setOf("case","foundation")}.flatMap{it.sections}.filter{it.runnable}
  assertEquals(13,originals.size)
  originals.forEach{val output=JsonParser.parseString(it.expected);assertTrue(output.isJsonObject || output.isJsonArray)}
  assertTrue(books().flatMap{it.sections}.filter{!it.runnable}.all{it.expected.isEmpty()})
  assertTrue(books().first{it.id=="foundations"}.sections.any{it.code.contains("urlopen") && !it.runnable})
 }
 @Test fun deepSearchFindsAConceptAbsentFromTheCaseTitle() {
  val books=books().associateBy{it.id}
  assertTrue(books.getValue("04-knowledge").matches("cosine",true))
  assertTrue(books.getValue("04-knowledge").matches("източник",false))
  assertTrue(books.getValue("08-review").sections.any{it.matches("ast.Attribute",true) && it.solution(true).contains("ast.Attribute")})
  assertFalse(books.getValue("02-replies").matches("impossible_search_829147",true))
 }
 @Test fun everyGuideHasOfficialReferences() {
  val hosts=setOf("docs.python.org","docs.ollama.com","scikit-learn.org","developers.google.com")
  books().forEach{book->
   assertTrue(book.references.isNotEmpty())
   book.references.forEach{ref->val uri=URI(ref.url);assertEquals("https",uri.scheme);assertTrue(uri.host.isNotBlank());if(book.type() in setOf("case","foundation"))assertTrue(uri.host in hosts);assertTrue(ref.title.isNotBlank())}
  }
 }
}
