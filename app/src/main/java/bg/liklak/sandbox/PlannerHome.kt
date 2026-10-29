package bg.liklak.sandbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A compact entry to existing catalogue filters, cases, plans and saved materials. */
@Composable fun PlannerHome(en:Boolean,cases:List<CatalogEntry>,saved:List<SavedEntry>,onCreate:()->Unit,onCatalog:()->Unit,onSaved:()->Unit,onCategory:(String)->Unit,onCase:(String)->Unit) {
 val tasks=listOf(
  Triple("Documents","Extract & check","documents"),
  Triple("Requests","Route & reply","customer care"),
  Triple("Data & tables","Clean & validate","data"),
  Triple("Code & tests","Review & test","development")
 )
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  item {
   Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text("What would you\nlike to solve?",fontSize=responsiveSp(28),lineHeight=responsiveSp(32),fontWeight=FontWeight.SemiBold)
    Text("Choose a task. Explore a worked example.",fontSize=responsiveSp(14),lineHeight=responsiveSp(20),color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  item {
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    tasks.chunked(2).forEach { row ->
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      row.forEach { (title,description,category) ->
       ActionButton({onCategory(category)},Modifier.weight(1f).heightIn(min=88.dp)) {
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)) {
         Text(title,fontSize=responsiveSp(16),lineHeight=responsiveSp(21),fontWeight=FontWeight.SemiBold)
         Text(description+" →",fontSize=responsiveSp(12),lineHeight=responsiveSp(17),color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
       }
      }
     }
    }
    PrimaryActionButton(onCatalog,Modifier.fillMaxWidth()){Text("All 36 cases →")}
   }
  }
  cases.firstOrNull()?.let { example ->
   item {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text("Start with an example",fontSize=responsiveSp(18),lineHeight=responsiveSp(23),fontWeight=FontWeight.SemiBold)
     Panel(Modifier.fillMaxWidth()) {
      Text(example.title(en),fontSize=responsiveSp(21),lineHeight=responsiveSp(27),fontWeight=FontWeight.SemiBold)
      Text("Route a request using explicit rules, then inspect the result.",fontSize=responsiveSp(14),lineHeight=responsiveSp(21),color=MaterialTheme.colorScheme.onSurfaceVariant)
      ActionButton({onCase(example.id)},Modifier.fillMaxWidth()){Text("Explore case →")}
     }
    }
   }
  }
  item {
   Panel(Modifier.fillMaxWidth()) {
    Text("Working on your own task?",fontSize=responsiveSp(20),lineHeight=responsiveSp(26),fontWeight=FontWeight.SemiBold)
    Text("Build a plan with steps linked to the casebook.",fontSize=responsiveSp(14),lineHeight=responsiveSp(21),color=MaterialTheme.colorScheme.onSurfaceVariant)
    ActionButton(onCreate,Modifier.fillMaxWidth()){Text("Build your plan →")}
   }
  }
  item {ActionButton(onSaved,Modifier.fillMaxWidth()){Text(if(saved.isEmpty())"My saved materials →" else "Saved materials (${saved.size}) →")}}
 }
}
