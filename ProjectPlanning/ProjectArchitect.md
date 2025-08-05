I'm developing a Gemma 3n AI Edge to enter into the Kaggle competition due in a couple of days.
I successfully implemented Gemma 3n and vision inference with AI Edge on my Pixel 9 pro.
Now I want to help with UX and improve the flow for this app.
The app, currently has the ability to rebuild with different model prompt/reasoning configurations. I want to now add a streaming chat capability for model discussions with Gemma 3N.

# Top Bar
* At all times must represent the AI Status (Initialized, preparing session, session ready, cleaning session, etc.)
* The settings icon also, must be present to change the model and the image parameters, reasoning, etc. 

# Overall app flow:
1) User submits image or text
2) MediaPipe LLM inference Runs on AI Edge
3) The model returns guesses of food in a JSON format to search on our local DB populated by USDA
4) JSON of food items sent to DB to identify nutrition information
5) Nutrition information presented to the user and they have the opportunity to save Meal nutrition Log to Health Connect Record.

#My focus now - There's a few variations I'm hoping to capture/better optimize.

## Single Shot - Quickly identify a single food item or simple meal.
* Situation: all the photo / ingredients can be captured in the image sent to inference.
* The Goal is Speed of inference + response while being correct.
* Example: Banana, plantains, salmon
* It does steps 1-5 above as rapidly as possible with little user feedback
* User Chat not necessary
* If the model is unable to find the item or nutritional context for the item, it may automatically feed the image + results into the Chain-of-thought / reasoning model below and ask to discuss with user.

## Discuss with Model - discuss with the model on ingredients that may or may not be in the photograph
* Potential Chain of Thought to initially analyze the food and ask the user for additional feedback.
* Multimodal: vision + text inference (audio not needed)
* Streaming Chat + discussion with model
  Example 1: Model needs help with ingredients or context not photographed
- Model: I identified a Taco, Can you let me know if it's from a specific restaurant chain and ingredients that may not be visible from the photo.
  -- User (response 1): Chipotle, Honey Chicken Tacos, with cheese, sour cream, etc.
  -- Model either searches users inputs into DB without needing image capabilities again.
  -- User (response 2): vague
  -- Model reprocesses image Inference with additional context from User
  Example 2: Model needs help with multiple ingredients/foods
- Model: I see a salad, can you help me break down the ingredients?
  ** Example 3: Model is incorrect and human gives feedback
  -- example: Model thinks there's 4 tacos, but human says there are 3.
* Ingredients confirmed with Human
* Nutritional information confirmed with Human
* Ask if human wants to record the meal record to Health Connect
## Text only - No Image Inference - Quick text interface and streaming
* Image/Vision inference not necessary.
* Streaming/Chat only discussion to identify meal/food


-------------------------------
#Gemini 2.5 Pro Proposal - Please review the following carefully, but develop your own thoughts + opinions on how to best implement the goals above.
1. The Three-Path UI: Your Foundation
   Stick with your clear, task-oriented main screen. This is your strength.
* Path A: "Snap & Log" (One-Shot Vision)
* Path B: "Analyze & Chat" (Image + Discussion)
* Path C: "Text Log" (Text-Only Chat)
  A Chip at the top showing model status (Initializing, Ready) is a perfect, professional touch.
2. Architecture for Rapid, Focused Development
   State Management:Use a single MainViewModel to manage which path the user is in (ScreenState). When they select a path, navigate to a dedicated Composable screen for that experience, each with its own focused ViewModel.
* SnapScreenViewModel
* ChatScreenViewModel (Can be reused for both Discuss and Text-only modes)

Path A: "Snap & Log" - The Goal is Speed & a Seamless Failure Case
This path needs to be lightning-fast and intelligent when it fails.
* Prompting is Everything: Use a strict, non-negotiable prompt to force JSON output.
    * System Prompt: "You are a food recognition API. Your only job is to analyze the image and return a valid JSON object listing the identified foods. Do not add any explanation or conversational text. The format must be: {\"foods\": [{\"name\": \"food item 1\"}, {\"name\": \"food item 2\"}]}. If you cannot identify any food with high confidence, return {\"foods\": []}."
