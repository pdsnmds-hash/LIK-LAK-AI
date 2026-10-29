package bg.liklak.sandbox

import androidx.room.Room
import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires a device; checks durable storage rather than an in-memory substitute. */
@RunWith(AndroidJUnit4::class)
class StorageTest {
 @Test fun savedConfigurationSurvivesDatabaseReopen() = runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val name="storage-test-${System.nanoTime()}.db"
  fun open()=Room.databaseBuilder(context,SandboxDatabase::class.java,name).build()
  val entry=SavedEntry("config:01-routing:test","01-routing","config","\"invoice error\"","My label")
  val first=open()
  try {first.saved().save(entry)} finally {first.close()}
  val second=open()
  try {assertEquals(entry,second.saved().observe().first().single());second.saved().delete(entry.key);assertTrue(second.saved().observe().first().isEmpty())}
  finally {second.close();context.deleteDatabase(name)}
 }
 @Test fun languageDoesNotChangeStableCaseIdsOrPersonalLabels() = runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val content=Content(context)
  val prefs=SettingsRepository(context)
  val original=prefs.settings.first()
  try {
   prefs.update(original.copy(english=false))
   assertTrue(prefs.settings.first().english)
   val reloaded=Content(context)
   assertEquals(content.cases.map{it.id},reloaded.cases.map{it.id})
   assertTrue(reloaded.cases.all{it.title(true).isNotBlank() && it.title(false).isNotBlank()})
   val another=SettingsRepository(context)
   assertTrue(another.settings.first().english)
  } finally {prefs.update(original)}
 }
 @Test fun cancelAndResetClearPreviousResult() {
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
   val app=ApplicationProvider.getApplicationContext<Application>()
   val store=ViewModelStore()
   val vm=LabViewModel(app,SavedStateHandle())
   store.put("test",vm)
   try {
    val c=vm.content.cases.first()
    vm.open(c);vm.run(c);vm.cancel()
    assertEquals("cancelled",vm.experiment.value.status)
    assertEquals("",vm.experiment.value.result)
    vm.select(c,1);vm.reset(c)
    assertEquals("idle",vm.experiment.value.status)
    assertEquals(Engine.pretty(Engine.variants(c)[0]),vm.experiment.value.input)
   } finally {store.clear()}
  }
 }

 @Test fun enrichedAssetsLoadWithReadingScenariosAndLocalizedPrompts() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val content=Content(context)
  assertEquals(110,content.handbooks.size)
  assertEquals(36,buildCatalog(content.cases,content.handbooks,content.sources).size)
  val prompt=content.handbooks.getValue("prompt-p01-routing").sections.first()
  assertEquals(prompt.code(true),prompt.code(false))
  assertTrue(content.handbooks.getValue("13-meeting").sections.none{it.runnable})
 }

 @Test fun rapidSettingsEditsStayImmediateAndPersistTheLastQuery() = runBlocking {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val prefs=SettingsRepository(app)
  val original=prefs.settings.first()
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val store=ViewModelStore()
  lateinit var vm: LabViewModel
  instrumentation.runOnMainSync {vm=LabViewModel(app,SavedStateHandle());store.put("typing",vm)}
  try {
   withTimeout(5000){vm.settings.first{it==original}}
   instrumentation.runOnMainSync {
    val target="13-meeting"
    target.indices.forEach {index->
     val typed=target.take(index+1)
     vm.update(vm.settings.value.copy(query=typed))
     assertEquals("Typing must not wait for DataStore",typed,vm.settings.value.query)
    }
   }
   withTimeout(5000){prefs.settings.first{it.query=="13-meeting"}}
   assertEquals("13-meeting",vm.settings.value.query)
  } finally {
   instrumentation.runOnMainSync {store.clear()}
   prefs.update(original)
  }
 }

 @Test fun aboutPhotosAreBundledAndDecodeOffline() {
  val app=ApplicationProvider.getApplicationContext<Context>()
  listOf("about-workspace.png","about-collaboration.png").forEach{file->
   val bitmap=app.assets.open("images/$file").use{android.graphics.BitmapFactory.decodeStream(it)}
   assertNotNull(bitmap);assertTrue(bitmap.width>=1000);assertTrue(bitmap.height>=500)
   bitmap.recycle()
  }
 }

 @Test fun automationPlansPersistAndAllMaterialLinksResolve() = runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val books=Content(context).handbooks
  planProcesses.forEach{process->listOf("local","cloud").forEach{mode->
   AutomationPlan(process=process.id,execution=mode).route(true).forEach{assertTrue("Missing planner link: ${it.articleId}",books.containsKey(it.articleId))}
  }}
  val name="plan-test-${System.nanoTime()}.db"
  fun open()=Room.databaseBuilder(context,SandboxDatabase::class.java,name).build()
  val plan=AutomationPlan("persist","data","CRM pilot","Test rows","Clean records","Operator","No duplicate keys","local")
  val db=open()
  try{db.saved().save(SavedEntry("plan:${plan.id}","","plan",plan.json(),plan.title))}finally{db.close()}
  val reopened=open()
  try{assertEquals(plan,AutomationPlan.decode(reopened.saved().observe().first().single().input))}finally{reopened.close();context.deleteDatabase(name)}
 }

}
