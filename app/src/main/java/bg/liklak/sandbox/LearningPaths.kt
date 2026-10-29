package bg.liklak.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun learningPathIds(chapter: HandbookChapter,books: Map<String,Handbook>): List<String> =
 Regex("\\b(?:\\d{2}-[a-z][a-z0-9-]*|recipe-[a-z][a-z0-9-]*)\\b")
  .findAll(chapter.body_en).map{it.value}.filter{it in books}.distinct().toList()

@Composable fun LearningPathPage(chapter: HandbookChapter,books: Map<String,Handbook>,en: Boolean,onBack: ()->Unit,onRead: (String)->Unit) {
 BackHandler(onBack=onBack)
 val steps=learningPathIds(chapter,books)
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  item {ActionButton(onBack){Text("← "+tr(en,"Назад","Back"))}}
  item {PageTitle(tr(en,"УЧЕБНА ПЪТЕКА","LEARNING PATH"),chapter.title(en),chapter.body(en))}
  steps.forEachIndexed{i,id->val book=books.getValue(id);item(key=id){Panel(Modifier.fillMaxWidth()) {
   Kicker(tr(en,"СТЪПКА","STEP")+" "+(i+1).toString().padStart(2,'0')+" / ${steps.size}")
   Text(book.title(en),fontSize=responsiveSp(24),lineHeight=responsiveSp(30),fontWeight=FontWeight.SemiBold)
   Body(book.intro(en))
   Body(tr(en,"${book.sections.size} раздела с материали и примери.","${book.sections.size} sections of material and examples."))
   PrimaryActionButton({onRead(id)},Modifier.fillMaxWidth()){Text(tr(en,"Отворете урока →","Open lesson →"))}
  }}}
 }
}
