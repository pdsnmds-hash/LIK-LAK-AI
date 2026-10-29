package bg.liklak.sandbox
import org.junit.Assert.*
import org.junit.Test
class PlanRouteTest {
 @Test fun everyRouteIncludesProcessLessonsEvaluationAndPilot(){
  planProcesses.forEach{process->listOf("local","cloud").forEach{execution->
   val p=AutomationPlan("test",process.id,"Plan","Source sample","Checked output","Reviewer","No missing values",execution)
   for(en in listOf(false,true)){
    val steps=p.route(en)
    assertEquals(process.lessons,steps.take(3).map{it.articleId})
    assertTrue(steps.any{it.articleId=="10-evaluation"})
    assertTrue(steps.any{it.articleId=="33-approval"})
    assertEquals("36-pilot",steps.last().articleId)
    assertEquals(execution=="cloud",steps.any{it.articleId=="19-api-contract"})
    assertTrue(steps.all{it.instruction.isNotBlank()})
    assertTrue(p.export(en).contains("Source sample"));assertTrue(p.export(en).contains("36-pilot"))
   }
  }}
 }
}
