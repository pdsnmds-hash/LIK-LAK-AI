package bg.liklak.sandbox

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PlanBuilder(initial:AutomationPlan,en:Boolean,onBack:()->Unit,onBuild:(AutomationPlan)->Unit) {
 var step by rememberSaveable{mutableIntStateOf(0)}
 var process by rememberSaveable{mutableStateOf(initial.process)}
 var title by rememberSaveable{mutableStateOf(initial.title)}
 var input by rememberSaveable{mutableStateOf(initial.input)}
 var output by rememberSaveable{mutableStateOf(initial.output)}
 var reviewer by rememberSaveable{mutableStateOf(initial.reviewer)}
 var check by rememberSaveable{mutableStateOf(initial.check)}
 var execution by rememberSaveable{mutableStateOf(initial.execution)}
 val p=AutomationPlan(initial.id,process,title,input,output,reviewer,check,execution)
 // Keep the transition alive when the progress item scrolls out of the lazy list.
 val stageProgress by animateFloatAsState(step/2f,tween(500),label="Planner stage")
 val listState=rememberLazyListState()
 LaunchedEffect(step){listState.scrollToItem(0)}
 LazyColumn(state=listState,modifier=Modifier.fillMaxSize().imePadding(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {ActionButton({if(step==0)onBack() else step-=1}){Text(tr(en,"← Назад","← Back"))}}
  item {PageTitle("${step+1} / 3",when(step){0->tr(en,"Изберете процес.","Choose a workflow.");1->tr(en,"Опишете задачата.","Describe the task.");else->tr(en,"Определете проверките.","Define the checks.")})}
  item {PlannerProgress(step,en,stageProgress)}
  if(step==0) {
   planProcesses.forEach{option->item {ChoiceButton((if(en)option.en else option.bg),process==option.id,{process=option.id},Modifier.fillMaxWidth())}}
   item {Body(tr(en,"Изберете най-близкия процес. След това ще получите свързани материали и примери.","Choose the closest workflow. You will get related guides and examples."))}
  } else if(step==1) {
   item {Body(tr(en,"Използвайте описание или тестови данни. Не са необходими реални клиентски документи.","Use a description or test data. Real customer documents are not needed."))}
   item {PlanField(tr(en,"Име на плана","Plan name"),title,{title=it})}
   item {PlanField(tr(en,"Какво получавате на вход?","What comes in?"),input,{input=it})}
   item {PlanField(tr(en,"Какъв резултат искате?","What should come out?"),output,{output=it})}
   item {ActionButton({val info=p.processInfo();if(title.isBlank())title=if(en)info.en else info.bg;if(input.isBlank())input=if(en)info.inputEn else info.inputBg;if(output.isBlank())output=if(en)info.outputEn else info.outputBg}){Text(tr(en,"Попълнете пример","Fill an example"))}}
  } else {
   item {PlanField(tr(en,"Кой одобрява резултата?","Who approves the result?"),reviewer,{reviewer=it})}
   item {PlanField(tr(en,"Как ще разберете, че е правилен?","How will you verify it is correct?"),check,{check=it})}
   item {Body(tr(en,"Пример: всички суми съвпадат с източника; липсващите полета са маркирани.","Example: all amounts match the source; missing fields are flagged."))}
   item {ChoiceButton(tr(en,"Локални правила / без API","Local rules / no API"),execution=="local",{execution="local"},Modifier.fillMaxWidth())}
   item {ChoiceButton(tr(en,"План за модел чрез сървърен API","Plan for a model via server API"),execution=="cloud",{execution="cloud"},Modifier.fillMaxWidth())}
   item {Body(tr(en,"Одобрението е задължително. Приложението подготвя план, не свързва системи и не изпраща данни.","Approval is required. The app prepares a plan; it does not connect systems or send data."))}
  }
  item {val ready=when(step){0->true;1->listOf(title,input,output).all{it.isNotBlank()};else->p.valid()}
   if(!ready)Body(tr(en,"Попълнете всички полета, за да продължите.","Complete all fields to continue."))
   PrimaryActionButton({if(step<2)step++ else onBuild(p.copy(id=initial.id.ifEmpty{java.util.UUID.randomUUID().toString()}))},Modifier.fillMaxWidth(),enabled=ready){Text(if(step<2)tr(en,"Продължете →","Continue →")else tr(en,"Създайте план →","Generate plan →"))}
  }
 }
}
@Composable private fun PlanField(label:String,value:String,change:(String)->Unit){OutlinedTextField(value=value,onValueChange={change(it.take(1200))},label={Text(label)},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=6,supportingText={Text("${value.length} / 1200")})}
