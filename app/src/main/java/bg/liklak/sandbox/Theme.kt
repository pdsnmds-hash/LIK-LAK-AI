package bg.liklak.sandbox

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val dark=darkColorScheme(primary=Color(0xFF9CF5BC),onPrimary=Color(0xFF0B1012),background=Color(0xFF0B1012),onBackground=Color(0xFFE8F0ED),surface=Color(0xFF111A1E),onSurface=Color(0xFFE8F0ED),surfaceVariant=Color(0xFF172329),onSurfaceVariant=Color(0xFFA2B3AD),outline=Color(0xFF30423C),secondary=Color(0xFF83DDE8),primaryContainer=Color(0xFF234135),onPrimaryContainer=Color(0xFFB7FFD1),secondaryContainer=Color(0xFF234135),onSecondaryContainer=Color(0xFFB7FFD1),tertiary=Color(0xFF83DDE8),tertiaryContainer=Color(0xFF193B41),onTertiaryContainer=Color(0xFFB7F4FC),surfaceTint=Color(0xFF9CF5BC),surfaceDim=Color(0xFF0B1012),surfaceBright=Color(0xFF233137),surfaceContainerLowest=Color(0xFF0B1012),surfaceContainerLow=Color(0xFF111A1E),surfaceContainer=Color(0xFF152126),surfaceContainerHigh=Color(0xFF172329),surfaceContainerHighest=Color(0xFF203037),outlineVariant=Color(0xFF30423C))
private val light=lightColorScheme(primary=Color(0xFF176B3B),onPrimary=Color.White,background=Color(0xFFF3F5F0),onBackground=Color(0xFF17251D),surface=Color.White,onSurface=Color(0xFF17251D),surfaceVariant=Color(0xFFE8EEE7),onSurfaceVariant=Color(0xFF52665A),outline=Color(0xFFCCD8CF),secondary=Color(0xFF126579),primaryContainer=Color(0xFFD2ECD9),onPrimaryContainer=Color(0xFF15472B),secondaryContainer=Color(0xFFD2ECD9),onSecondaryContainer=Color(0xFF15472B),tertiary=Color(0xFF126579),tertiaryContainer=Color(0xFFD7EDF0),onTertiaryContainer=Color(0xFF124A56),surfaceTint=Color(0xFF176B3B),surfaceDim=Color(0xFFE3E9E1),surfaceBright=Color.White,surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF3F5F0),surfaceContainer=Color(0xFFEDF1EA),surfaceContainerHigh=Color(0xFFE8EEE7),surfaceContainerHighest=Color(0xFFE0E8DE),outlineVariant=Color(0xFFCCD8CF))
@Composable fun SandboxTheme(theme:String,content:@Composable ()->Unit) {
 val width=LocalConfiguration.current.screenWidthDp
 val sizing=remember(width){UiSizing(width)}
 val typography=remember(sizing.factor){Typography().forWidth(sizing.factor)}
 CompositionLocalProvider(LocalUiSizing provides sizing) {
  MaterialTheme(colorScheme=if(theme=="dark" || (theme=="system" && isSystemInDarkTheme())) dark else light,typography=typography,shapes=Shapes(small=RoundedCornerShape(8.dp),medium=RoundedCornerShape(12.dp),large=RoundedCornerShape(16.dp),extraLarge=RoundedCornerShape(20.dp)),content=content)
 }
}
