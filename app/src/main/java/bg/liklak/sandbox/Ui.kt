@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package bg.liklak.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*

@Composable fun SandboxApp(vm: LabViewModel) {
 val settings by vm.settings.collectAsStateWithLifecycle()
 val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
 val saved by vm.saved.collectAsStateWithLifecycle()
 val planSave by vm.planSave.collectAsStateWithLifecycle()
 val homeCases=remember(vm.content){val catalog=buildCatalog(vm.content.cases,vm.content.handbooks,vm.content.sources);listOf("01-routing","06-cleaning","11-documents").map{id->catalog.first{it.id==id}}}
 val nav=rememberNavController()
 val back by nav.currentBackStackEntryAsState()
 val rawRoute=back?.destination?.route?:"lab"
 val route=if(rawRoute in listOf("catalog","plan-builder","plan-result")) "lab" else rawRoute
 var showSettings by rememberSaveable { mutableStateOf(false) }
 var replayIntro by rememberSaveable { mutableStateOf(false) }
 fun openDestination(id: String) {
  nav.navigate(id) { popUpTo("lab"); launchSingleTop=true }
 }
 var activePlan by rememberSaveable { mutableStateOf(AutomationPlan().json()) }
 val en=settings.english
 SandboxTheme(settings.theme) {
  if(!settingsReady) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) }
  else if(!settings.onboardingComplete || replayIntro) {
   Onboarding(en,replayIntro,{vm.update(settings.copy(english=it))},{vm.update(settings.copy(onboardingComplete=true));replayIntro=false})
  } else BoxWithConstraints(Modifier.fillMaxSize()) {
   val wide=maxWidth>=840.dp
   Scaffold(containerColor=MaterialTheme.colorScheme.background,
    topBar={ Column(Modifier.statusBarsPadding()) {
     Header(en,{showSettings=true})
     HorizontalDivider(color=MaterialTheme.colorScheme.outline)
    } },
    bottomBar={if(!wide) NavigationBar(modifier=Modifier.heightIn(min=80.dp*LocalDensity.current.fontScale.coerceIn(1f,1.75f)),containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
     destinations(en).forEachIndexed { index,(id,label) -> NavigationBarItem(modifier=Modifier.padding(horizontal=3.dp,vertical=4.dp).background(if(route==id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(10.dp)).border(if(route==id) 2.dp else 1.dp,if(route==id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,RoundedCornerShape(10.dp)),selected=route==id,onClick={openDestination(id)},colors=NavigationBarItemDefaults.colors(indicatorColor=androidx.compose.ui.graphics.Color.Transparent,selectedIconColor=MaterialTheme.colorScheme.onPrimaryContainer,selectedTextColor=MaterialTheme.colorScheme.onPrimaryContainer),icon={Text("0${index+1}",fontFamily=FontFamily.Monospace,fontSize=responsiveSp(12))},label={Text(label,fontSize=responsiveSp(11))}) }
    }}
   ) { pad -> Row(Modifier.padding(pad).fillMaxSize()) {
    if(wide) NavigationRail(containerColor=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxHeight().width(160.dp)) {
     Spacer(Modifier.height(32.dp)); destinations(en).forEachIndexed { i,(id,label) -> NavigationRailItem(modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp).fillMaxWidth().background(if(route==id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(10.dp)).border(if(route==id) 2.dp else 1.dp,if(route==id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,RoundedCornerShape(10.dp)),selected=route==id,onClick={openDestination(id)},colors=NavigationRailItemDefaults.colors(indicatorColor=androidx.compose.ui.graphics.Color.Transparent,selectedIconColor=MaterialTheme.colorScheme.onPrimaryContainer,selectedTextColor=MaterialTheme.colorScheme.onPrimaryContainer),icon={Text("0${i+1}",fontFamily=FontFamily.Monospace)},label={Text(label)}) }
    }
    NavHost(navController=nav,startDestination="lab",modifier=Modifier.weight(1f)) {
     composable("lab") { PlannerHome(en,homeCases,saved,{activePlan=AutomationPlan().json();nav.navigate("plan-builder")},{vm.update(settings.copy(query="",category="all",level=0,approach="all"));nav.navigate("catalog")},{openDestination("saved")},{nav.navigate("case/$it")}) }
     composable("plan-builder") { PlanBuilder(com.google.gson.Gson().fromJson(activePlan,AutomationPlan::class.java),en,{nav.popBackStack()}){p->activePlan=p.json();nav.navigate("plan-result"){popUpTo("lab")}} }
     composable("plan-result") { AutomationPlan.decode(activePlan)?.let{p->PlanResult(p,vm.content.handbooks,en,saved.any{it.key=="plan:${p.id}" && it.input==p.json()},planSave,{vm.savePlan(p)},{nav.navigate("plan-builder")},{nav.navigate("guide/$it")},{openDestination("about")})} }
     composable("catalog") { Catalog(vm,settings,saved){id->nav.navigate(if(vm.content.cases.any{it.id==id})"case/$id" else "guide/$id")} }
     composable("toolkit") { Toolkit(vm.content.sources,vm.content.handbooks,en,onRead={nav.navigate("guide/$it")},onResources={nav.navigate("resources/$it")}) }
     composable("saved") { SavedPage(vm,saved,en,onPlan={p->activePlan=p.json();nav.navigate("plan-result")}) { id,input -> vm.open(vm.content.cases.first{it.id==id},input); nav.navigate("case/$id") } }
     composable("about") { AboutPage(en) }
     composable("resources/{kind}") {entry -> ResourceCatalog(vm.content.handbooks,entry.arguments?.getString("kind")?:"library",en,{nav.popBackStack()}){nav.navigate("guide/$it")} }
     composable("path/{id}") {entry->
      vm.content.handbooks.getValue("learning-paths").sections.firstOrNull{it.id==entry.arguments?.getString("id")}?.let{chapter->
       LearningPathPage(chapter,vm.content.handbooks,en,{nav.popBackStack()}){nav.navigate("guide/$it")}
      }
     }
     composable("guide/{id}") { entry ->
      vm.content.handbooks[entry.arguments?.getString("id")]?.let { book ->
       HandbookReader(book,en,settings.codeSize,onOpenPath={nav.navigate("path/$it")}){nav.popBackStack()}
      }
     }
     composable("case/{id}") { entry ->
      val c=vm.content.cases.firstOrNull {it.id==entry.arguments?.getString("id")}
      if(c!=null) { LaunchedEffect(c.id){ vm.open(c) }; CasePage(c,vm,settings,saved.any{it.key=="case:${c.id}"},onRead={nav.navigate("guide/${c.id}")},onAbout={nav.navigate("about"){launchSingleTop=true}}) { nav.popBackStack() } }
     }
    }
   } }
   if(showSettings) SettingsDialog(settings,{vm.update(it)}, {showSettings=false},vm::clear,{showSettings=false;replayIntro=true})
  }
 }
}
@Composable private fun Header(en: Boolean,onSettings: ()->Unit) {
 val compact=LocalUiSizing.current.compact
 val large=LocalDensity.current.fontScale>1.4f || (compact && LocalDensity.current.fontScale>1.15f)
 val brandSize=if(compact && large)18 else 20
 @Composable fun Brand() { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  BrandMark(36.dp,en)
  Column {
   Row {Text("LIK LAK",fontWeight=FontWeight.ExtraBold,fontFamily=FontFamily.Monospace,fontSize=responsiveSp(brandSize),lineHeight=responsiveSp(24));Text(" / AI",color=MaterialTheme.colorScheme.primary,fontFamily=FontFamily.Monospace,fontSize=responsiveSp(brandSize),lineHeight=responsiveSp(24))}
   Text("SANDBOX / v1.7",fontFamily=FontFamily.Monospace,fontSize=responsiveSp(10),color=MaterialTheme.colorScheme.onSurfaceVariant,letterSpacing=2.sp)
  }
 } }
 @Composable fun Controls() { Row {
  ActionButton(onSettings){Text(tr(en,"Настройки","Settings"),fontSize=responsiveSp(12))}
 } }
 if(large) Column(Modifier.fillMaxWidth().padding(horizontal=LocalUiSizing.current.pagePadding,vertical=8.dp)){Brand();Controls()}
 else Row(Modifier.fillMaxWidth().padding(horizontal=LocalUiSizing.current.pagePadding,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Brand()};Controls()}
}
private fun destinations(en: Boolean)=listOf("lab" to tr(en,"Лаборатория","Lab"),"toolkit" to tr(en,"Инструменти","Toolkit"),"saved" to tr(en,"Запазени","Saved"),"about" to tr(en,"За нас","About"))

