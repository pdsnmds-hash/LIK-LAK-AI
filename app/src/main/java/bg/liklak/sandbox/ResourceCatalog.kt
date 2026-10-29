package bg.liklak.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun resourceBooks(books: Map<String,Handbook>,kind: String)=books.values.filter {
 if(kind=="library")it.type() !in setOf("tool","recipe","prompt") else it.type()==kind
}
fun resourceTitle(kind: String,en: Boolean)=when(kind) {
 "tool"->tr(en,"Инструменти","Tools")
 "recipe"->tr(en,"Python рецепти","Python recipes")
 "prompt"->tr(en,"Промптове","Prompts")
 "supplement"->tr(en,"Пътеки, практикум и речник","Paths, practice and glossary")
 else->tr(en,"Ръководства","Guides")
}
@Composable fun ResourceCatalog(books: Map<String,Handbook>,kind: String,en: Boolean,onBack: ()->Unit,open: (String)->Unit) {
 var query by rememberSaveable(kind){mutableStateOf("")}
 val filtered=remember(books,kind,query,en){resourceBooks(books,kind).filter{it.matches(query,en)}}
 BackHandler(onBack=onBack)
 Column(Modifier.fillMaxSize()) {
  Column(Modifier.padding(horizontal=20.dp)) {
   ActionButton(onBack){Text("← "+tr(en,"Назад","Back"))}
   Text(resourceTitle(kind,en),fontSize=responsiveSp(30),lineHeight=responsiveSp(36),fontWeight=FontWeight.SemiBold)
   OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),singleLine=true,label={Text(tr(en,"Търсене в материалите","Search these resources"))})
   Kicker("${filtered.size} / "+tr(en,"МАТЕРИАЛА","RESOURCES"))
  }
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   filtered.forEach {book->item(key=book.id){Panel(Modifier.fillMaxWidth().clickable{open(book.id)}) {
    Text(book.title(en),fontSize=responsiveSp(23),lineHeight=responsiveSp(29),fontWeight=FontWeight.SemiBold)
    Body(book.intro(en))
    ActionButton({open(book.id)}){Text(tr(en,"Отворете →","Open →"))}
   }}}
   if(filtered.isEmpty())item{Body(tr(en,"Няма съвпадения. Променете търсенето.","No matches. Try another search."))}
  }
 }
}
