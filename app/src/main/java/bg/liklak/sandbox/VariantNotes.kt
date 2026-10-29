package bg.liklak.sandbox

/** Short, variant-specific explanations; no changes to the underlying algorithms. */
fun variantNote(id: String,index: Int,en: Boolean): String {
 val notes=when(id) {
  "01-routing"->listOf(
   "Matches invoice/payment and suggests billing. No message is sent." to "Matches invoice/payment and suggests billing. No message is sent.",
   "Finds billing and support signals, so it requests review." to "Finds billing and support signals, so it requests review.",
   "The word onboarding routes to hr; an operator still reviews it." to "The word onboarding routes to hr; an operator still reviews it.",
   "With no keywords, returns other without invented confidence." to "With no keywords, returns other without invented confidence.")
  "02-replies"->listOf(
   "Builds a concise draft using only the supplied policy." to "Builds a concise draft using only the supplied policy.",
   "Uses a formal tone while preserving the payment policy." to "Uses a formal tone while preserving the payment policy.",
   "An unknown intent produces a clarification question." to "An unknown intent produces a clarification question.",
   "Missing policy stops with an error instead of an invented reply." to "Missing policy stops with an error instead of an invented reply.")
  "03-sop"->listOf(
   "Splits on semicolons into five ordered steps." to "Splits on semicolons into five ordered steps.",
   "Skips empty segments and preserves the three actions in order." to "Skips empty segments and preserves the three actions in order.",
   "An empty process fails without inventing actions." to "An empty process fails without inventing actions.")
  "04-knowledge"->listOf(
   "Ranks shared words and returns the document passage KB-01." to "Ranks shared words and returns the document passage KB-01.",
   "Finds KB-02 about customer-request status." to "Finds KB-02 about customer-request status.",
   "No shared words gives not_found without an invented source." to "No shared words gives not_found without an invented source.")
  "05-extraction"->listOf(
   "Extracts 12 and office, then validates the range and category." to "Extracts 12 and office, then validates the range and category.",
   "Missing quantity stays null and the record is invalid." to "Missing quantity stays null and the record is invalid.",
   "Zero is extracted but fails the permitted range." to "Zero is extracted but fails the permitted range.",
   "The unknown category is not guessed; valid is false." to "The unknown category is not guessed; valid is false.")
  "06-cleaning"->listOf(
   "Trims whitespace, normalizes case and counts duplicates." to "Trims whitespace, normalizes case and counts duplicates.",
   "A and a become one code; the two empty rows are counted separately." to "A and a become one code; the two empty rows are counted separately.",
   "An empty list gives zero counts and no unique codes." to "An empty list gives zero counts and no unique codes.")
  "07-tests"->listOf(
   "Checks a short title, whitespace, 80 and 81 characters: four cases." to "Checks a short title, whitespace, 80 and 81 characters: four cases.",
   "Changes the limit to 20 and checks lengths 20 and 21." to "Changes the limit to 20 and checks lengths 20 and 21.",
   "Limit 0 is an invalid configuration and stops the run." to "Limit 0 is an invalid configuration and stops the run.")
  "08-review"->listOf(
   "Finds a direct eval call on line 1 without executing it." to "Finds a direct eval call on line 1 without executing it.",
   "The built-in int example has no eval finding; this is not a full audit." to "The built-in int example has no eval finding; this is not a full audit.",
   "Applies the same limited rule to eval on line 2." to "Applies the same limited rule to eval on line 2.")
  "09-brief"->listOf(
   "Builds a local brief with input, output, exclusions and human approval." to "Builds a local brief with input, output, exclusions and human approval.",
   "Describes server execution in the brief; it does not contact a server." to "Describes server execution in the brief; it does not contact a server.")
  "10-evaluation"->listOf(
   "Compares four pairs: three matches and one displayed error." to "Compares four pairs: three matches and one displayed error.",
   "Both predictions match: correct=2, total=2." to "Both predictions match: correct=2, total=2.",
   "Unequal lengths are rejected without truncating the arrays." to "Unequal lengths are rejected without truncating the arrays.",
   "An empty dataset is rejected without a fabricated accuracy score." to "An empty dataset is rejected without a fabricated accuracy score.")
  "11-documents"->listOf(
   "Extracts DOC-042, 125.00 and EUR from text. This is not OCR." to "Extracts DOC-042, 125.00 and EUR from text. This is not OCR.",
   "Extracts the new reference, 90.50 and USD from supplied text." to "Extracts the new reference, 90.50 and USD from supplied text.",
   "GBP is unsupported; amount and currency remain null." to "GBP is unsupported; amount and currency remain null.")
  "12-workflow"->listOf(
   "Stops at awaiting_approval. No external action occurs." to "Stops at awaiting_approval. No external action occurs.",
   "Approval prepares a manual export without sending anything." to "Approval prepares a manual export without sending anything.",
   "An unknown category is invalid even with approval." to "An unknown category is invalid even with approval.")
  else->error("Unknown experiment: $id")
 }
 return if(en) notes[index].second else notes[index].first
}
