package bg.liklak.sandbox
import org.junit.Assert.*
import org.junit.Test
class AutomationPlanTest {
 private fun sample()=AutomationPlan("test","documents","Invoice review","Test invoice text","Verified fields","Operator","Amounts match source","local")
 @Test fun validationAndPersistence(){val p=sample();assertTrue(p.valid());assertEquals(p,AutomationPlan.decode(p.json()));assertFalse(p.copy(check=" ").valid());assertFalse(p.copy(process="unknown").valid());assertNull(AutomationPlan.decode("{}"));assertFalse(p.copy(title="x".repeat(1201)).valid())}
 @Test fun plansReflectAnswersAndExecution(){for(en in listOf(false,true)){val p=sample();val export=p.export(en);listOf(p.title,p.input,p.output,p.reviewer,p.check).forEach{assertTrue(export.contains(it))};assertEquals(6,p.steps(en).size);assertEquals(4,p.stops(en).size);assertNotEquals(p.steps(en),p.copy(execution="cloud").steps(en));assertNotEquals(p.stops(en),p.copy(execution="cloud").stops(en))}}
}
