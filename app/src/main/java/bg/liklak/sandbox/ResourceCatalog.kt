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
 "tool"->tr(en,"Tools","Tools")
 "recipe"->tr(en,"Python recipes","Python recipes")
 "prompt"->tr(en,"Prompts","Prompts")
 "supplement"->tr(en,"Paths, practice and glossary","Paths, practice and glossary")
 else->tr(en,"Guides","Guides")
}
@Composable fun ResourceCatalog(books: Map<String,Handbook>,kind: String,en: Boolean,onBack: ()->Unit,open: (String)->Unit) {
 var query by rememberSaveable(kind){mutableStateOf("")}
 val filtered=remember(books,kind,query,en){resourceBooks(books,kind).filter{it.matches(query,en)}}
 BackHandler(onBack=onBack)
 Column(Modifier.fillMaxSize()) {
  Column(Modifier.padding(horizontal=20.dp)) {
   ActionButton(onBack){Text("← "+tr(en,"Back","Back"))}
   Text(resourceTitle(kind,en),fontSize=responsiveSp(30),lineHeight=responsiveSp(36),fontWeight=FontWeight.SemiBold)
   OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),singleLine=true,label={Text(tr(en,"Search these resources","Search these resources"))})
   Kicker("${filtered.size} / "+tr(en,"RESOURCES","RESOURCES"))
  }
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   filtered.forEach {book->item(key=book.id){Panel(Modifier.fillMaxWidth().clickable{open(book.id)}) {
    Text(book.title(en),fontSize=responsiveSp(23),lineHeight=responsiveSp(29),fontWeight=FontWeight.SemiBold)
    Body(book.intro(en))
    ActionButton({open(book.id)}){Text(tr(en,"Open →","Open →"))}
   }}}
   if(filtered.isEmpty())item{Body(tr(en,"No matches. Try another search.","No matches. Try another search."))}
  }
 }
}
