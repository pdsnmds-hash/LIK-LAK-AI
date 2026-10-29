package bg.liklak.sandbox

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.runner.RunWith

/** Runs against the real window width; the host runner selects 411/380/320 dp. */
@RunWith(AndroidJUnit4::class)
class ResponsiveLayoutTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private lateinit var original:Settings
 private lateinit var vm:LabViewModel
 private lateinit var output:File
 private var english=true
 private val width get()=ui.activity.resources.configuration.screenWidthDp
 private fun exists(text:String)=ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
 private fun tap(text:String){ui.onNodeWithText(text).performClick();ui.waitForIdle()}
 private fun scrollTap(text:String,substring:Boolean=false) {
  val matcher=hasText(text,substring=substring)
  if(ui.onAllNodes(matcher).fetchSemanticsNodes().isEmpty())ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(matcher)
  ui.onNode(matcher).performScrollTo().performClick();ui.waitForIdle()
 }
 private fun firstCase(){
  val matcher=hasText(local("Отворете кейса →","Explore case →"))
  ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(matcher)
  ui.onAllNodes(matcher).onFirst().performScrollTo().performClick()
 }
 private fun local(bg:String,en:String)=if(english)en else bg
 private fun tab(bg:String,en:String)=tap(local(bg,en))
 private fun back(){ui.runOnUiThread{ui.activity.onBackPressedDispatcher.onBackPressed()};ui.waitForIdle()}
 private fun visible(name:String,screenshot:Boolean=true) {
  ui.waitForIdle()
  val nodes=ui.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes()
  val errors=mutableListOf<String>();var checked=0
  for(node in nodes) {
   if(node.boundsInRoot.width<1 || node.boundsInRoot.height<1 || node.config.contains(SemanticsActions.SetText))continue
   val results=mutableListOf<TextLayoutResult>()
   ui.runOnIdle{node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)}
   for(result in results){
    if(result.layoutInput.text.isEmpty())continue
    checked++
    // Paragraph width includes unused constraints. Check the actual rendered lines.
    val right=(0 until result.lineCount).maxOfOrNull{result.getLineRight(it)}?:0f
    val left=(0 until result.lineCount).minOfOrNull{result.getLineLeft(it)}?:0f
    val bottom=if(result.lineCount>0)result.getLineBottom(result.lineCount-1) else 0f
    if(right>result.size.width+1f || left< -1f || bottom>result.size.height+1f || result.multiParagraph.didExceedMaxLines)
     errors+="${result.layoutInput.text.text.take(100)} [${result.size}, right=$right left=$left bottom=$bottom]"
   }
  }
  if(screenshot) {
   val bitmap=ui.onAllNodes(isRoot(),useUnmergedTree=true).onLast().captureToImage().asAndroidBitmap()
   File(output,"${if(english)"EN" else "BG"}-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
  }
  File(output,"checks.txt").appendText("$width ${if(english)"EN" else "BG"} $name: $checked texts; ${errors.size} overflow\n"+errors.joinToString("\n")+"\n")
  Assert.assertTrue("$width $name text overflow:\n"+errors.joinToString("\n"),errors.isEmpty())
 }
 private fun scan(name:String,dialog:Boolean=false,swipes:Int=3) {
  visible(name)
  val matcher=SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
  repeat(swipes){index->
   if(ui.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()){
    val scroll=if(dialog)ui.onAllNodes(matcher).onLast() else ui.onAllNodes(matcher).onFirst()
    scroll.performTouchInput{swipeUp()};ui.waitForIdle();visible("$name-scroll-${index+1}",false)
   }
  }
 }
 private fun home(){
  tap(if(exists("Lab"))"Lab" else "Лаборатория")
  if(ui.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty())ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
  ui.waitForIdle()
 }
 private fun language(en:Boolean) {
  tap(if(exists("Settings"))"Settings" else "Настройки")
  ui.onNode(hasText(if(en)"English" else "Български",substring=true)).performClick();english=en
  tap(local("Готово","Done"))
 }
 private fun fillFirst(value:String) {
  ui.onAllNodes(hasSetTextAction()).onFirst().performTextInput(value)
  ui.runOnUiThread{WindowCompat.getInsetsController(ui.activity.window,ui.activity.window.decorView).hide(WindowInsetsCompat.Type.ime())}
  ui.waitForIdle()
 }
 @Before fun ready()=runBlocking {
  original=SettingsRepository(ui.activity.application).settings.first();english=original.english
  ui.runOnUiThread{vm=ViewModelProvider(ui.activity)[LabViewModel::class.java]}
  ui.waitUntil(10_000){exists("Settings") || exists("Настройки") || exists("Skip") || exists("Пропуснете")}
  if(exists("Skip"))tap("Skip") else if(exists("Пропуснете"))tap("Пропуснете")
  output=File(ui.activity.cacheDir,"responsive-$width"+if(ui.activity.resources.configuration.fontScale>1.1f)"-large" else "").apply{mkdirs()}
  File(output,"checks.txt").writeText("Actual width=$width dp\n")
 }
 @After fun restore()=runBlocking {
  if(::vm.isInitialized && ::original.isInitialized){
   ui.runOnUiThread{vm.update(original)}
   withTimeout(5000){SettingsRepository(ui.activity.application).settings.first{it==original}}
  }
 }
 @Test fun sectionsFitInBothLanguages() {
  for(en in listOf(true,false)) {
   home();language(en)
   scan("home")
   home();tap(local("Всички 36 кейса →","Browse all 36 cases →"));scan("catalog")
   home();firstCase()
   visible("case-overview")
   scrollTap(local("02 Експеримент","02 Experiment"),true)
   scan("experiment")
   home();firstCase()
   scrollTap(local("Прочетете ръководството →","Read the guide →"));scan("handbook",swipes=5)
   home();tab("Инструменти","Toolkit");scan("toolkit")
   home();tab("Запазени","Saved");visible("saved")
   home();tab("За нас","About");scan("about",swipes=7)
   home();tap(local("Създайте свой план →","Build your plan →"));visible("builder-1")
   scrollTap(local("Продължете →","Continue →"));scan("builder-2")
   scrollTap(local("Попълнете пример","Fill an example"))
   scrollTap(local("Продължете →","Continue →"));scan("builder-3")
   ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
   ui.onAllNodes(hasSetTextAction()).onFirst().performScrollTo().performTextInput("QA reviewer")
   ui.runOnUiThread{WindowCompat.getInsetsController(ui.activity.window,ui.activity.window.decorView).hide(WindowInsetsCompat.Type.ime())};ui.waitForIdle()
   ui.onAllNodes(hasSetTextAction() and !hasText("QA reviewer")).onFirst().performScrollTo().performTextInput("Matches source")
   ui.runOnUiThread{WindowCompat.getInsetsController(ui.activity.window,ui.activity.window.decorView).hide(WindowInsetsCompat.Type.ime())};ui.waitForIdle()
   scrollTap(local("Създайте план →","Generate plan →"));scan("plan-result",swipes=7)
   scrollTap(local("Преглед и експорт","Preview and export"));scan("export",dialog=true,swipes=1)
   tap(local("Затворете","Close"))
   home();tap(local("Настройки","Settings"));scan("settings",dialog=true)
   scrollTap(local("Покажете въведението отново","Replay introduction"))
   scan("intro-1");tap(local("Продължете →","Continue →"))
   scan("intro-2");tap(local("Продължете →","Continue →"))
   scan("intro-3");tap(local("Пропуснете","Skip"))
  }
 }
 @Test fun largeTextKeepsChromeAndIntroVisible() {
  home();language(false);scan("large-home",swipes=1)
  tap("Настройки");scan("large-settings",dialog=true,swipes=1)
  scrollTap("Покажете въведението отново")
  scan("large-intro-1",swipes=1);tap("Продължете →")
  scan("large-intro-2",swipes=1);tap("Продължете →")
  scan("large-intro-3",swipes=1);tap("Пропуснете")
 }

}