@Composable private fun Catalog(vm: LabViewModel,s: Settings,saved: List<SavedEntry>,open: (String)->Unit) {
 val en=s.english
 val allCases=remember(vm.content){buildCatalog(vm.content.cases,vm.content.handbooks,vm.content.sources)}
 val filtered=allCases.filter { c ->
  (s.approach!="cloud" || c.cloudGuide) && (s.approach!="local" || c.interactive) &&
  (s.category=="all" || s.category==c.category) && (s.level==0 || s.level==c.level) &&
  c.matches(s.query,en,vm.content.handbooks)
 }.let {if(s.reverse) it.reversed() else it}
 BoxWithConstraints {
  val columns=if(maxWidth>=1000.dp) 3 else if(maxWidth>=620.dp) 2 else 1
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
   item { PageTitle(tr(en,"ЛОКАЛНО / AI SANDBOX","LOCAL FIRST / AI SANDBOX"),tr(en,"По-малко рутина.\nПовече яснота.","Less routine.\nMore clarity."),tr(en,"Разгледайте практични подходи за автоматизация. Променете входа, проследете резултата и вижте логиката.","Explore practical approaches to automation. Change an input, follow the result and inspect the logic.")) }
   item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
    Metric(allCases.size.toString(),tr(en,"практични кейса","practical cases"),Modifier.weight(1f));Metric(vm.content.cases.size.toString(),tr(en,"експеримента","experiments"),Modifier.weight(1f));Metric("BG / EN",tr(en,"два езика","two languages"),Modifier.weight(1f))
   } }
   item { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
    OutlinedTextField(value=s.query,onValueChange={vm.update(s.copy(query=it))},label={Text(tr(en,"Търсене на задача или инструмент","Search a task or tool"))},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp))
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
      ChoiceButton(tr(en,"Всички","All"),s.category=="all",{vm.update(s.copy(category="all"))})
      allCases.map{it.category}.distinct().forEach{cat->ChoiceButton(categoryLabel(cat,en),s.category==cat,{vm.update(s.copy(category=cat))})}
     }
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
      listOf(1 to tr(en,"Начално","Beginner"),2 to tr(en,"Средно","Intermediate"),3 to tr(en,"Напреднало","Advanced")).forEach{(level,label)->ChoiceButton(label,s.level==level,{vm.update(s.copy(level=if(s.level==level)0 else level))})}
      ChoiceButton(tr(en,"Всички подходи","All approaches"),s.approach=="all",{vm.update(s.copy(approach="all"))})
      ChoiceButton(tr(en,"Локално","Local"),s.approach=="local",{vm.update(s.copy(approach="local"))})
      ChoiceButton(tr(en,"API справочник","API guide"),s.approach=="cloud",{vm.update(s.copy(approach="cloud"))})
      ActionButton({vm.update(s.copy(reverse=!s.reverse))}){Text(if(s.reverse) "↓ ${allCases.size}–01" else "↑ 01–${allCases.size}")}
     }
    }
    Kicker("${filtered.size.toString().padStart(2,'0')} / "+tr(en,"КЕЙСОВЕ · РЕШЕБНИК И ЕКСПЕРИМЕНТИ","CASES · GUIDES AND EXPERIMENTS"))
   } }
   if(filtered.isEmpty()) item { Panel { Text(tr(en,"Няма намерени кейсове.","No cases found."));ActionButton({vm.update(s.copy(query="",category="all",level=0,approach="all"))}){Text(tr(en,"Изчистете филтрите","Clear filters"))} } }
   filtered.chunked(columns).forEach { row -> item { Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
    row.forEach { c -> CaseCard(c,en,saved.any{it.key=="case:${c.id}"},Modifier.weight(1f)){open(c.id)} }
    repeat(columns-row.size) {Spacer(Modifier.weight(1f))}
   } } }
   item { Body(tr(en,"Без акаунт. Без проследяване. Каталогът и експериментите работят офлайн.","No account. No tracking. The catalogue and experiments work offline.")) }
  }
 }
}
@Composable private fun Metric(value: String,label: String,modifier: Modifier=Modifier) { Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(value,fontFamily=FontFamily.Monospace,fontSize=responsiveSp(18),lineHeight=responsiveSp(22),fontWeight=FontWeight.Bold);Text(label,fontSize=responsiveSp(11),color=MaterialTheme.colorScheme.onSurfaceVariant)} }
@Composable private fun CasePage(c: LabCase,vm: LabViewModel,s: Settings,bookmarked: Boolean,onRead: ()->Unit,onAbout: ()->Unit,onBack: ()->Unit) {
 val en=s.english; val guide=c.guide(en)
 var tab by rememberSaveable(c.id){mutableIntStateOf(0)}
 var preview by rememberSaveable(c.id){mutableStateOf<String?>(null)}
 var brief by rememberSaveable(c.id){mutableStateOf(false)}
 var saveDialog by rememberSaveable(c.id){mutableStateOf(false)}
 var label by rememberSaveable(c.id){mutableStateOf("")}
 val x by vm.experiment.collectAsStateWithLifecycle()
 val context=LocalContext.current
 BackHandler(onBack=onBack)
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  item { ActionButton(onBack){Text("← "+tr(en,"Лаборатория","Lab"))} }
  item { PageTitle(tr(en,"КЕЙС","CASE")+" ${c.id.take(2)} / ${categoryLabel(c.category,en).uppercase()}",c.title(en),guide.problem) }
  item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   ActionButton({vm.bookmark(c)}){Text(if(bookmarked) tr(en,"✓ Запазен","✓ Saved") else tr(en,"+ Запазване","+ Save case"))}
   ActionButton({brief=true}){Text(tr(en,"Подгответе описание","Prepare a brief"))}
  } }
  item { val tabs=listOf(tr(en,"Обзор","Overview"),tr(en,"Експеримент","Experiment"),tr(en,"Код","Code"),tr(en,"Свързване","Setup"),tr(en,"Проверки","Checks"))
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { tabs.forEachIndexed {i,t->ChoiceButton("0${i+1} $t",tab==i,{tab=i})} }
  }
  when(tab) {
   0 -> {
    vm.content.handbooks[c.id]?.let { book -> item { HandbookEntry(book,en,onRead) } }
    item { Panel { Kicker(tr(en,"ВХОД → ПРОВЕРКА → РЕЗУЛТАТ","INPUT → VALIDATE → RESULT"));guide.steps.forEachIndexed{i,step-> Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){Text("${i+1}".padStart(2,'0'),fontFamily=FontFamily.Monospace,color=MaterialTheme.colorScheme.primary);Body(step)}} } }
    item { PrimaryActionButton({tab=1},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(tr(en,"Отворете експеримента →","Open experiment →"))} }
   }
   1 -> {
    item { Kicker(tr(en,"ЛОКАЛЕН АЛГОРИТЪМ / БЕЗ API ЗАЯВКИ","LOCAL ALGORITHM / NO API REQUESTS")) }
    item { val variants=Engine.variants(c)
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {variants.indices.forEach {i->ChoiceButton(if(i==0) tr(en,"Основен вариант","Default") else tr(en,"Вариант ${i+1}","Variant ${i+1}"),x.variant==i,{vm.select(c,i)})}}
    }
    item { Panel(Modifier.fillMaxWidth()) {Kicker(tr(en,"КАКВО ПРАВИ КОДЪТ","WHAT THE CODE DOES"));Body(variantNote(c.id,x.variant,en))} }
    item { BoxWithConstraints {
     val wide=maxWidth>680.dp
     if(wide) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) { InputPanel(c,vm,x,s,Modifier.weight(1f));OutputPanel(x,s,Modifier.weight(1f)){preview=c.title(en)+"\n\nINPUT / JSON\n"+x.input+"\n\nOUTPUT / JSON\n"+x.result} }
     else Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {InputPanel(c,vm,x,s,Modifier.fillMaxWidth());OutputPanel(x,s,Modifier.fillMaxWidth()){preview=c.title(en)+"\n\nINPUT / JSON\n"+x.input+"\n\nOUTPUT / JSON\n"+x.result}}
    } }
    item { ActionButton({saveDialog=true},modifier=Modifier.fillMaxWidth()){Text(tr(en,"Запазете конфигурацията","Save configuration"))} }
    if(c.id=="11-documents") item {Body(tr(en,"Входът е вече разпознат текст. Тази стъпка не разпознава изображение.","The input is already recognized text. This step does not perform image OCR."))}
   }
   2 -> {
    item { Panel { Kicker("PYTHON 3.10+ / "+tr(en,"СПРАВОЧЕН КОД","REFERENCE CODE"));Body(tr(en,"Самостоятелен пример за компютър. Приложението изпълнява еквивалентна Kotlin логика; този код не се изпълнява тук.","A standalone computer example. The app runs equivalent Kotlin logic; this code is not executed here."));CodeBlock(c.code,en,s.codeSize);ActionButton({vm.save(c,"code","")}){Text(tr(en,"Запазете кода","Save code"))} } }
    item { Panel { Kicker(tr(en,"PROMPT / МОДЕЛНО РАЗШИРЕНИЕ","PROMPT / MODEL EXTENSION"));Body(tr(en,"Предайте на отделно конфигуриран модел заедно с входа. Проверявайте схемата и резултата преди използване.","Pass to a separately configured model with the input. Validate its schema and result before use."));CodeBlock(if(en)c.prompt else c.prompt_bg,en,s.codeSize) } }
   }
   3 -> { c.refs.forEach { ref -> vm.content.sources[ref]?.let { source -> item {SourcePanel(source,en)} } }; item {Body(tr(en,"Фиксирайте версии и моделна ревизия след проверка на съвместимостта. Справочните интеграции не се стартират от приложението.","Pin versions and a model revision after checking compatibility. Reference integrations are not launched by this app."))} }
   4 -> {
    item { Panel { Kicker(tr(en,"КРИТЕРИИ ЗА ПРОВЕРКА","ACCEPTANCE CHECKS"));guide.acceptance.forEach{Body("• $it")} } }
    item { Panel { Kicker(tr(en,"ОГРАНИЧЕНИЯ","LIMITATIONS"));Body(guide.limitations) } }
   }
  }
  item { Panel { Kicker(tr(en,"С MAJESTICCORE","WITH MAJESTICCORE"));Text(tr(en,"Имате сходен процес?","Have a similar workflow?"),fontSize=responsiveSp(24),lineHeight=responsiveSp(29),fontWeight=FontWeight.SemiBold);Body(guide.help);PrimaryActionButton(onAbout){Text(tr(en,"Обсъдете този процес →","Discuss this workflow →"))} } }
 }
 if(brief) BriefDialog(c,en){brief=false}
 if(preview!=null) PreviewDialog(preview!!,en,{preview=null}) { body->copy(context,body,en);preview=null }
 if(saveDialog) AlertDialog(onDismissRequest={saveDialog=false},title={Text(tr(en,"Локална конфигурация","Local configuration"))},text={OutlinedTextField(label,{label=it},label={Text(tr(en,"Ваш етикет (по избор)","Your label (optional)"))})},confirmButton={ActionButton({vm.save(c,"config",label);saveDialog=false;label=""}){Text(tr(en,"Запазване","Save"))}},dismissButton={ActionButton({saveDialog=false}){Text(tr(en,"Отказ","Cancel"))}})
}