* Execution Flow:
    1. User takes a photo.
    2. SnapScreenViewModel launches a stateless LlmInference session.
    3. It calls generateResponse() with your strict prompt.
    4. Success: The returned string is parsed as JSON. The list of food names is sent to your local DB for nutrition info. Results are displayed.
    5. Failure (The Impressive Part): The returned JSON is {"foods": []}.
        * DO NOT show an error message like "Failed."
        * DO show a Dialog or BottomSheet.
        * Message: "I'm having trouble with this one. Would you like to discuss the ingredients with me?"
        * Buttons: "Yes, let's chat" / "No, cancel".
        * Action: If "Yes," navigate to the "Analyze & Chat" screen, passing the image URI as an argument. This transition is a key "wow" factor, showing the app is smart enough to ask for help.

Path B: "Analyze & Chat" - The Goal is Showcasing an AI Agent
This is where you implement Function Calling. It is the single most impressive feature you can add in this timeframe. RAG is too complex for 24 hours.
* Define Your Tools (Keep it simple!):
    1. getNutritionFromDB(foodName: String): This is your killer function. The LLM can now directly query your database.
    2. requestUserInput(question: String): This allows the model to explicitly ask for help, making the conversation feel intelligent and guided.
* Prompting for Function Calling:
    * System Prompt: "You are a nutrition assistant with access to tools. Your goal is to identify all ingredients in the user's meal. Start by analyzing the image. If you are uncertain about anything (like dressings, sauces, or hidden items), use the 'requestUserInput' tool to ask the user a question. When you have confidently identified an ingredient, use the 'getNutritionFromDB' tool to get its nutritional information."
* Execution Flow (The Agent Loop):
    1. The ChatScreenViewModel is initialized with the image URI (either from the main menu or the transition from Path A).
    2. It maintains a single, continuous LlmInference session.
    3. When the user sends a message (or on initial load), it calls generateResponse().
    4. The ViewModel checks the response type:
        * If it's a Tool Call:
            * If requestUserInput, display the question in the chat UI as a message from the AI.
            * If getNutritionFromDB, execute the query against your local database, show a "loading" indicator, and then send the result back to the LLM in the next turn.
        * If it's a standard text response: Display the message in the chat UI. This loop—where the LLM decides to either talk or use a tool—is the core of a modern AI agent and will be highly impressive.

Path C: "Text Log" - The Goal is Utility and Code Reuse
This is your safety net. It demonstrates the breadth of the app's capabilities with minimal extra effort.
* Architecture: Reuse the exact same ChatScreen and ChatScreenViewModel from Path B.
* Difference: Simply initialize it without an image URI.
* Prompting: Use the same Function Calling prompt as Path B, but remove the instruction to "Analyze the image."
    * System Prompt: "You are a nutrition assistant with access to tools. Your goal is to identify all ingredients in the user's meal based on their description... (etc.)"
* Benefit: This path automatically inherits your Function Calling capabilities. The user can describe a meal, and the LLM can still use getNutritionFromDB to find nutrition info. This is efficient and powerful.
  Final Advice for the 24-Hour Sprint
1. Don't Get Fancy on UI: Use standard Material 3 components. Card, Button, Dialog, CircularProgressIndicator. A clean, functional, and bug-free UI is better than a flashy, broken one.
2. Focus on the "Golden Paths": Make sure the Snap -> Success, Snap -> Fail -> Chat, and Chat -> Function Call flows work perfectly.
3. In Your Submission Video/README: Explicitly call out your architecture. Say things like:
    * "We implemented a multi-path design to tailor the experience to the user's need: a rapid 'Snap & Log', a powerful 'Analyze & Chat', and a simple 'Text Log'."
    * "Our 'Analyze & Chat' mode transforms Gemma 3n into an on-device agent using Function Calling. The model can autonomously decide to query our local USDA nutrition database or ask the user for clarifying information."
    * "We designed an intelligent failure-to-success pipeline, where the one-shot model seamlessly hands off complex images to the conversational agent."

