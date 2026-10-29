package bg.liklak.sandbox

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Width scaling is separate from Android's user font scale, which remains intact. */
internal data class UiSizing(val widthDp:Int) {
 val factor=(widthDp/411f).coerceIn(.875f,1f)
 val pagePadding=when{widthDp<360->14.dp;widthDp<400->16.dp;else->20.dp}
 val panelPadding=when{widthDp<360->14.dp;widthDp<400->16.dp;else->20.dp}
 val compact get()=widthDp<360
}
internal val LocalUiSizing=staticCompositionLocalOf{UiSizing(411)}
@Composable internal fun responsiveSp(size:Int)=(size*LocalUiSizing.current.factor).coerceAtLeast(10.5f).sp
internal fun Typography.forWidth(factor:Float):Typography {
 fun TextStyle.scaled()=copy(fontSize=fontSize*factor,lineHeight=lineHeight*factor,letterSpacing=letterSpacing*factor)
 return copy(displayLarge=displayLarge.scaled(),displayMedium=displayMedium.scaled(),displaySmall=displaySmall.scaled(),headlineLarge=headlineLarge.scaled(),headlineMedium=headlineMedium.scaled(),headlineSmall=headlineSmall.scaled(),titleLarge=titleLarge.scaled(),titleMedium=titleMedium.scaled(),titleSmall=titleSmall.scaled(),bodyLarge=bodyLarge.scaled(),bodyMedium=bodyMedium.scaled(),bodySmall=bodySmall.scaled(),labelLarge=labelLarge.scaled(),labelMedium=labelMedium.scaled(),labelSmall=labelSmall.scaled())
}
