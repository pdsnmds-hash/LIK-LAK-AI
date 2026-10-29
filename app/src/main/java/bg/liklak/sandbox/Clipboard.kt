package bg.liklak.sandbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

internal fun copy(context: Context,text: String,en: Boolean) {
 (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("LIK LAK",text))
 Toast.makeText(context,tr(en,"Copied","Copied"),Toast.LENGTH_SHORT).show()
}
