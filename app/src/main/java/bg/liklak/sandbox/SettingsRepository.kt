package bg.liklak.sandbox

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")
data class Settings(val english: Boolean=true, val theme: String="system", val query: String="", val category: String="all", val level: Int=0, val reverse: Boolean=false, val codeSize: Int=14, val approach: String="all", val onboardingComplete:Boolean=false)
class SettingsRepository(private val context: Context) {
 private object Keys {
  val english=booleanPreferencesKey("english"); val theme=stringPreferencesKey("theme"); val query=stringPreferencesKey("query"); val category=stringPreferencesKey("category"); val level=intPreferencesKey("level"); val reverse=booleanPreferencesKey("reverse"); val codeSize=intPreferencesKey("codeSize"); val approach=stringPreferencesKey("approach"); val onboardingComplete=booleanPreferencesKey("onboarding_complete")
 }
 val settings=context.settingsStore.data.map { p -> Settings(true,p[Keys.theme]?:"system",p[Keys.query]?:"",p[Keys.category]?:"all",p[Keys.level]?:0,p[Keys.reverse]?:false,p[Keys.codeSize]?:14,p[Keys.approach]?:"all",p[Keys.onboardingComplete]?:false) }
 suspend fun update(s: Settings) { context.settingsStore.edit { p -> p[Keys.english]=true;p[Keys.theme]=s.theme;p[Keys.query]=s.query;p[Keys.category]=s.category;p[Keys.level]=s.level;p[Keys.reverse]=s.reverse;p[Keys.codeSize]=s.codeSize;p[Keys.approach]=s.approach;p[Keys.onboardingComplete]=s.onboardingComplete } }
}
