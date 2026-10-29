package bg.liklak.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable fun HandbookReader(book: Handbook,english: Boolean,codeSize: Int,onOpenPath: (String)->Unit,onBack: ()->Unit) {
 var query by rememberSaveable(book.id) {mutableStateOf("")}
 var contents by rememberSaveable(book.id) {mutableStateOf(false)}
 var search by rememberSaveable(book.id) {mutableStateOf(false)}
 val chapters=remember(book,query,english) {book.sections.filter {it.matches(query,english)}}
 val listState=rememberLazyListState()
 val scope=rememberCoroutineScope()
 val context=LocalContext.current
 BackHandler {if(contents)contents=false else if(search){search=false;query=""} else onBack()}
 Column(Modifier.fillMaxSize()) {
  Surface(color=MaterialTheme.colorScheme.background) {
   Column(Modifier.fillMaxWidth().padding(horizontal=12.dp)) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
     ActionButton(onBack) {Text("← "+tr(english,"Назад","Back"))}
     ActionButton({contents=true}) {Text(tr(english,"Съдържание","Contents"))}
     ActionButton({search=!search;if(!search)query=""}) {Text(tr(english,"Търсене","Search"))}
    }
    if(search) OutlinedTextField(query,{query=it},label={Text(tr(english,"В това ръководство","In this guide"))},
     modifier=Modifier.fillMaxWidth().padding(bottom=12.dp),singleLine=true)
    if(query.isNotBlank()) Text(tr(english,"${chapters.size} от ${book.sections.size} раздела","${chapters.size} of ${book.sections.size} sections"),
     modifier=Modifier.padding(bottom=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=responsiveSp(12))
   }
  }
  HorizontalDivider(color=MaterialTheme.colorScheme.outline)
  LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=listState,contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
   item(key="intro") {
    PageTitle(tr(english,"ПРАКТИЧЕСКО РЪКОВОДСТВО","PRACTICAL HANDBOOK"),book.title(english),book.intro(english))
    Spacer(Modifier.height(16.dp))
    Kicker("${book.sections.size} "+tr(english,"РАЗДЕЛА / ОФЛАЙН","SECTIONS / OFFLINE"))
   }
   if(chapters.isEmpty()) item(key="empty") {Panel(Modifier.fillMaxWidth()) {
    Body(tr(english,"Няма съвпадения. Търсенето обхваща обясненията, кода и решенията.","No matches. Search covers explanations, code and solutions."))
    ActionButton({query=""}){Text(tr(english,"Изчистете търсенето","Clear search"))}
   }}
   chapters.forEach {chapter -> item(key=chapter.id) {
    var reveal by rememberSaveable(book.id,chapter.id,query) {mutableStateOf(query.isNotBlank() && chapter.solution(english).contains(query,true))}
    val index=book.sections.indexOf(chapter)+1
    Panel(Modifier.fillMaxWidth().then(if(book.id=="learning-paths")Modifier.clickable{onOpenPath(chapter.id)} else Modifier)) {
     Kicker(index.toString().padStart(2,'0')+" / "+tr(english,"РАЗДЕЛ","SECTION"))
     Text(chapter.title(english),fontSize=responsiveSp(25),lineHeight=responsiveSp(31),fontWeight=FontWeight.SemiBold)
     SelectionContainer {Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
      chapter.body(english).split("\n\n").forEach {Body(it)}
     }}
     if(book.id=="learning-paths") PrimaryActionButton({onOpenPath(chapter.id)},Modifier.fillMaxWidth()){Text(tr(english,"Отворете пътеката →","Open path →"))}
     if(chapter.code.isNotBlank()) {
      HorizontalDivider(color=MaterialTheme.colorScheme.outline)
      Kicker(if(chapter.language=="shell") tr(english,"ТЕРМИНАЛ / НА КОМПЮТЪР","TERMINAL / ON A COMPUTER")
       else if(chapter.language=="prompt") tr(english,"PROMPT / КОПИРАЙТЕ ШАБЛОНА","PROMPT / COPY TEMPLATE")
       else if(chapter.language=="json") tr(english,"JSON / ПРИМЕР","JSON / EXAMPLE")
       else if(chapter.runnable) tr(english,"PYTHON / ЛОКАЛНА РЕЦЕПТА","PYTHON / LOCAL RECIPE")
       else tr(english,"PYTHON / ОТДЕЛНА ИНТЕГРАЦИЯ","PYTHON / SEPARATE INTEGRATION"))
      CodeBlock(chapter.code(english),english,codeSize)
      if(chapter.expected.isNotBlank()) {
       Kicker(tr(english,"ИЗХОД ОТ ИЗПЪЛНЕНИЕТО","EXECUTED OUTPUT"))
       CodeBlock(chapter.expected,english,codeSize)
      }
     }
     if(chapter.solution(english).isNotBlank()) {
      val show=reveal
      ActionButton({reveal=!reveal},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
       Text(if(show)tr(english,"Скрийте решението","Hide solution") else tr(english,"Покажете решението","Reveal solution"))
      }
      if(show) {Kicker(tr(english,"РЕШЕНИЕ И ОБОСНОВКА","SOLUTION AND RATIONALE"));SelectionContainer{Body(chapter.solution(english))}}
     }
    }
   }}
   item(key="references") {Panel(Modifier.fillMaxWidth()) {
    Kicker(tr(english,"ОФИЦИАЛНИ ИЗТОЧНИЦИ","OFFICIAL REFERENCES"))
    Body(tr(english,"Линковете са само за копиране.","Reference links are available for copying only."))
    book.references.forEach{ref -> ActionButton({copy(context,ref.url,english)},modifier=Modifier.fillMaxWidth()){Text(ref.title+tr(english," · копирайте линка"," · copy link"))}}
   }}
  }
 }
 if(contents) AlertDialog(onDismissRequest={contents=false},title={Text(tr(english,"Съдържание","Contents"))},text={
  Column(Modifier.verticalScroll(rememberScrollState())) {
   chapters.forEachIndexed {i,c -> ActionButton({contents=false;scope.launch{listState.animateScrollToItem(i+1)}},modifier=Modifier.fillMaxWidth()) {
    Text((book.sections.indexOf(c)+1).toString().padStart(2,'0')+"  "+c.title(english),modifier=Modifier.fillMaxWidth())
   }}
  }
 },confirmButton={ActionButton({contents=false}){Text(tr(english,"Затваряне","Close"))}})
}

@Composable fun HandbookEntry(book: Handbook,en: Boolean,onRead: ()->Unit) {
 Panel(Modifier.fillMaxWidth()) {
  Kicker(tr(en,"ОТ ЗАДАЧА ДО РЕШЕНИЕ","FROM PROBLEM TO SOLUTION"))
  Text(tr(en,"Практическо ръководство","Practical handbook"),fontSize=responsiveSp(25),lineHeight=responsiveSp(31),fontWeight=FontWeight.SemiBold)
  Body(book.intro(en))
  Body(tr(en,"${book.sections.size} раздела с обяснения, примери и проверки.","${book.sections.size} sections of explanations, examples and checks."))
  PrimaryActionButton(onRead,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(tr(en,"Прочетете ръководството →","Read the guide →"))}
 }
}
