package bg.liklak.sandbox

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*

@Composable private fun motionEnabled():Boolean {
 val resolver=LocalContext.current.contentResolver
 fun readEnabled()=Settings.Global.getFloat(resolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f
 var enabled by remember{mutableStateOf(readEnabled())}
 DisposableEffect(resolver){
  val observer=object:ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){enabled=readEnabled()}}
  resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
  onDispose{resolver.unregisterContentObserver(observer)}
 }
 return enabled
}
@Composable private fun accentProperties():LottieDynamicProperties {
 val color=MaterialTheme.colorScheme.primary.toArgb()
 return rememberLottieDynamicProperties(
  rememberLottieDynamicProperty(LottieProperty.COLOR,color,keyPath=arrayOf("**","Accent")),
  rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR,color,keyPath=arrayOf("**","Accent"))
 )
}
@Composable fun PlannerProgress(step:Int,en:Boolean,progress:Float) {
 val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.planner_progress))
 val enabled=motionEnabled()
 val labels=if(en)listOf("Choose","Describe","Check")else listOf("Избор","Описание","Проверка")
 Column(Modifier.fillMaxWidth().semantics(mergeDescendants=true){contentDescription=tr(en,"Стъпка ${step+1} от 3: ","Step ${step+1} of 3: ")+labels[step];progressBarRangeInfo=ProgressBarRangeInfo((step+1).toFloat(),1f..3f,1)},verticalArrangement=Arrangement.spacedBy(4.dp)) {
  LottieAnimation(composition,progress={if(enabled)progress else step.coerceIn(0,2)/2f},dynamicProperties=accentProperties(),modifier=Modifier.fillMaxWidth().height(64.dp).clearAndSetSemantics{})
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){labels.forEachIndexed{i,label->Text((if(i<step)"✓ " else "")+label,modifier=Modifier.weight(1f),textAlign=TextAlign.Center,color=if(i==step)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}
 }
}
@Composable fun SaveConfirmation(event:Long,play:Boolean,en:Boolean,onFinished:()->Unit) {
 val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.saved_confirmation))
 val animation=rememberLottieAnimatable()
 val enabled=motionEnabled()
 val finished by rememberUpdatedState(onFinished)
 LaunchedEffect(composition,event,play,enabled){
  val c=composition?:return@LaunchedEffect
  if(play && enabled){animation.snapTo(c,progress=0f);animation.animate(c,iterations=1);finished()}
  else {animation.snapTo(c,progress=1f);if(play)finished()}
 }
 Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){liveRegion=LiveRegionMode.Polite},verticalAlignment=Alignment.CenterVertically) {
  LottieAnimation(composition,progress={if(play && enabled)animation.progress else 1f},dynamicProperties=accentProperties(),modifier=Modifier.size(64.dp).clearAndSetSemantics{})
  Text(tr(en,"Планът е запазен на устройството.","Plan saved on this device."),color=MaterialTheme.colorScheme.primary)
 }
}
