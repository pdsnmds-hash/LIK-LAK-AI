package bg.liklak.sandbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class IntroPage(val titleBg:String,val titleEn:String,val bodyBg:String,val bodyEn:String)
internal val introPages=listOf(
 IntroPage("Start with a real task.","Start with a real task.","36 AI and programming cases: documents, data, customer care and development. Open explanations and code directly from the catalogue.","36 AI and programming cases: documents, data, customer care and development. Open explanations and code directly from the catalogue."),
 IntroPage("Try it. Understand why.","Try it. Understand why.","Change the input in 12 local experiments, run an example and compare the result. Read the limitations and detailed solution.","Change the input in 12 local experiments, run an example and compare the result. Read the limitations and detailed solution."),
 IntroPage("Turn a task into a plan.","Turn a task into a plan.","Describe the input, desired output and human review. Get steps linked to articles and save the plan on your device.","Describe the input, desired output and human review. Get steps linked to articles and save the plan on your device.")
)
@Composable fun Onboarding(en:Boolean,replay:Boolean,onFinish:()->Unit) {
 var page by rememberSaveable{mutableIntStateOf(0)}
 val scroll=rememberScrollState()
 LaunchedEffect(page){scroll.scrollTo(0)}
 BackHandler(enabled=page>0 || replay){if(page>0)page-- else onFinish()}
 Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),contentAlignment=Alignment.TopCenter) {
  Column(Modifier.widthIn(max=640.dp).fillMaxSize().padding(LocalUiSizing.current.pagePadding),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Box(Modifier.weight(1f)){Kicker("LIK LAK / AI")}
    ActionButton(onFinish){Text(tr(en,"Skip","Skip"))}
   }
   Column(Modifier.weight(1f).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    if(page==0) {
     Box(Modifier.fillMaxWidth().height(if(LocalDensity.current.fontScale>1.4f)280.dp else 210.dp).clip(RoundedCornerShape(20.dp)),contentAlignment=Alignment.Center){
      Image(painterResource(R.drawable.splash_art),contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.BottomCenter)
      Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
       BrandMark(104.dp,en)
       Text("LIK LAK / AI",color=Color(0xFFE8F0ED),fontFamily=FontFamily.Monospace,fontSize=responsiveSp(24),lineHeight=responsiveSp(29),fontWeight=FontWeight.Bold)
       Text("SANDBOX",color=Color(0xFF9CF5BC),fontFamily=FontFamily.Monospace,letterSpacing=4.sp,fontSize=responsiveSp(12))
      }
     }

    } else if(page==1) {
     Panel(Modifier.fillMaxWidth()){
      Kicker(tr(en,"INPUT → CHECK → RESULT","INPUT → CHECK → RESULT"))
      listOf(tr(en,"01  Choose an example","01  Choose an example"),tr(en,"02  Change the input","02  Change the input"),tr(en,"03  Compare the result","03  Compare the result")).forEach{Text(it,fontSize=responsiveSp(20),lineHeight=responsiveSp(24))}
     }
    } else {
     Panel(Modifier.fillMaxWidth()){
      Kicker(tr(en,"YOUR PRACTICAL ROUTE","YOUR PRACTICAL ROUTE"))
      PlannerProgress(2,en,1f)
      Body(tr(en,"Task → articles → saved plan","Task → articles → saved plan"))
     }
    }
    Kicker("${page+1} / ${introPages.size}")
    Text(if(en)introPages[page].titleEn else introPages[page].titleBg,color=MaterialTheme.colorScheme.onBackground,fontSize=responsiveSp(30),lineHeight=responsiveSp(35),fontWeight=FontWeight.SemiBold)
    Body(if(en)introPages[page].bodyEn else introPages[page].bodyBg)
    Panel(Modifier.fillMaxWidth()){
     Body(when(page){
      0->tr(en,"Works offline. No registration.","Works offline. No registration.")
      1->tr(en,"Examples run locally. No automatic cloud-model requests.","Examples run locally. No automatic cloud-model requests.")
      else->tr(en,"Save and export TXT. External links are copy-only.","Save and export TXT. External links are copy-only.")
     })
    }
   }
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
    if(page>0)ActionButton({page--}){Text(tr(en,"Back","Back"))}
    PrimaryActionButton({if(page<introPages.lastIndex)page++ else onFinish()},Modifier.weight(1f)){
     Text(if(page==introPages.lastIndex)tr(en,"Get started →","Get started →")else tr(en,"Continue →","Continue →"))
    }
   }
  }
 }
}
