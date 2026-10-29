package bg.liklak.sandbox

import com.google.gson.Gson

data class PlanProcess(val id:String,val bg:String,val en:String,val lessons:List<String>,val inputBg:String,val inputEn:String,val outputBg:String,val outputEn:String)
val planProcesses=listOf(
 PlanProcess("requests","Handle requests","Handle requests",listOf("01-routing","02-replies","33-approval"),"Requests from a shared inbox","Requests from a shared inbox","Category and a draft reply","Category and a draft reply"),
 PlanProcess("documents","Process documents","Process documents",listOf("11-documents","23-ocr-review","05-extraction"),"Document text without personal data","Document text without personal data","Verified fields and a list of gaps","Verified fields and a list of gaps"),
 PlanProcess("data","Tables and CRM","Tables and CRM",listOf("06-cleaning","14-crm","recipe-csv"),"A table export with test records","A table export with test records","Clean records and an error report","Clean records and an error report"),
 PlanProcess("development","Development and quality","Development and quality",listOf("07-tests","08-review","18-debugging"),"A small function and expected behavior","A small function and expected behavior","Tests and proposed changes","Tests and proposed changes")
)
data class PlanLesson(val articleId:String,val instruction:String)
data class AutomationPlan(val id:String="",val process:String="requests",val title:String="",val input:String="",val output:String="",val reviewer:String="",val check:String="",val execution:String="local") {
 fun valid()=listOf(title,input,output,reviewer,check).all{it.trim().isNotEmpty() && it.length<=1200} && planProcesses.any{it.id==process} && execution in listOf("local","cloud")
 fun processInfo()=planProcesses.first{it.id==process}
 fun json()=Gson().toJson(this)
 companion object {fun decode(value:String):AutomationPlan?=runCatching{Gson().fromJson(value,AutomationPlan::class.java).takeIf{it.valid()}}.getOrNull()}
 fun processing(en:Boolean)=when(process){
  "requests"->tr(en,"Categorize by explicit signals; send mixed requests for review. Draft replies use only approved policy.","Categorize by explicit signals; send mixed requests for review. Draft replies use only approved policy.")
  "documents"->tr(en,"Extract text and its source first. Validate required fields, formats and totals; flag unreadable values.","Extract text and its source first. Validate required fields, formats and totals; flag unreadable values.")
  "data"->tr(en,"Fix the column schema. Normalize values, detect duplicates using a chosen key and separate invalid rows.","Fix the column schema. Normalize values, detect duplicates using a chosen key and separate invalid rows.")
  else->tr(en,"Specify expected behavior. Add a test for the defect and edge cases; review the minimal change before merging.","Specify expected behavior. Add a test for the defect and edge cases; review the minimal change before merging.")
 }
 fun steps(en:Boolean)=listOf(
  tr(en,"Prepare 10 test examples without sensitive data. Keep the originals.","Prepare 10 test examples without sensitive data. Keep the originals."),
  if(execution=="local")tr(en,"Define explicit rules and validate the input. Use the local example in the linked materials.","Define explicit rules and validate the input. Use the local example in the linked materials.") else tr(en,"Prepare a separate server-side model integration: response schema, cost limit and timeout. Keep the API key on the server.","Prepare a separate server-side model integration: response schema, cost limit and timeout. Keep the API key on the server."),
  processing(en),
  tr(en,"Compare every result with the expected outcome: ","Compare every result with the expected outcome: ")+check,
  tr(en,"Send the proposal for review by: ","Send the proposal for review by: ")+reviewer,
  tr(en,"Run a small pilot. Record corrections, errors and review time before expanding.","Run a small pilot. Record corrections, errors and review time before expanding.")
 )
 fun stops(en:Boolean)=listOf(tr(en,"Missing or invalid input → stop and request a correction.","Missing or invalid input → stop and request a correction."),tr(en,"Unsupported fact, conflict or failed check → human review.","Unsupported fact, conflict or failed check → human review."),if(execution=="cloud")tr(en,"Timeout, invalid JSON or exhausted budget → no automatic write retry.","Timeout, invalid JSON or exhausted budget → no automatic write retry.")else tr(en,"A case outside the rules → do not guess; hand it to the reviewer.","A case outside the rules → do not guess; hand it to the reviewer."),tr(en,"Without approval → no sending or changes to real records.","Without approval → no sending or changes to real records."))
 fun route(en:Boolean):List<PlanLesson> {
  val lessons=processInfo().lessons
  val result=mutableListOf(
   PlanLesson(lessons[0],tr(en,"Apply the example to your input: ","Apply the example to your input: ")+input),
   PlanLesson(lessons[1],processing(en)),
   PlanLesson(lessons[2],tr(en,"Prepare the output: ","Prepare the output: ")+output)
  )
  if(execution=="cloud")result+=PlanLesson("19-api-contract",tr(en,"Define input/output schemas, invalid responses and timeouts before connecting a model.","Define input/output schemas, invalid responses and timeouts before connecting a model."))
  result+=PlanLesson("10-evaluation",tr(en,"Create test examples and verify: ","Create test examples and verify: ")+check)
  if(lessons.none{it=="33-approval"})result+=PlanLesson("33-approval",tr(en,"Define approval of the exact output. Reviewer: ","Define approval of the exact output. Reviewer: ")+reviewer)
  result+=PlanLesson("36-pilot",tr(en,"Combine the steps into a small pilot. Measure errors and review time; stop when this criterion fails: ","Combine the steps into a small pilot. Measure errors and review time; stop when this criterion fails: ")+check)
  return result
 }
 fun export(en:Boolean):String=buildString {
  appendLine("LIK LAK / "+tr(en,"AUTOMATION PLAN","AUTOMATION PLAN"));appendLine(title)
  appendLine(tr(en,"Process: ","Process: ")+if(en)processInfo().en else processInfo().bg)
  appendLine(tr(en,"Input: ","Input: ")+input);appendLine(tr(en,"Output: ","Output: ")+output)
  appendLine(tr(en,"Reviewer: ","Reviewer: ")+reviewer);appendLine(tr(en,"Acceptance check: ","Acceptance check: ")+check)
  appendLine(tr(en,"Approach: ","Approach: ")+execution)
  appendLine("\n"+tr(en,"STEPS","STEPS"));route(en).forEachIndexed{i,s->appendLine("${i+1}. ${s.instruction} [${s.articleId}]")}
  appendLine("\n"+tr(en,"STOP CONDITIONS","STOP CONDITIONS"));stops(en).forEach{appendLine("• $it")}
  appendLine("\n"+tr(en,"Materials: ","Materials: ")+processInfo().lessons.joinToString(", "))
  appendLine(tr(en,"A working pilot outline; the app has not executed the integration.","A working pilot outline; the app has not executed the integration."))
 }
}
