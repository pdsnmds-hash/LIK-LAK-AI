package bg.liklak.sandbox

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable fun AboutPhoto(asset: String,en: Boolean,collaboration: Boolean=false) {
 val context=LocalContext.current
 val photo=remember(asset){context.assets.open("images/$asset").use{BitmapFactory.decodeStream(it)}.asImageBitmap()}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Image(photo,contentDescription=if(collaboration)tr(en,"Работа по документи в екип","People working together on documents") else tr(en,"Подредено работно пространство с лаптоп и документи","An organized workspace with a laptop and documents"),
   contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().aspectRatio(if(collaboration)1.8f else 1.6f).clip(RoundedCornerShape(16.dp)))
  Kicker(if(collaboration)tr(en,"ХОРА И ПОСЛЕДОВАТЕЛНИ ПРОЦЕСИ","PEOPLE AND CONSISTENT PROCESSES") else tr(en,"РАБОТА, ПОДРЕДЕНА В ДЕТАЙЛИ","WORK, ORGANIZED IN DETAIL"))
 }
}
