package bg.liklak.sandbox

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun PlanResult(p:AutomationPlan,books:Map<String,Handbook>,en:Boolean,saved:Boolean,feedback:PlanSaveFeedback,onSave:()->Unit,onEdit:()->Unit,onRead:(String)->Unit,onAbout:()->Unit) {
 val context=LocalContext.current
 var lastSaveEvent by rememberSaveable(p.id){mutableLongStateOf(feedback.event)}
 var playSave by remember(p.id){mutableStateOf(false)}
 var animationEvent by remember(p.id){mutableLongStateOf(0)}
 LaunchedEffect(feedback.event,feedback.status){
  if(feedback.planId==p.id && feedback.status=="success" && feedback.event>lastSaveEvent){lastSaveEvent=feedback.event;animationEvent=feedback.event;playSave=true}
 }
 var export by rememberSaveable{mutableStateOf(false)}
 var exportError by remember{mutableStateOf(false)}
 val writeFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->if(uri!=null){exportError=runCatching{context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use{it.write(p.export(en))}}.isFailure}}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {PageTitle(tr(en,"YOUR / PLAN","YOUR / PLAN"),p.title,tr(en,"Follow the steps and open the linked articles with code.","Follow the steps and open the linked articles with code."))}
  item {Panel(Modifier.fillMaxWidth()){Kicker(tr(en,"YOUR TASK","YOUR TASK"));Body(tr(en,"Input: ","Input: ")+p.input);Body(tr(en,"Output: ","Output: ")+p.output);Body(tr(en,"Check: ","Check: ")+p.check);Body(tr(en,"Reviewer: ","Reviewer: ")+p.reviewer)}}
  p.route(en).forEachIndexed{i,step->item{val book=books.getValue(step.articleId);Panel(Modifier.fillMaxWidth()){Kicker("${i+1} / ${p.route(en).size}");Text(book.title(en),fontSize=responsiveSp(24),lineHeight=responsiveSp(29));Body(step.instruction);PrimaryActionButton({onRead(step.articleId)},Modifier.fillMaxWidth()){Text(tr(en,"Open article →","Open article →"))}}}}
  item {
   val saving=feedback.planId==p.id && feedback.status=="saving"
   if(saved) SaveConfirmation(animationEvent,playSave,en){playSave=false}
   if(feedback.planId==p.id && feedback.status=="error")Body(tr(en,"Could not save. Please try again.","Could not save. Please try again."))
   PrimaryActionButton(onSave,Modifier.fillMaxWidth(),enabled=!saving){Text(if(saving)tr(en,"Saving…","Saving…")else if(saved)tr(en,"✓ Plan saved","✓ Plan saved")else tr(en,"Save plan","Save plan"))}
  }
  item {ActionButton({export=true},Modifier.fillMaxWidth()){Text(tr(en,"Preview and export","Preview and export"))}}
  item {ActionButton(onEdit,Modifier.fillMaxWidth()){Text(tr(en,"Edit answers","Edit answers"))}}
  item {ActionButton(onAbout,Modifier.fillMaxWidth()){Text(tr(en,"Contacts and information →","Contacts and information →"))}}
 }
 if(export) androidx.compose.ui.window.Dialog(onDismissRequest={export=false}) {Surface(shape=MaterialTheme.shapes.large){Column(Modifier.padding(LocalUiSizing.current.panelPadding),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(tr(en,"Plan preview","Plan preview"),fontSize=responsiveSp(24),lineHeight=responsiveSp(29));Box(Modifier.weight(1f,false).heightIn(max=400.dp).verticalScroll(rememberScrollState())){Text(p.export(en))};if(exportError)Text(tr(en,"Could not write the file. Please try again.","Could not write the file. Please try again."));ActionButton({writeFile.launch("LIK-LAK-plan-${p.id.take(8)}.txt")}){Text(tr(en,"Save TXT file","Save TXT file"))};ActionButton({copy(context,p.export(en),en)}){Text(tr(en,"Copy","Copy"))};ActionButton({export=false}){Text(tr(en,"Close","Close"))}}}}
}
