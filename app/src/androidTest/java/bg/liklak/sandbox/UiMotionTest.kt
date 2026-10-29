package bg.liklak.sandbox

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiMotionTest {
 @Test fun localLottieAssetsRenderDifferentProgressAndSuccessFrames() {
  val app=ApplicationProvider.getApplicationContext<Application>()
  for(resource in listOf(R.raw.planner_progress,R.raw.saved_confirmation)) {
   val loaded=LottieCompositionFactory.fromRawResSync(app,resource)
   assertNull(loaded.exception)
   val composition=loaded.value!!
   assertTrue(composition.images.isEmpty());assertTrue(composition.warnings.isEmpty())
   val drawable=LottieDrawable();assertTrue(drawable.setComposition(composition))
   val w=composition.bounds.width();val h=composition.bounds.height()
   drawable.setBounds(0,0,w,h)
   fun render(progress:Float):Int {
    val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    drawable.progress=progress;drawable.draw(Canvas(bitmap))
    val pixels=IntArray(w*h);bitmap.getPixels(pixels,0,w,0,0,w,h);bitmap.recycle()
    assertTrue("Frame must contain visible vector content",pixels.any{it!=0})
    return pixels.contentHashCode()
   }
   assertNotEquals(render(0f),render(1f))
   if(resource==R.raw.planner_progress){assertNotEquals(render(0f),render(.5f));assertNotEquals(render(.5f),render(1f))}
  }
 }
 @Test fun saveFeedbackFollowsDurableCommitAndRepeatedSaveHasNewEvent()=runBlocking {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val store=ViewModelStore()
  val plan=AutomationPlan("motion-test-${System.nanoTime()}","data","Save feedback test","Rows","Clean rows","Reviewer","No duplicates")
  lateinit var vm:LabViewModel
  val db=SandboxDatabase.open(app)
  try {
   instrumentation.runOnMainSync{vm=LabViewModel(app,SavedStateHandle());store.put("motion",vm);vm.savePlan(plan);assertEquals("saving",vm.planSave.value.status)}
   val first=withTimeout(5000){vm.planSave.first{it.status=="success"}}
   assertEquals(plan.id,first.planId);assertEquals(1L,first.event)
   assertEquals(plan,AutomationPlan.decode(db.saved().observe().first().first{it.key=="plan:${plan.id}"}.input))
   instrumentation.runOnMainSync{vm.savePlan(plan)}
   val second=withTimeout(5000){vm.planSave.first{it.status=="success" && it.event>first.event}}
   assertEquals(2L,second.event)
  }finally{db.saved().delete("plan:${plan.id}");db.close();instrumentation.runOnMainSync{store.clear()}}
 }
}
