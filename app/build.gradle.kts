import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }
// Codemagic prepare_signing.py writes this file; never commit its credentials.
val uploadPropertiesFile = rootProject.file("release/upload-key.properties")
val uploadProperties = Properties().apply {
 if (uploadPropertiesFile.isFile) uploadPropertiesFile.inputStream().use { load(it) }
}
android {
 namespace = "bg.liklak.sandbox"
 compileSdk = 37
 defaultConfig { applicationId = "bg.liklak.sandbox"; minSdk = 26; targetSdk = 36; versionCode = 8; versionName = "1.7.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; ndk { abiFilters += listOf("armeabi-v7a","arm64-v8a","x86","x86_64") } }
 signingConfigs {
  if (uploadPropertiesFile.isFile) create("release") {
   fun required(name:String):String = requireNotNull(uploadProperties.getProperty(name)?.takeIf { it.isNotEmpty() }) { "Missing release signing property: $name" }
   storeFile = rootProject.file(required("storeFile"))
   storePassword = required("storePassword")
   keyAlias = required("keyAlias")
   keyPassword = required("keyPassword")
  }
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = false
   if (uploadPropertiesFile.isFile) signingConfig = signingConfigs.getByName("release")
  }
 }
 androidResources { localeFilters += listOf("en") }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 testOptions { unitTests.isReturnDefaultValues = true }
}
// Debug builds remain independent of private signing files.
val requireReleaseSigning = tasks.register("requireReleaseSigning") {
 doLast { check(uploadPropertiesFile.isFile) { "Release signing is not configured. Run docs/codemagic/prepare_signing.py with a Codemagic identity first." } }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(requireReleaseSigning) }
kotlin { jvmToolchain(17) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2026.09.00"))
 implementation("androidx.activity:activity-compose:1.13.0")
 implementation("androidx.core:core-splashscreen:1.2.0")
 // AppCompat pulls Fragment transitively; prevent the old transitive version from shipping.
 constraints {
  implementation("androidx.fragment:fragment:1.9.1")
  implementation("androidx.core:core:1.19.1")
  implementation("androidx.core:core-ktx:1.19.1")
  implementation("androidx.appcompat:appcompat:1.8.0")
  implementation("androidx.appcompat:appcompat-resources:1.8.0")
 }
 implementation("androidx.compose.material3:material3")
 implementation("com.airbnb.android:lottie-compose:6.7.1")
 implementation("androidx.navigation:navigation-compose:2.10.2")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
 implementation("androidx.datastore:datastore-preferences:1.3.0")
 implementation("androidx.room:room-runtime:2.8.5")
 ksp("androidx.room:room-compiler:2.8.5")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
 implementation("com.google.code.gson:gson:2.15.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
