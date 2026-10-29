package bg.liklak.sandbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable fun SettingsDialog(s: Settings,update: (Settings)->Unit,dismiss: ()->Unit,clear: ()->Unit,replayIntro:()->Unit) {
 val en=s.english
 var confirmClear by rememberSaveable {mutableStateOf(false)}
 Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxWidth().padding(LocalUiSizing.current.pagePadding).heightIn(max=LocalConfiguration.current.screenHeightDp.dp*0.88f),
   shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
   Column(Modifier.padding(LocalUiSizing.current.panelPadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {Kicker("LIK LAK / "+tr(en,"PREFERENCES","PREFERENCES"));Text(tr(en,"Your workspace.","Your workspace."),fontSize=responsiveSp(28),lineHeight=responsiveSp(34),fontWeight=FontWeight.SemiBold)}
    HorizontalDivider(color=MaterialTheme.colorScheme.outline)
    Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
     Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Kicker("01 / "+tr(en,"APPEARANCE","APPEARANCE"))
      listOf("system" to tr(en,"System · match device","System · match device"),"dark" to tr(en,"Dark · graphite & mint","Dark · graphite & mint"),"light" to tr(en,"Light · paper console","Light · paper console")).forEach{(id,label)->
       ChoiceButton(label,s.theme==id,{update(s.copy(theme=id))},Modifier.fillMaxWidth())
      }
     }
     Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Kicker("02 / "+tr(en,"CODE SIZE","CODE SIZE"))
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(14,18,22).forEach{size->ChoiceButton("$size sp",s.codeSize==size,{update(s.copy(codeSize=size))},Modifier.weight(1f))}}
     }
     Panel(Modifier.fillMaxWidth()) {
      Kicker("03 / "+tr(en,"LOCAL LIBRARY","LOCAL LIBRARY"))
      Body(tr(en,"Cases, code and configurations stay on this device.","Cases, code and configurations stay on this device."))
      ActionButton({confirmClear=true},Modifier.fillMaxWidth()){Text(tr(en,"Clear library","Clear library"))}
     }
     ActionButton(replayIntro,Modifier.fillMaxWidth()){Text(tr(en,"Replay introduction","Replay introduction"))}
    }
    PrimaryActionButton(dismiss,Modifier.fillMaxWidth()){Text(tr(en,"Done","Done"))}
   }
  }
 }
 if(confirmClear) AlertDialog(onDismissRequest={confirmClear=false},containerColor=MaterialTheme.colorScheme.surface,
  title={Text(tr(en,"Clear saved items?","Clear saved items?"))},
  text={Body(tr(en,"This removes saved cases, code and configurations.","This removes saved cases, code and configurations."))},
  confirmButton={PrimaryActionButton({clear();confirmClear=false}){Text(tr(en,"Clear","Clear"))}},
  dismissButton={ActionButton({confirmClear=false}){Text(tr(en,"Cancel","Cancel"))}})
}
