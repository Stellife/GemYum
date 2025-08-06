"""
Comprehensive Glycemic Index (GI) and Glycemic Load (GL) data
GI Scale: Low (0-55), Medium (56-69), High (70+)
GL Scale: Low (0-10), Medium (11-19), High (20+)
"""

# Glycemic Index mapping for common foods
GLYCEMIC_INDEX_MAP = {
    # ========== FRUITS ==========
    # Low GI Fruits (0-55)
    "apple": 36,
    "pear": 38,
    "orange": 43,
    "grapefruit": 25,
    "cherries": 22,
    "plum": 39,
    "peach": 42,
    "strawberries": 40,
    "strawberry": 40,
    "blackberries": 25,
    "raspberries": 32,
    "blueberries": 53,
    "grapes": 46,
    "kiwi": 53,
    "avocado": 15,
    
    # Medium GI Fruits (56-69)
    "mango": 60,
    "papaya": 59,
    "pineapple": 59,
    "cantaloupe": 65,
    "raisins": 64,
    
    # High GI Fruits (70+)
    "watermelon": 72,
    "dates": 103,
    
    # ========== VEGETABLES ==========
    # Low GI Vegetables (most vegetables)
    "broccoli": 15,
    "cauliflower": 15,
    "spinach": 15,
    "lettuce": 15,
    "kale": 15,
    "cabbage": 10,
    "mushroom": 15,
    "tomato": 15,
    "cucumber": 15,
    "bell pepper": 15,
    "pepper": 15,
    "asparagus": 15,
    "celery": 15,
    "zucchini": 15,
    "eggplant": 15,
    "green beans": 15,
    "carrots": 35,
    "carrot": 35,
    "sweet potato": 54,
    "yam": 54,
    
    # Medium GI Vegetables
    "corn": 60,
    "beets": 61,
    "beetroot": 61,
    
    # High GI Vegetables
    "potato": 85,
    "russet potato": 85,
    "instant mashed potato": 87,
    "pumpkin": 75,
    
    # ========== GRAINS & CEREALS ==========
    # Low GI Grains
    "quinoa": 53,
    "steel cut oats": 42,
    "oatmeal": 55,
    "oats": 55,
    "barley": 28,
    "bulgur": 48,
    "buckwheat": 54,
    
    # Medium GI Grains
    "brown rice": 68,
    "basmati rice": 58,
    "couscous": 65,
    "wild rice": 57,
    
    # High GI Grains
    "white rice": 73,
    "jasmine rice": 89,
    "instant rice": 87,
    "rice cakes": 82,
    "cornflakes": 81,
    "corn flakes": 81,
    "rice krispies": 82,
    "cheerios": 74,
    "instant oatmeal": 79,
    
    # ========== BREADS ==========
    # Low GI Breads
    "whole grain bread": 51,
    "whole wheat bread": 54,
    "sourdough bread": 54,
    "rye bread": 51,
    "pumpernickel": 46,
    "ezekiel bread": 36,
    
    # Medium GI Breads
    "pita bread": 57,
    "naan": 62,
    "croissant": 67,
    
    # High GI Breads
    "white bread": 75,
    "french bread": 95,
    "baguette": 95,
    "bagel": 72,
    "english muffin": 77,
    "hamburger bun": 75,
    "hot dog bun": 75,
    
    # ========== PASTA & NOODLES ==========
    # Low GI Pasta
    "whole wheat pasta": 42,
    "pasta": 49,  # al dente
    "spaghetti": 49,
    "fettuccine": 47,
    "linguine": 49,
    "ravioli": 39,
    "tortellini": 50,
    "soba noodles": 46,
    
    # Medium GI Pasta
    "rice noodles": 61,
    "udon noodles": 62,
    
    # High GI Pasta
    "instant noodles": 70,
    "overcooked pasta": 70,
    
    # ========== LEGUMES (all low GI) ==========
    "lentils": 32,
    "chickpeas": 28,
    "garbanzo beans": 28,
    "black beans": 30,
    "kidney beans": 24,
    "pinto beans": 39,
    "navy beans": 31,
    "soybeans": 16,
    "split peas": 25,
    "hummus": 6,
    
    # ========== DAIRY ==========
    # Low GI Dairy
    "milk": 31,
    "skim milk": 32,
    "whole milk": 27,
    "yogurt": 33,
    "greek yogurt": 11,
    "plain yogurt": 14,
    "cottage cheese": 10,
    "cheese": 0,  # Most cheeses have no carbs
    "cheddar": 0,
    "mozzarella": 0,
    "parmesan": 0,
    "butter": 0,
    
    # Medium GI Dairy
    "ice cream": 61,
    "frozen yogurt": 65,
    
    # ========== PROTEINS (most have GI of 0) ==========
    "chicken": 0,
    "beef": 0,
    "pork": 0,
    "fish": 0,
    "salmon": 0,
    "tuna": 0,
    "shrimp": 0,
    "eggs": 0,
    "egg": 0,
    "tofu": 15,
    "tempeh": 15,
    
    # ========== NUTS & SEEDS (all low GI) ==========
    "almonds": 15,
    "almond": 15,
    "walnuts": 15,
    "walnut": 15,
    "cashews": 22,
    "cashew": 22,
    "peanuts": 14,
    "peanut": 14,
    "pistachios": 15,
    "pecans": 10,
    "macadamia": 10,
    "chia seeds": 1,
    "flax seeds": 1,
    "pumpkin seeds": 10,
    "sunflower seeds": 35,
    
    # ========== SNACKS & SWEETS ==========
    # Low GI Snacks
    "dark chocolate": 23,
    "nuts": 15,
    "popcorn": 55,
    
    # Medium GI Snacks
    "milk chocolate": 49,
    "potato chips": 56,
    "tortilla chips": 63,
    
    # High GI Snacks
    "pretzels": 83,
    "crackers": 74,
    "rice crackers": 91,
    "jelly beans": 78,
    "gummy bears": 78,
    "donuts": 76,
    "donut": 76,
    "cookies": 77,
    "oreos": 77,
    "cake": 73,
    "muffin": 71,
    
    # ========== BEVERAGES ==========
    # Low GI Beverages
    "water": 0,
    "coffee": 0,
    "tea": 0,
    "diet soda": 0,
    "almond milk": 25,
    "soy milk": 34,
    
    # Medium GI Beverages
    "orange juice": 50,
    "apple juice": 41,
    "cranberry juice": 68,
    
    # High GI Beverages
    "soda": 70,
    "cola": 70,
    "coca-cola": 70,
    "pepsi": 70,
    "sprite": 70,
    "gatorade": 78,
    "energy drink": 70,
    
    # ========== FAST FOOD ITEMS ==========
    "french fries": 75,
    "fries": 75,
    "pizza": 60,
    "hamburger": 66,
    "cheeseburger": 66,
    "burrito": 55,
    "taco": 52,
    "fried chicken": 70,
    
    # ========== BREAKFAST ITEMS ==========
    "pancakes": 67,
    "waffles": 76,
    "french toast": 75,
    "granola": 61,
    "muesli": 56,
    
    # ========== CONDIMENTS & SAUCES ==========
    "ketchup": 55,
    "bbq sauce": 70,
    "honey": 61,
    "maple syrup": 54,
    "jam": 65,
    "peanut butter": 14,
    "nutella": 55,
}

def calculate_glycemic_load(glycemic_index, carbs_per_serving):
    """
    Calculate Glycemic Load
    GL = (GI × carbohydrate content per serving) / 100
    """
    if glycemic_index is None or carbs_per_serving is None:
        return None
    return round((glycemic_index * carbs_per_serving) / 100, 1)

def get_gi_category(gi_value):
    """Categorize GI value"""
    if gi_value is None:
        return None
    if gi_value <= 55:
        return "Low"
    elif gi_value <= 69:
        return "Medium"
    else:
        return "High"

def get_gl_category(gl_value):
    """Categorize GL value"""
    if gl_value is None:
        return None
    if gl_value <= 10:
        return "Low"
    elif gl_value <= 19:
        return "Medium"
    else:
        return "High"