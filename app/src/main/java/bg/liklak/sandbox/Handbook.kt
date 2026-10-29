package bg.liklak.sandbox

/** Authored, bundled educational content. Recipes are read/copied, never executed by Android. */
data class Handbook(
 val id: String, val title_bg: String, val title_en: String,
 val intro_bg: String, val intro_en: String,
 val sections: List<HandbookChapter>, val references: List<HandbookReference>,
 val kind: String?=null,val category: String?=null,val level: Int?=null,val tool_ids: List<String>?=null
) {
 fun type()=kind?:"case"
 fun title(en: Boolean) = title_en
 fun intro(en: Boolean) = intro_en
 fun matches(query: String, en: Boolean) = query.isBlank() ||
  (title(en)+" "+intro(en)).contains(query,true) || sections.any {it.matches(query,en)}
}
data class HandbookChapter(
 val id: String, val title_bg: String, val title_en: String,
 val body_bg: String, val body_en: String, val code: String,
 val language: String, val runnable: Boolean, val expected: String,
 val solution_bg: String, val solution_en: String, val code_bg: String?=null,val code_en: String?=null
) {
 fun title(en: Boolean) = title_en
 fun body(en: Boolean) = body_en
 fun code(en: Boolean) = code_en?:code
 fun solution(en: Boolean) = solution_en
 fun matches(query: String,en: Boolean) = query.isBlank() ||
  (title(en)+"\n"+body(en)+"\n"+code(en)+"\n"+expected+"\n"+solution(en)).contains(query,true)
}
data class HandbookReference(val title: String,val url: String)
