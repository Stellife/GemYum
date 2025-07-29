



We’re ready to proceed to the next critical phase! Storing + Documenting the results + Populating our RAG Documents/library with great Reinforcement Learning data-sets.
Below are my initial ideas, I’m more interested in what additional information would be helpful to store in this Document as we start to plan how we want to populate our RAG library + how we’re implementing this from a UX/UI perspective to appear as minimal user burden + pleasant to share feedback.  If a meal was identified trigger -> FEEDBACK CARD request:
Overall score Slider (0= Embarrassing - 5=Great)

Any Errors? + Option to share details of what went wrong:
* Food was completely wrong
* Food was slightly wrong (missing ingredients or mis-sized)
* Quantity was wrong
* Nutritional information feels wrong - multi-select.

Additional Meal Notes shared: (ex. Restaurant + details to compare later) - ex. if the user shares they got a chipotle chicken taco, we can start to track that better for future queries + see how off our initial USDA analysis is:
* Restaurant or Home-Cooked
  * If Restaurant: enter name
  * Enter meal + details
* Optional entry addition to enter correct (Nutritional info):
  * Value of nutritional item (/item)
  * (optional): source of information

* Option selector to write to Health Connect as a nutritional data point (Will implement the actual Health Connect integration in the next phase, but we should start thinking of UX/UI to mark the intention to write nutritional value to Health Connect)
  * If user provided their values write their values to Health Connect
  * If no user provided values, use Computed values

* Store Document with the following:
  * Insight Generated Date + time
  * Model Details:
    * Model name
    * Media Quality size
    * Image Analysis mode
  * Time for complete analysis
    * list of sub-times (session, prompt, image, inference, parsing, nutrition)
  * Did model return results: YES, Failed - not JSON, Failed - no ID
  * AI RESPONSE - Raw: JSON or other values
  * AI Response per food item:
    * Nutritional information (per item) with DV%:
  * (If more than 1 item) AI response for Total:
    * Nutritional item per total with DV%:
  * Was computed value written to Health Connect?
  * Human Score (0-5):
  * Human reported errors? - list: [Food was completely wrong, Food was slightly wrong, quantity was wrong, nutritional information feels wrong]
  * Human reported notes of errors:
  * Human shared Restaurant + Meal details:
  * Human corrected nutritional information?







**Additional Models:** - They're currently only TEXT preview.
* Gemma 3N-E2B-lm - https://huggingface.co/google/gemma-3n-E4B-it-litert-lm-preview
  * Download link - https://huggingface.co/google/gemma-3n-E2B-it-litert-lm-preview/resolve/main/gemma-3n-E2B-it-int4.litertlm?download=true
* Gemma 3N-E4B-lm - https://huggingface.co/google/gemma-3n-E4B-it-litert-lm-preview
    * Download Link - https://huggingface.co/google/gemma-3n-E4B-it-litert-lm-preview/resolve/main/gemma-3n-E4B-it-int4.litertlm?download=true