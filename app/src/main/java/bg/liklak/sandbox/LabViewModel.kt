package bg.liklak.sandbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class Experiment(val input: String="", val status: String="idle", val result: String="", val variant: Int=0)
data class PlanSaveFeedback(val planId:String="",val status:String="idle",val event:Long=0)
class LabViewModel(application: Application, private val state: SavedStateHandle): AndroidViewModel(application) {
 val content=Content(application)
 private val prefs=SettingsRepository(application)
 private val database=SandboxDatabase.open(application)
 private val currentSettings=MutableStateFlow(Settings())
 val settings: StateFlow<Settings> = currentSettings.asStateFlow()
 private val settingsWrites=Channel<Settings>(Channel.CONFLATED)
 private var settingsEdited=false
 private val currentSettingsReady=MutableStateFlow(false)
 val settingsReady:StateFlow<Boolean> = currentSettingsReady.asStateFlow()
 init {
  viewModelScope.launch {
   val stored=try {prefs.settings.first()} catch(e:java.io.IOException){Settings()}
   if(!settingsEdited) currentSettings.value=stored
   currentSettingsReady.value=true
   // Persist in order without feeding delayed disk snapshots back into a live text field.
   for(next in settingsWrites) prefs.update(next)
  }
 }
 val saved=database.saved().observe().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val experiment=MutableStateFlow(Experiment())
 private val currentPlanSave=MutableStateFlow(PlanSaveFeedback())
 val planSave:StateFlow<PlanSaveFeedback> = currentPlanSave.asStateFlow()
 private var job: Job?=null
 private var activeId=""
 fun update(s: Settings) {
  settingsEdited=true
  currentSettings.value=s
  settingsWrites.trySend(s)
 }
 fun open(c: LabCase, restored: String?=null) {
  if(activeId==c.id && restored==null) return
  job?.cancel(); activeId=c.id
  val input=restored ?: state.get<String>("input:${c.id}") ?: Engine.pretty(Engine.variants(c)[0])
  val variant=Engine.variants(c).indexOfFirst { Engine.pretty(it)==input }.coerceAtLeast(0)
  experiment.value=Experiment(input=input,variant=variant)
 }
 fun select(c: LabCase,index: Int) {
  job?.cancel(); val input=Engine.pretty(Engine.variants(c)[index]); state["input:${c.id}"]=input
  experiment.value=Experiment(input=input,variant=index)
 }
 fun run(c: LabCase) {
  job?.cancel()
  val x=experiment.value
  experiment.value=x.copy(status="running",result="")
  job=viewModelScope.launch {
   try {
    // Yield to the dispatcher so cancellation can interrupt the run without fabricated timing logs.
    yield()
    val result=withContext(Dispatchers.Default) { ensureActive(); Engine.pretty(Engine.run(c.id,JsonParser.parseString(x.input),settings.value.english)) }
    ensureActive(); experiment.value=x.copy(status="success",result=result)
   } catch(e: CancellationException) { throw e }
   catch(e: Exception) { experiment.value=x.copy(status="error",result=tr(settings.value.english,"Invalid input. Check required fields and permitted values.","Invalid input. Check required fields and permitted values.")) }
  }
 }
 fun cancel() { job?.cancel(); experiment.value=experiment.value.copy(status="cancelled",result="") }
 fun reset(c: LabCase) { select(c,0) }
 fun bookmark(c: LabCase) { viewModelScope.launch { val key="case:${c.id}"; if(saved.value.any{it.key==key}) database.saved().delete(key) else database.saved().save(SavedEntry(key,c.id,"case","","")) } }
 fun save(c: LabCase,kind: String,label: String) { viewModelScope.launch {
  database.saved().save(SavedEntry("$kind:${c.id}:${System.currentTimeMillis()}",c.id,kind,if(kind=="config") experiment.value.input else c.code,label))
 } }
 fun savePlan(p:AutomationPlan) {
  if(!p.valid() || currentPlanSave.value.status=="saving")return
  val event=currentPlanSave.value.event
  currentPlanSave.value=PlanSaveFeedback(p.id,"saving",event)
  viewModelScope.launch {
   try {
    database.saved().save(SavedEntry("plan:${p.id}","","plan",p.json(),p.title))
    currentPlanSave.value=PlanSaveFeedback(p.id,"success",event+1)
   } catch(e:CancellationException){throw e}
   catch(e:Exception){currentPlanSave.value=PlanSaveFeedback(p.id,"error",event)}
  }
 }
 fun delete(key: String) { viewModelScope.launch {database.saved().delete(key)} }
 fun clear() { viewModelScope.launch { database.saved().clear() } }
 override fun onCleared() { database.close(); super.onCleared() }
}
