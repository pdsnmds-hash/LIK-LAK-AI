package bg.liklak.sandbox

import com.google.gson.Gson

data class PlanProcess(val id:String,val bg:String,val en:String,val lessons:List<String>,val inputBg:String,val inputEn:String,val outputBg:String,val outputEn:String)
val planProcesses=listOf(
 PlanProcess("requests","Обработка на запитвания","Handle requests",listOf("01-routing","02-replies","33-approval"),"Запитвания от обща поща","Requests from a shared inbox","Категория и чернова на отговор","Category and a draft reply"),
 PlanProcess("documents","Работа с документи","Process documents",listOf("11-documents","23-ocr-review","05-extraction"),"Текст от документи без лични данни","Document text without personal data","Проверени полета и списък с пропуски","Verified fields and a list of gaps"),
 PlanProcess("data","Таблици и CRM","Tables and CRM",listOf("06-cleaning","14-crm","recipe-csv"),"Експорт на таблица с тестови записи","A table export with test records","Почистени записи и отчет за грешки","Clean records and an error report"),
 PlanProcess("development","Разработка и качество","Development and quality",listOf("07-tests","08-review","18-debugging"),"Малка функция и очаквано поведение","A small function and expected behavior","Тестове и предложения за промени","Tests and proposed changes")
)
data class PlanLesson(val articleId:String,val instruction:String)
data class AutomationPlan(val id:String="",val process:String="requests",val title:String="",val input:String="",val output:String="",val reviewer:String="",val check:String="",val execution:String="local") {
 fun valid()=listOf(title,input,output,reviewer,check).all{it.trim().isNotEmpty() && it.length<=1200} && planProcesses.any{it.id==process} && execution in listOf("local","cloud")
 fun processInfo()=planProcesses.first{it.id==process}
 fun json()=Gson().toJson(this)
 companion object {fun decode(value:String):AutomationPlan?=runCatching{Gson().fromJson(value,AutomationPlan::class.java).takeIf{it.valid()}}.getOrNull()}
 fun processing(en:Boolean)=when(process){
  "requests"->tr(en,"Категоризирайте по явни признаци; отделете смесените заявки за преглед. Черновата използва само одобрена политика.","Categorize by explicit signals; send mixed requests for review. Draft replies use only approved policy.")
  "documents"->tr(en,"Първо извлечете текста и източника. Валидирайте задължителните полета, формата и сумите; маркирайте нечетливите стойности.","Extract text and its source first. Validate required fields, formats and totals; flag unreadable values.")
  "data"->tr(en,"Фиксирайте схема на колоните. Нормализирайте стойностите, открийте дубликати по избран ключ и отделете невалидните редове.","Fix the column schema. Normalize values, detect duplicates using a chosen key and separate invalid rows.")
  else->tr(en,"Опишете очакваното поведение. Добавете тест за дефекта и граничните случаи; прегледайте минималната промяна преди сливане.","Specify expected behavior. Add a test for the defect and edge cases; review the minimal change before merging.")
 }
 fun steps(en:Boolean)=listOf(
  tr(en,"Подгответе 10 тестови примера без чувствителни данни. Запазете оригиналите.","Prepare 10 test examples without sensitive data. Keep the originals."),
  if(execution=="local")tr(en,"Опишете явни правила и валидирайте входа. Използвайте локалния пример от материалите.","Define explicit rules and validate the input. Use the local example in the linked materials.") else tr(en,"Подгответе отделна сървърна интеграция с модел: схема на отговора, лимит на разходите и изчакване. API ключът остава на сървъра.","Prepare a separate server-side model integration: response schema, cost limit and timeout. Keep the API key on the server."),
  processing(en),
  tr(en,"Сравнете всеки резултат с очакванията: ","Compare every result with the expected outcome: ")+check,
  tr(en,"Изпратете предложението за проверка на: ","Send the proposal for review by: ")+reviewer,
  tr(en,"Проведете малък пилот. Записвайте корекции, грешки и време за проверка преди разширяване.","Run a small pilot. Record corrections, errors and review time before expanding.")
 )
 fun stops(en:Boolean)=listOf(tr(en,"Липсващ или невалиден вход → спрете и поискайте корекция.","Missing or invalid input → stop and request a correction."),tr(en,"Неподкрепен факт, противоречие или неуспешна проверка → преглед от човек.","Unsupported fact, conflict or failed check → human review."),if(execution=="cloud")tr(en,"Изчакване, невалиден JSON или достигнат бюджет → без автоматичен повторен запис.","Timeout, invalid JSON or exhausted budget → no automatic write retry.")else tr(en,"Случай извън правилата → без предположение; предайте на проверяващия.","A case outside the rules → do not guess; hand it to the reviewer."),tr(en,"Без одобрение → без изпращане или промяна на реални записи.","Without approval → no sending or changes to real records."))
 fun route(en:Boolean):List<PlanLesson> {
  val lessons=processInfo().lessons
  val result=mutableListOf(
   PlanLesson(lessons[0],tr(en,"Приложете примера към входа: ","Apply the example to your input: ")+input),
   PlanLesson(lessons[1],processing(en)),
   PlanLesson(lessons[2],tr(en,"Подгответе резултата: ","Prepare the output: ")+output)
  )
  if(execution=="cloud")result+=PlanLesson("19-api-contract",tr(en,"Опишете входната и изходната схема, невалидните отговори и изчакването преди свързване на модел.","Define input/output schemas, invalid responses and timeouts before connecting a model."))
  result+=PlanLesson("10-evaluation",tr(en,"Създайте тестови примери и проверете: ","Create test examples and verify: ")+check)
  if(lessons.none{it=="33-approval"})result+=PlanLesson("33-approval",tr(en,"Определете одобрението на точния резултат. Проверяващ: ","Define approval of the exact output. Reviewer: ")+reviewer)
  result+=PlanLesson("36-pilot",tr(en,"Съберете стъпките в малък пилот. Измервайте грешките и времето за проверка; спрете при неуспешен критерий: ","Combine the steps into a small pilot. Measure errors and review time; stop when this criterion fails: ")+check)
  return result
 }
 fun export(en:Boolean):String=buildString {
  appendLine("LIK LAK / "+tr(en,"ПЛАН ЗА АВТОМАТИЗАЦИЯ","AUTOMATION PLAN"));appendLine(title)
  appendLine(tr(en,"Процес: ","Process: ")+if(en)processInfo().en else processInfo().bg)
  appendLine(tr(en,"Вход: ","Input: ")+input);appendLine(tr(en,"Резултат: ","Output: ")+output)
  appendLine(tr(en,"Проверяващ: ","Reviewer: ")+reviewer);appendLine(tr(en,"Критерий: ","Acceptance check: ")+check)
  appendLine(tr(en,"Подход: ","Approach: ")+execution)
  appendLine("\n"+tr(en,"СТЪПКИ","STEPS"));route(en).forEachIndexed{i,s->appendLine("${i+1}. ${s.instruction} [${s.articleId}]")}
  appendLine("\n"+tr(en,"УСЛОВИЯ ЗА СПИРАНЕ","STOP CONDITIONS"));stops(en).forEach{appendLine("• $it")}
  appendLine("\n"+tr(en,"Материали: ","Materials: ")+processInfo().lessons.joinToString(", "))
  appendLine(tr(en,"Работна заготовка за пилот; интеграцията не е изпълнена от приложението.","A working pilot outline; the app has not executed the integration."))
 }
}
