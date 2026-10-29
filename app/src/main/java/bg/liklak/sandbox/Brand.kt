package bg.liklak.sandbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

@Composable fun BrandMark(size:Dp,en:Boolean) {
 Image(painterResource(R.drawable.brand_mark),tr(en,"Лого на LIK LAK AI Sandbox","LIK LAK AI Sandbox logo"),modifier=Modifier.size(size).clip(RoundedCornerShape(size*.22f)))
}
