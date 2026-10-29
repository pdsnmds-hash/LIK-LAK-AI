package bg.liklak.sandbox

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable internal fun Kicker(text: String) { Text(text,color=MaterialTheme.colorScheme.primary,fontFamily=FontFamily.Monospace,fontSize=responsiveSp(11),letterSpacing=1.6.sp) }
@Composable internal fun Body(text: String) { Text(text,fontSize=responsiveSp(16),lineHeight=responsiveSp(25),color=MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable internal fun Panel(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) {
 Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) { Column(Modifier.padding(LocalUiSizing.current.panelPadding),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
}
@Composable internal fun PageTitle(kicker: String,title: String,description: String="") {
 Column(verticalArrangement=Arrangement.spacedBy(16.dp)) { Kicker(kicker);Text(title,fontSize=responsiveSp(36),lineHeight=responsiveSp(40),fontWeight=FontWeight.SemiBold,letterSpacing=(-1).sp);if(description.isNotEmpty())Body(description) }
}
@Composable internal fun CaseCard(c: CatalogEntry,en: Boolean,bookmarked: Boolean,modifier: Modifier,open: ()->Unit) {
 Panel(modifier.clickable(onClick=open)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Kicker(c.id.take(2)); Text(categoryLabel(c.category,en),fontSize=responsiveSp(11),color=MaterialTheme.colorScheme.primary) }
  Text(c.title(en),fontSize=responsiveSp(24),lineHeight=responsiveSp(28),fontWeight=FontWeight.SemiBold)
  Body(c.summary(en))
  HorizontalDivider(color=MaterialTheme.colorScheme.outline)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {ActionButton(open){Text((if(c.interactive)tr(en,"Explore case","Explore case") else tr(en,"Read the guide","Read the guide"))+" →")};if(bookmarked)Text("✓",color=MaterialTheme.colorScheme.primary)}
 }
}

@Composable internal fun CodeBlock(code: String,en: Boolean,size: Int=14) {
 val context=LocalContext.current
 val narrow=LocalUiSizing.current.compact
 var wrap by rememberSaveable{mutableStateOf(narrow)}
 var copied by remember(code) {mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.End) {
   ActionButton({wrap=!wrap}){Text(tr(en,if(wrap)"No wrap" else "Wrap",if(wrap)"No wrap" else "Wrap"),fontSize=responsiveSp(12))}
   ActionButton({copy(context,code,en);copied=true}){Text(if(copied) tr(en,"✓ Copied","✓ Copied") else tr(en,"Copy","Copy"),fontSize=responsiveSp(12))}
  }
  Surface(color=MaterialTheme.colorScheme.background,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth()) {
   SelectionContainer { Text(code,fontFamily=FontFamily.Monospace,fontSize=size.sp,lineHeight=(size+7).sp,color=MaterialTheme.colorScheme.secondary,modifier=Modifier.then(if(wrap)Modifier else Modifier.horizontalScroll(rememberScrollState())).padding(16.dp)) }
  }
 }
}