@Composable private fun InputPanel(c: LabCase,vm: LabViewModel,x: Experiment,s: Settings,modifier: Modifier) { Panel(modifier) {
 Kicker(tr(s.english,"ВХОД / JSON","INPUT / JSON"));CodeBlock(x.input,s.english,s.codeSize)
 Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  PrimaryActionButton({vm.run(c)},enabled=x.status!="running"){Text(tr(s.english,"Изпълни →","Run →"))}
  if(x.status=="running")ActionButton(vm::cancel){Text(tr(s.english,"Спиране","Cancel"))}
  ActionButton({vm.reset(c)}){Text(tr(s.english,"Нулиране","Reset"))}
 }
} }
@Composable private fun OutputPanel(x: Experiment,s: Settings,modifier: Modifier,onExport: ()->Unit) { Panel(modifier) {
 Kicker(tr(s.english,"РЕЗУЛТАТ / JSON","OUTPUT / JSON"))
 Text(when(x.status){"running"->tr(s.english,"Изпълнява се…","Running…");"success"->tr(s.english,"✓ Изчислено локално","✓ Computed locally");"error"->tr(s.english,"! Грешка във входа","! Input error");"cancelled"->tr(s.english,"Изпълнението е спряно","Run cancelled");else->tr(s.english,"Готово за изпълнение.","Ready to run.")},color=if(x.status=="error")MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
 if(x.status=="running")LinearProgressIndicator(Modifier.fillMaxWidth())
 if(x.result.isNotEmpty())CodeBlock(x.result,s.english,s.codeSize)
 if(x.status=="success")ActionButton(onExport){Text(tr(s.english,"Преглед и експорт","Preview and export"))}
} }
@Composable private fun SourcePanel(source: Source,en: Boolean) {
 val context=LocalContext.current
 Panel(Modifier.fillMaxWidth()) {Text(source.title,fontSize=responsiveSp(22),lineHeight=responsiveSp(27),fontWeight=FontWeight.Medium);Body(if(en)source.en else source.bg);ActionButton({copy(context,source.url,en)}){Text(tr(en,"Копирайте линка към документацията","Copy documentation link"))};if(source.pricing.isNotEmpty())ActionButton({copy(context,source.pricing,en)}){Text(tr(en,"Копирайте линка към цените","Copy pricing link"))}}
}
@Composable private fun Toolkit(sources: Map<String,Source>,books: Map<String,Handbook>,en: Boolean,onRead: (String)->Unit,onResources: (String)->Unit) {
 var legacy by rememberSaveable {mutableStateOf(false)}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  item {PageTitle(tr(en,"ИНСТРУМЕНТИ / РЕШЕБНИК","TOOLS / WORKBOOK"),tr(en,"От идея до решение.","From idea to solution."),tr(en,"Практически сценарии, проверени локални рецепти и шаблони за самостоятелна работа.","Practical scenarios, verified local recipes and templates for independent work."))}
  item {Panel(Modifier.fillMaxWidth()) {
   listOf("library","tool","recipe","prompt","supplement").forEach {kind->
    val count=resourceBooks(books,kind).size
    ActionButton({onResources(kind)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("$count · "+resourceTitle(kind,en))}
   }
  }}
  books["foundations"]?.let {book->item {HandbookEntry(book,en){onRead(book.id)}}}
  books["learning-paths"]?.let {book->item {HandbookEntry(book,en){onRead(book.id)}}}
  item {Panel {Kicker(tr(en,"КЪДЕ СЕ ИЗПЪЛНЯВА?","WHERE DOES IT RUN?"));Body(tr(en,"В приложението: 12 Kotlin експеримента. Останалите сценарии са ръководства. Python рецептите се изпълняват отделно на компютър. Prompts се копират; отварянето им не извиква модел.","In the app: 12 Kotlin experiments. Other scenarios are reading guides. Python recipes run separately on a computer. Prompts are copied; opening one does not call a model."))}}
  item {ActionButton({legacy=!legacy}){Text(tr(en,"Основни интеграционни инструкции","Core integration instructions"))}}
  if(legacy)sources.values.forEach {item {SourcePanel(it,en)}}
 }
}
@Composable private fun SavedPage(vm: LabViewModel,saved: List<SavedEntry>,en: Boolean,onPlan:(AutomationPlan)->Unit,open: (String,String?)->Unit) {
 val context=LocalContext.current
 if(saved.isEmpty()) {
  BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp)) {
   Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min=maxHeight),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp,Alignment.CenterVertically)) {
    Kicker(tr(en,"ВАШАТА БИБЛИОТЕКА","YOUR LIBRARY"))
    Text(tr(en,"Запазете идеята.","Keep the idea."),modifier=Modifier.fillMaxWidth(),fontSize=responsiveSp(32),lineHeight=responsiveSp(39),fontWeight=FontWeight.SemiBold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    Panel(Modifier.fillMaxWidth()){Text(tr(en,"Библиотеката е празна.","Your library is empty."),fontSize=responsiveSp(22),lineHeight=responsiveSp(27),modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center);Text(tr(en,"Създайте план от Лабораторията или отворете кейс и запазете кода или конфигурацията за по-късно.","Build a plan from Lab, or open a case to save its code or configuration."),modifier=Modifier.fillMaxWidth(),fontSize=responsiveSp(16),lineHeight=responsiveSp(25),color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
   }
  }
  return
 }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {PageTitle(tr(en,"ВАШАТА / БИБЛИОТЕКА","YOUR / LIBRARY"),tr(en,"Запазено за по-късно.","Keep it for later."),tr(en,"Планове, кейсове, код и конфигурации. Съхраняват се само на това устройство.","Plans, cases, code and configurations. Stored only on this device."))}
  saved.filter{it.kind=="plan"}.forEach{entry->item(key=entry.key){Panel(Modifier.fillMaxWidth()){Kicker(tr(en,"ПЛАН ЗА АВТОМАТИЗАЦИЯ","AUTOMATION PLAN"));Text(entry.label,fontSize=responsiveSp(22),lineHeight=responsiveSp(27));ActionButton({AutomationPlan.decode(entry.input)?.let(onPlan)}){Text(tr(en,"Отворете плана →","Open plan →"))};ActionButton({vm.delete(entry.key)}){Text(tr(en,"Премахване","Remove"))}}}}
  saved.forEach {entry->vm.content.cases.firstOrNull{it.id==entry.caseId}?.let {c-> item(key=entry.key) {Panel(Modifier.fillMaxWidth()) {
   Kicker(when(entry.kind){"config"->tr(en,"КОНФИГУРАЦИЯ","CONFIGURATION");"code"->tr(en,"КОД","CODE");else->tr(en,"КЕЙС","CASE")})
   Text(c.title(en),fontSize=responsiveSp(22),lineHeight=responsiveSp(27));if(entry.label.isNotBlank())Body(entry.label)
   Row(Modifier.horizontalScroll(rememberScrollState())) {
    ActionButton({open(c.id,if(entry.kind=="config")entry.input else null)}){Text(tr(en,"Отворете →","Open →"))}
    if(entry.kind=="code")ActionButton({copy(context,entry.input,en)}){Text(tr(en,"Копиране","Copy"))}
    ActionButton({vm.delete(entry.key)}){Text(tr(en,"Премахване","Remove"))}
   }
  } }}}
 }
}

