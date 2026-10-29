package bg.liklak.sandbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** All actions have a visible boundary and fill, including small toolbar actions. */
@Composable fun ActionButton(onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true,content: @Composable RowScope.()->Unit) {
 OutlinedButton(onClick,modifier.heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(10.dp),
  border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),
  colors=ButtonDefaults.outlinedButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant,contentColor=MaterialTheme.colorScheme.primary),
  contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp),content=content)
}
@Composable fun PrimaryActionButton(onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true,content: @Composable RowScope.()->Unit) {
 Button(onClick,modifier.heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(10.dp),
  border=BorderStroke(1.dp,MaterialTheme.colorScheme.primary),content=content)
}
@Composable fun ChoiceButton(label: String,chosen: Boolean,onClick: ()->Unit,modifier: Modifier=Modifier) {
 OutlinedButton(onClick,modifier.heightIn(min=48.dp).semantics{selected=chosen},shape=RoundedCornerShape(10.dp),
  border=BorderStroke(if(chosen)2.dp else 1.dp,if(chosen)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
  colors=ButtonDefaults.outlinedButtonColors(containerColor=if(chosen)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
   contentColor=if(chosen)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant),
  contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)) {Text((if(chosen)"✓ " else "")+label)}
}
