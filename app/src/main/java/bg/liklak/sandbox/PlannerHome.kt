package bg.liklak.sandbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PlannerHome(en:Boolean,cases:List<CatalogEntry>,saved:List<SavedEntry>,onCreate:()->Unit,onCatalog:()->Unit,onSaved:()->Unit,onCase:(String)->Unit) {
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {PageTitle(tr(en,"КЕЙСОВЕ / ПРАКТИКА / ПЛАНОВЕ","CASES / PRACTICE / PLANS"),tr(en,"Намерете решение.\nОпитайте го.","Find a solution.\nTry it out."),tr(en,"36 кейса с обяснения и код. Разгледайте ги свободно или създайте собствен план.","36 cases with explanations and code. Browse freely or build your own plan."))}
  item {PrimaryActionButton(onCatalog,Modifier.fillMaxWidth()){Text(tr(en,"Всички 36 кейса →","Browse all 36 cases →"))}}
  item {ActionButton(onCreate,Modifier.fillMaxWidth()){Text(tr(en,"Създайте свой план →","Build your plan →"))}}
  item {Kicker(tr(en,"ЗАПОЧНЕТЕ С КЕЙС","START WITH A CASE"))}
  cases.forEach{c->item(key=c.id){CaseCard(c,en,saved.any{it.key=="case:${c.id}"},Modifier.fillMaxWidth()){onCase(c.id)}}}
  item {ActionButton(onCatalog,Modifier.fillMaxWidth()){Text(tr(en,"Разгледайте целия каталог →","Explore the full catalogue →"))}}
  item {ActionButton(onSaved,Modifier.fillMaxWidth()){Text(tr(en,"Моите запазени материали →","My saved materials →"))}}
 }
}