@Composable private fun AboutPage(en: Boolean) {
 val context=LocalContext.current
 val services=if(en) listOf("Administrative & Office Support" to "Schedules, correspondence and document organization.","Customer Care & Helpdesk" to "Email, phone and chat communication; request organization.","HR & Payroll Assistance" to "Onboarding, employee documents, attendance and administrative coordination.","Data Entry & Information Management" to "Checks and updates of CRM, databases and digital records.","Billing & Financial Administration" to "Invoice preparation, payment tracking and record organization.") else listOf("Административна и офис подкрепа" to "Графици, кореспонденция и организация на документи.","Обслужване на клиенти" to "Комуникация по имейл, телефон и чат; организация на запитвания.","HR и административна помощ при възнаграждения" to "Въвеждане на служители, кадрови документи, присъствия и координация.","Въвеждане и управление на данни" to "Проверки и актуализиране на CRM, бази данни и цифрови записи.","Фактуриране и финансова администрация" to "Подготовка на фактури, проследяване на плащания и организация на записи.")
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  item {AboutPhoto("about-workspace.png",en)}
  item {PageTitle(tr(en,"ХОРА / ПРОЦЕСИ / ПОДКРЕПА","PEOPLE / PROCESSES / SUPPORT"),OrganizationDetails.studio,tr(en,"Подпомагаме бизнеса в организирането на ежедневните административни и оперативни дейности. Работим със стартиращи компании, малки и средни предприятия и утвърдени организации при процеси, които изискват точност, последователност и внимание към детайла. Допълваме работата на екипите, за да се фокусират върху клиентите и основните си приоритети.","We support businesses in organizing everyday administrative and operational work. We assist startups, small and medium-sized businesses, and established organizations with processes that require accuracy, consistency and attention to detail. Our support complements existing teams so they can focus on customers and core priorities."))}
  services.forEachIndexed{i,(title,body)->item{Panel(Modifier.fillMaxWidth()){Kicker("0${i+1} / MAJESTICCORE");Text(title,fontSize=responsiveSp(22),lineHeight=responsiveSp(27));Body(body)}};if(i==1)item{AboutPhoto("about-collaboration.png",en,true)}}
  item {Panel {Kicker(tr(en,"ЗА ЛАБОРАТОРИЯТА","ABOUT THE LAB"));Body(tr(en,"AI Sandbox е образователен каталог с подходи. Техническата реализация се обсъжда отделно и не представлява потвърдено портфолио от AI проекти на компанията.","AI Sandbox is an educational catalogue of approaches. Technical implementation is discussed separately and does not represent a confirmed company portfolio of AI projects."))}}
  item {Panel(Modifier.fillMaxWidth()) {
   Kicker(tr(en,"НЕКА ОБСЪДИМ","LET’S TALK"));Text(tr(en,"Обсъдете процеса си.","Discuss your workflow."),fontSize=responsiveSp(26),lineHeight=responsiveSp(32))
   Kicker(tr(en,"ДАННИ ЗА ОРГАНИЗАЦИЯТА","ORGANIZATION DETAILS"))
   Text(OrganizationDetails.organization,fontSize=responsiveSp(22),lineHeight=responsiveSp(27))
   Body(OrganizationDetails.address)
   Body(OrganizationDetails.email)
   ActionButton({copy(context,OrganizationDetails.email,en)}){Text(tr(en,"Копирайте имейла","Copy email address"))}
   ActionButton({copy(context,OrganizationDetails.address,en)}){Text(tr(en,"Копирайте адреса","Copy address"))}
   ActionButton({copy(context,OrganizationDetails.contacts,en)}){Text(tr(en,"Копирайте контактите","Copy contacts"))}
  }}
  item {Panel {Kicker("FAQ");Body(tr(en,"Нужна ли е регистрация? Не.\n\nРаботи ли офлайн? Да — каталогът, кодът, експериментите и библиотеката.\n\nИзпращат ли се примери? Не автоматично. Можете да копирате прегледаното описание.\n\nМоже ли да обсъдим реализация? Да. Обхват, цена и срок се уточняват отделно.\n\nВъншните линкове се копират; приложението не отваря сайтове.","Is registration required? No.\n\nDoes it work offline? Yes — the catalogue, code, experiments and library.\n\nAre examples sent anywhere? Not automatically. You can copy a reviewed brief.\n\nCan we discuss implementation? Yes. Scope, cost and timing are agreed separately.\n\nExternal links can be copied; the app does not open websites."))}}
 }
}
@Composable private fun BriefDialog(c: LabCase,en: Boolean,dismiss: ()->Unit) {
 val context=LocalContext.current
 val features=listOf(tr(en,"Организация на входа","Input organization"),tr(en,"Правила за проверка","Validation rules"),tr(en,"Човешко одобрение","Human approval"))
 var selected by rememberSaveable {mutableStateOf(listOf(0,2))}
 var body by rememberSaveable {mutableStateOf<String?>(null)}
 fun generate() = tr(en,"Здравейте, ","Hello ")+OrganizationDetails.studio+","+"\n\n"+tr(en,"Процес: ","Workflow: ")+c.title(en)+" (${c.id})\n"+tr(en,"Желан резултат: ","Desired outcome: ")+c.guide(en).problem+"\n\n"+tr(en,"Възможности: ","Features: ")+selected.joinToString(", "){features[it]}+"\n\n"+tr(en,"Моля, помогнете да уточним обхвата на подкрепата, проверките и необходимите стъпки.","Please help clarify the support scope, checks and required steps.")
 AlertDialog(onDismissRequest=dismiss,title={Text(tr(en,"Описание за MajesticCore","Brief for MajesticCore"))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  if(body==null) {
   Body(c.guide(en).help)
   features.forEachIndexed {i,f->Row(Modifier.fillMaxWidth().clickable{selected=if(i in selected)selected-i else selected+i},verticalAlignment=Alignment.CenterVertically){Checkbox(i in selected,{selected=if(it)selected+i else selected-i});Text(f)}}
  } else {
   Body(tr(en,"Редактирайте описанието и го копирайте.","Edit the brief and copy it."))
   OutlinedTextField(body!!,{body=it},modifier=Modifier.fillMaxWidth().heightIn(min=220.dp),label={Text(tr(en,"Текст на писмото","Email body"))})
  }
 }},confirmButton={ActionButton({if(body==null) body=generate() else {
  copy(context,body.orEmpty(),en)
 }}){Text(if(body==null)tr(en,"Преглед","Preview") else tr(en,"Копирайте описанието","Copy brief"))}},dismissButton={ActionButton(dismiss){Text(tr(en,"Отказ","Cancel"))}})
}
@Composable private fun PreviewDialog(text: String,en: Boolean,dismiss: ()->Unit,share: (String)->Unit) {
 AlertDialog(onDismissRequest=dismiss,title={Text(tr(en,"Преглед на експорта","Export preview"))},text={Column(Modifier.verticalScroll(rememberScrollState())){Body(tr(en,"Копирайте показаните вход и резултат.","Copy the displayed input and result."));CodeBlock(text,en)}},confirmButton={ActionButton({share(text)}){Text(tr(en,"Копиране","Copy"))}},dismissButton={ActionButton(dismiss){Text(tr(en,"Отказ","Cancel"))}})
}
