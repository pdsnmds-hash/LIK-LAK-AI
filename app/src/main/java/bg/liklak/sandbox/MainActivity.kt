package bg.liklak.sandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity: ComponentActivity() {
 private val vm:LabViewModel by viewModels()
 override fun onCreate(savedInstanceState:Bundle?) {
  val splash=installSplashScreen()
  super.onCreate(savedInstanceState)
  splash.setKeepOnScreenCondition{!vm.settingsReady.value}
  enableEdgeToEdge()
  setContent{SandboxApp(vm)}
 }
}
