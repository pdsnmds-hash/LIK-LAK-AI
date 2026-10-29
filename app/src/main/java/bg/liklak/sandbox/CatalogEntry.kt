package bg.liklak.sandbox

/** Catalog entries explicitly distinguish executable experiments from reading scenarios. */
data class CatalogEntry(val id: String,val titleBg: String,val titleEn: String,val summaryBg: String,val summaryEn: String,
 val category: String,val level: Int,val interactive: Boolean,val cloudGuide: Boolean,val toolNames: String) {
 fun title(en: Boolean)=if(en)titleEn else titleBg
 fun summary(en: Boolean)=if(en)summaryEn else summaryBg
 fun matches(query: String,en: Boolean,books: Map<String,Handbook>)=query.isBlank() ||
  (id+" "+title(en)+" "+summary(en)+" "+categoryLabel(category,en)+" "+toolNames).contains(query,true) || books[id]?.matches(query,en)==true
}
fun buildCatalog(cases: List<LabCase>,books: Map<String,Handbook>,sources: Map<String,Source>): List<CatalogEntry> {
 val cloud=setOf("openai","gemini","claude")
 val experiments=cases.map{c->CatalogEntry(c.id,c.title_bg,c.title_en,c.bg.problem,c.en.problem,c.category,c.level,true,c.refs.any{it in cloud},c.refs.joinToString(" "){sources[it]?.title?:it})}
 val scenarios=books.values.filter{it.type()=="scenario"}.map{b->CatalogEntry(b.id,b.title_bg,b.title_en,b.intro_bg,b.intro_en,
  requireNotNull(b.category),requireNotNull(b.level),false,b.tool_ids.orEmpty().any{it in cloud},b.tool_ids.orEmpty().joinToString(" "){books["tool-$it"]?.title_en?:it})}
 return (experiments+scenarios).sortedBy{it.id}
}
