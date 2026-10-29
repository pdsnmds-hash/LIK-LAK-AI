package bg.liklak.sandbox

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTest {
 @Test fun completionPersistsWithoutChangingExistingSettings()=runBlocking {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val repo=SettingsRepository(app);val original=repo.settings.first()
  try {
   repo.update(original.copy(onboardingComplete=true))
   assertEquals(original.copy(onboardingComplete=true),SettingsRepository(app).settings.first())
   repo.update(original.copy(onboardingComplete=false))
   assertEquals(original.copy(onboardingComplete=false),SettingsRepository(app).settings.first())
  }finally{repo.update(original)}
 }
 @Test fun startupReadyExposesStoredIntroAndLanguageTogether()=runBlocking {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val repo=SettingsRepository(app);val original=repo.settings.first()
  val store=ViewModelStore();lateinit var vm:LabViewModel
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  try {
   repo.update(original.copy(english=true,onboardingComplete=true))
   instrumentation.runOnMainSync{vm=LabViewModel(app,SavedStateHandle());store.put("intro",vm)}
   withTimeout(5000){vm.settingsReady.first{it}}
   assertEquals(original.copy(english=true,onboardingComplete=true),vm.settings.value)
  }finally{instrumentation.runOnMainSync{store.clear()};repo.update(original)}
 }
 @Test fun generatedBrandAssetsAndNativeSplashResolveOffline() {
  val app=ApplicationProvider.getApplicationContext<Application>()
  val logo=BitmapFactory.decodeResource(app.resources,R.drawable.brand_mark)
  val art=BitmapFactory.decodeResource(app.resources,R.drawable.splash_art)
  assertNotNull(logo);assertTrue(logo.width>=512);assertEquals(logo.width,logo.height)
  assertNotNull(art);assertTrue(art.height>art.width)
  val icon=app.packageManager.getApplicationIcon(app.packageName)
  assertTrue(icon is AdaptiveIconDrawable)
  val preview=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
  icon.setBounds(0,0,256,256);icon.draw(Canvas(preview))
  java.io.File(app.cacheDir,"launcher-preview.png").outputStream().use{preview.compress(Bitmap.CompressFormat.PNG,100,it)}
  preview.recycle()
  val attributes=app.obtainStyledAttributes(R.style.LaunchTheme,intArrayOf(androidx.core.splashscreen.R.attr.postSplashScreenTheme,androidx.core.splashscreen.R.attr.windowSplashScreenAnimatedIcon))
  try{assertEquals(R.style.AppTheme,attributes.getResourceId(0,0));assertEquals(R.drawable.brand_icon_foreground,attributes.getResourceId(1,0))}finally{attributes.recycle()}
  logo.recycle();art.recycle()
 }
}
