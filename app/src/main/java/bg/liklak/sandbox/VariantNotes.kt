package bg.liklak.sandbox

/** Short, variant-specific explanations; no changes to the underlying algorithms. */
fun variantNote(id: String,index: Int,en: Boolean): String {
 val notes=when(id) {
  "01-routing"->listOf(
   "Търси invoice/payment и предлага billing. Не изпраща съобщение." to "Matches invoice/payment and suggests billing. No message is sent.",
   "Открива две теми — billing и support — и иска преглед." to "Finds billing and support signals, so it requests review.",
   "Думата onboarding насочва към hr; решението остава за оператор." to "The word onboarding routes to hr; an operator still reviews it.",
   "Без ключови думи връща other, без измислена увереност." to "With no keywords, returns other without invented confidence.")
  "02-replies"->listOf(
   "Сглобява кратка чернова само от подаденото правило." to "Builds a concise draft using only the supplied policy.",
   "Променя тона на официален и запазва условието за плащане." to "Uses a formal tone while preserving the payment policy.",
   "При неизвестно намерение задава уточняващ въпрос." to "An unknown intent produces a clarification question.",
   "Без правило спира с грешка, вместо да измисля отговор." to "Missing policy stops with an error instead of an invented reply.")
  "03-sop"->listOf(
   "Разделя текста по ; в пет подредени стъпки." to "Splits on semicolons into five ordered steps.",
   "Пропуска празните части и пази реда на трите действия." to "Skips empty segments and preserves the three actions in order.",
   "Празният процес води до грешка, без измислени действия." to "An empty process fails without inventing actions.")
  "04-knowledge"->listOf(
   "Сравнява общи думи и връща откъса KB-01 за документи." to "Ranks shared words and returns the document passage KB-01.",
   "Намира KB-02 за статус на клиентски заявки." to "Finds KB-02 about customer-request status.",
   "Без общи думи връща not_found, без измислен източник." to "No shared words gives not_found without an invented source.")
  "05-extraction"->listOf(
   "Извлича 12 и office, после проверява диапазона и категорията." to "Extracts 12 and office, then validates the range and category.",
   "Липсващото количество остава null; записът не е валиден." to "Missing quantity stays null and the record is invalid.",
   "Нулата е извлечена, но е извън разрешения диапазон." to "Zero is extracted but fails the permitted range.",
   "Непознатата категория не се отгатва; valid е false." to "The unknown category is not guessed; valid is false.")
  "06-cleaning"->listOf(
   "Премахва крайни интервали, уеднаквява регистъра и брои дубликати." to "Trims whitespace, normalizes case and counts duplicates.",
   "A и a стават един код; двата празни реда се броят отделно." to "A and a become one code; the two empty rows are counted separately.",
   "Празен списък връща нулеви броячи и няма уникални кодове." to "An empty list gives zero counts and no unique codes.")
  "07-tests"->listOf(
   "Проверява кратко, празно, 80 и 81 знака — четири гранични случая." to "Checks a short title, whitespace, 80 and 81 characters: four cases.",
   "Сменя границата на 20 и проверява дължини 20 и 21." to "Changes the limit to 20 and checks lengths 20 and 21.",
   "Лимит 0 е невалидна конфигурация и спира изпълнението." to "Limit 0 is an invalid configuration and stops the run.")
  "08-review"->listOf(
   "Намира пряк eval на ред 1, без да изпълнява кода." to "Finds a direct eval call on line 1 without executing it.",
   "Вграденият int пример няма eval находка; това не е пълен одит." to "The built-in int example has no eval finding; this is not a full audit.",
   "Показва същото ограничено правило за eval на ред 2." to "Applies the same limited rule to eval on line 2.")
  "09-brief"->listOf(
   "Събира вход, изход, ограничения и човешко одобрение в локален бриф." to "Builds a local brief with input, output, exclusions and human approval.",
   "Описва сървърно изпълнение в брифа; не извиква сървър." to "Describes server execution in the brief; it does not contact a server.")
  "10-evaluation"->listOf(
   "Сравнява четири двойки: 3 съвпадения и една показана грешка." to "Compares four pairs: three matches and one displayed error.",
   "Двете предвиждания съвпадат: correct=2, total=2." to "Both predictions match: correct=2, total=2.",
   "Различните дължини се отхвърлят, без скъсяване на масивите." to "Unequal lengths are rejected without truncating the arrays.",
   "Празният набор се отхвърля, без фиктивна точност." to "An empty dataset is rejected without a fabricated accuracy score.")
  "11-documents"->listOf(
   "От готов текст извлича DOC-042, 125.00 и EUR. Това не е OCR." to "Extracts DOC-042, 125.00 and EUR from text. This is not OCR.",
   "Извлича новата референция, 90.50 и USD от готовия текст." to "Extracts the new reference, 90.50 and USD from supplied text.",
   "GBP не е разрешена валута; сумата и валутата остават null." to "GBP is unsupported; amount and currency remain null.")
  "12-workflow"->listOf(
   "Спира на awaiting_approval. Не извършва външно действие." to "Stops at awaiting_approval. No external action occurs.",
   "След одобрение подготвя ръчен експорт, без изпращане." to "Approval prepares a manual export without sending anything.",
   "Непозната категория е invalid дори при одобрение." to "An unknown category is invalid even with approval.")
  else->error("Unknown experiment: $id")
 }
 return if(en) notes[index].second else notes[index].first
}
