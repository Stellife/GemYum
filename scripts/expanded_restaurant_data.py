"""
Expanded restaurant nutrition data including more chains and menu items
"""

EXPANDED_RESTAURANT_DATA = {
    # ========== COFFEE CHAINS ==========
    "Dunkin'": [
        {
            "name": "Original Blend Coffee (Medium)",
            "serving_size": "14 fl oz",
            "calories": 5,
            "total_fat_g": 0,
            "saturated_fat_g": 0,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 5,
            "total_carbohydrate_g": 1,
            "dietary_fiber_g": 0,
            "sugars_g": 0,
            "protein_g": 0,
            "caffeine_mg": 210
        },
        {
            "name": "Glazed Donut",
            "serving_size": "1 donut",
            "calories": 260,
            "total_fat_g": 14,
            "saturated_fat_g": 6,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 330,
            "total_carbohydrate_g": 31,
            "dietary_fiber_g": 1,
            "sugars_g": 12,
            "protein_g": 3
        },
        {
            "name": "Bacon, Egg & Cheese on Croissant",
            "serving_size": "1 sandwich",
            "calories": 520,
            "total_fat_g": 33,
            "saturated_fat_g": 14,
            "trans_fat_g": 0,
            "cholesterol_mg": 195,
            "sodium_mg": 1090,
            "total_carbohydrate_g": 37,
            "dietary_fiber_g": 1,
            "sugars_g": 8,
            "protein_g": 18
        },
        {
            "name": "Iced Caramel Macchiato (Medium)",
            "serving_size": "24 fl oz",
            "calories": 290,
            "total_fat_g": 9,
            "saturated_fat_g": 5,
            "trans_fat_g": 0,
            "cholesterol_mg": 30,
            "sodium_mg": 150,
            "total_carbohydrate_g": 45,
            "dietary_fiber_g": 0,
            "sugars_g": 42,
            "protein_g": 10,
            "caffeine_mg": 185
        },
        {
            "name": "Boston Kreme Donut",
            "serving_size": "1 donut",
            "calories": 300,
            "total_fat_g": 16,
            "saturated_fat_g": 7,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 370,
            "total_carbohydrate_g": 37,
            "dietary_fiber_g": 1,
            "sugars_g": 15,
            "protein_g": 3
        }
    ],
    
    # ========== PIZZA CHAINS ==========
    "Papa John's": [
        {
            "name": "Pepperoni Pizza (Large, Original Crust)",
            "serving_size": "1 slice",
            "calories": 330,
            "total_fat_g": 14,
            "saturated_fat_g": 5,
            "trans_fat_g": 0,
            "cholesterol_mg": 30,
            "sodium_mg": 840,
            "total_carbohydrate_g": 38,
            "dietary_fiber_g": 2,
            "sugars_g": 5,
            "protein_g": 13
        },
        {
            "name": "The Works Pizza (Large, Original Crust)",
            "serving_size": "1 slice",
            "calories": 340,
            "total_fat_g": 14,
            "saturated_fat_g": 5,
            "trans_fat_g": 0,
            "cholesterol_mg": 30,
            "sodium_mg": 900,
            "total_carbohydrate_g": 39,
            "dietary_fiber_g": 3,
            "sugars_g": 5,
            "protein_g": 14
        },
        {
            "name": "Cheese Pizza (Large, Thin Crust)",
            "serving_size": "1 slice",
            "calories": 270,
            "total_fat_g": 12,
            "saturated_fat_g": 5,
            "trans_fat_g": 0,
            "cholesterol_mg": 25,
            "sodium_mg": 670,
            "total_carbohydrate_g": 27,
            "dietary_fiber_g": 1,
            "sugars_g": 3,
            "protein_g": 12
        },
        {
            "name": "Garlic Knots",
            "serving_size": "8 knots",
            "calories": 400,
            "total_fat_g": 16,
            "saturated_fat_g": 3,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 940,
            "total_carbohydrate_g": 54,
            "dietary_fiber_g": 2,
            "sugars_g": 4,
            "protein_g": 10
        },
        {
            "name": "Chicken Wings (Buffalo)",
            "serving_size": "6 wings",
            "calories": 470,
            "total_fat_g": 35,
            "saturated_fat_g": 9,
            "trans_fat_g": 0,
            "cholesterol_mg": 155,
            "sodium_mg": 2240,
            "total_carbohydrate_g": 7,
            "dietary_fiber_g": 0,
            "sugars_g": 1,
            "protein_g": 34
        }
    ],
    
    "Little Caesars": [
        {
            "name": "Hot-N-Ready Classic Pepperoni",
            "serving_size": "1 slice",
            "calories": 280,
            "total_fat_g": 11,
            "saturated_fat_g": 5,
            "trans_fat_g": 0,
            "cholesterol_mg": 25,
            "sodium_mg": 610,
            "total_carbohydrate_g": 32,
            "dietary_fiber_g": 2,
            "sugars_g": 3,
            "protein_g": 14
        },
        {
            "name": "Crazy Bread",
            "serving_size": "1 piece",
            "calories": 100,
            "total_fat_g": 3,
            "saturated_fat_g": 0.5,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 150,
            "total_carbohydrate_g": 16,
            "dietary_fiber_g": 1,
            "sugars_g": 1,
            "protein_g": 3
        },
        {
            "name": "Italian Cheese Bread",
            "serving_size": "1 piece",
            "calories": 130,
            "total_fat_g": 6,
            "saturated_fat_g": 2.5,
            "trans_fat_g": 0,
            "cholesterol_mg": 10,
            "sodium_mg": 200,
            "total_carbohydrate_g": 13,
            "dietary_fiber_g": 1,
            "sugars_g": 1,
            "protein_g": 6
        }
    ],
    
    # ========== MEXICAN CHAINS ==========
    "Qdoba": [
        {
            "name": "Chicken Burrito Bowl",
            "serving_size": "1 bowl",
            "calories": 655,
            "total_fat_g": 22,
            "saturated_fat_g": 8,
            "trans_fat_g": 0,
            "cholesterol_mg": 135,
            "sodium_mg": 1370,
            "total_carbohydrate_g": 67,
            "dietary_fiber_g": 12,
            "sugars_g": 5,
            "protein_g": 48
        },
        {
            "name": "Steak Quesadilla",
            "serving_size": "1 quesadilla",
            "calories": 895,
            "total_fat_g": 45,
            "saturated_fat_g": 21,
            "trans_fat_g": 0,
            "cholesterol_mg": 140,
            "sodium_mg": 1885,
            "total_carbohydrate_g": 69,
            "dietary_fiber_g": 4,
            "sugars_g": 6,
            "protein_g": 47
        },
        {
            "name": "Chips & Queso",
            "serving_size": "Regular",
            "calories": 730,
            "total_fat_g": 43,
            "saturated_fat_g": 15,
            "trans_fat_g": 0,
            "cholesterol_mg": 50,
            "sodium_mg": 1350,
            "total_carbohydrate_g": 73,
            "dietary_fiber_g": 5,
            "sugars_g": 4,
            "protein_g": 17
        }
    ],
    
    "Moe's Southwest Grill": [
        {
            "name": "Homewrecker Burrito (Chicken)",
            "serving_size": "1 burrito",
            "calories": 865,
            "total_fat_g": 35,
            "saturated_fat_g": 13,
            "trans_fat_g": 0,
            "cholesterol_mg": 115,
            "sodium_mg": 2050,
            "total_carbohydrate_g": 88,
            "dietary_fiber_g": 13,
            "sugars_g": 7,
            "protein_g": 45
        },
        {
            "name": "Stack (Beef)",
            "serving_size": "2 tacos",
            "calories": 740,
            "total_fat_g": 38,
            "saturated_fat_g": 14,
            "trans_fat_g": 0,
            "cholesterol_mg": 95,
            "sodium_mg": 1520,
            "total_carbohydrate_g": 62,
            "dietary_fiber_g": 9,
            "sugars_g": 5,
            "protein_g": 35
        }
    ],
    
    # ========== SANDWICH CHAINS ==========
    "Jimmy John's": [
        {
            "name": "#4 Turkey Tom",
            "serving_size": "1 sandwich",
            "calories": 515,
            "total_fat_g": 26,
            "saturated_fat_g": 4,
            "trans_fat_g": 0,
            "cholesterol_mg": 45,
            "sodium_mg": 1380,
            "total_carbohydrate_g": 49,
            "dietary_fiber_g": 3,
            "sugars_g": 8,
            "protein_g": 25
        },
        {
            "name": "#9 Italian Night Club",
            "serving_size": "1 sandwich",
            "calories": 910,
            "total_fat_g": 56,
            "saturated_fat_g": 15,
            "trans_fat_g": 0,
            "cholesterol_mg": 95,
            "sodium_mg": 2750,
            "total_carbohydrate_g": 52,
            "dietary_fiber_g": 3,
            "sugars_g": 9,
            "protein_g": 42
        },
        {
            "name": "Tuna Salad Unwich",
            "serving_size": "1 unwich",
            "calories": 270,
            "total_fat_g": 16,
            "saturated_fat_g": 2.5,
            "trans_fat_g": 0,
            "cholesterol_mg": 40,
            "sodium_mg": 820,
            "total_carbohydrate_g": 11,
            "dietary_fiber_g": 3,
            "sugars_g": 6,
            "protein_g": 20
        }
    ],
    
    "Jersey Mike's": [
        {
            "name": "Mike's Way Sub (Regular #13)",
            "serving_size": "1 regular sub",
            "calories": 680,
            "total_fat_g": 35,
            "saturated_fat_g": 13,
            "trans_fat_g": 0,
            "cholesterol_mg": 85,
            "sodium_mg": 1680,
            "total_carbohydrate_g": 56,
            "dietary_fiber_g": 4,
            "sugars_g": 9,
            "protein_g": 36
        },
        {
            "name": "Philly Cheese Steak (Regular)",
            "serving_size": "1 regular sub",
            "calories": 740,
            "total_fat_g": 29,
            "saturated_fat_g": 13,
            "trans_fat_g": 0,
            "cholesterol_mg": 95,
            "sodium_mg": 1320,
            "total_carbohydrate_g": 68,
            "dietary_fiber_g": 4,
            "sugars_g": 10,
            "protein_g": 47
        }
    ],
    
    # ========== CASUAL DINING ==========
    "Olive Garden": [
        {
            "name": "Fettuccine Alfredo",
            "serving_size": "1 dinner portion",
            "calories": 1220,
            "total_fat_g": 75,
            "saturated_fat_g": 45,
            "trans_fat_g": 2.5,
            "cholesterol_mg": 235,
            "sodium_mg": 1200,
            "total_carbohydrate_g": 99,
            "dietary_fiber_g": 5,
            "sugars_g": 7,
            "protein_g": 36
        },
        {
            "name": "Breadstick",
            "serving_size": "1 breadstick",
            "calories": 140,
            "total_fat_g": 2,
            "saturated_fat_g": 0.5,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 460,
            "total_carbohydrate_g": 25,
            "dietary_fiber_g": 1,
            "sugars_g": 1,
            "protein_g": 5
        },
        {
            "name": "Chicken Parmigiana",
            "serving_size": "1 dinner portion",
            "calories": 1060,
            "total_fat_g": 52,
            "saturated_fat_g": 17,
            "trans_fat_g": 1,
            "cholesterol_mg": 145,
            "sodium_mg": 2180,
            "total_carbohydrate_g": 88,
            "dietary_fiber_g": 7,
            "sugars_g": 16,
            "protein_g": 58
        },
        {
            "name": "House Salad (with dressing)",
            "serving_size": "1 bowl",
            "calories": 290,
            "total_fat_g": 19,
            "saturated_fat_g": 3.5,
            "trans_fat_g": 0,
            "cholesterol_mg": 10,
            "sodium_mg": 680,
            "total_carbohydrate_g": 25,
            "dietary_fiber_g": 3,
            "sugars_g": 7,
            "protein_g": 5
        }
    ],
    
    "Applebee's": [
        {
            "name": "Classic Cheeseburger",
            "serving_size": "1 burger with fries",
            "calories": 1250,
            "total_fat_g": 70,
            "saturated_fat_g": 23,
            "trans_fat_g": 2,
            "cholesterol_mg": 165,
            "sodium_mg": 2310,
            "total_carbohydrate_g": 101,
            "dietary_fiber_g": 7,
            "sugars_g": 14,
            "protein_g": 51
        },
        {
            "name": "Boneless Wings (Classic Buffalo)",
            "serving_size": "1 order",
            "calories": 870,
            "total_fat_g": 46,
            "saturated_fat_g": 9,
            "trans_fat_g": 0,
            "cholesterol_mg": 135,
            "sodium_mg": 3590,
            "total_carbohydrate_g": 67,
            "dietary_fiber_g": 5,
            "sugars_g": 3,
            "protein_g": 45
        },
        {
            "name": "Oriental Chicken Salad",
            "serving_size": "1 salad",
            "calories": 1420,
            "total_fat_g": 99,
            "saturated_fat_g": 15,
            "trans_fat_g": 0,
            "cholesterol_mg": 110,
            "sodium_mg": 1830,
            "total_carbohydrate_g": 96,
            "dietary_fiber_g": 11,
            "sugars_g": 58,
            "protein_g": 43
        }
    ],
    
    "Chili's": [
        {
            "name": "Big Mouth Burgers - The Oldtimer",
            "serving_size": "1 burger with fries",
            "calories": 1650,
            "total_fat_g": 94,
            "saturated_fat_g": 29,
            "trans_fat_g": 2,
            "cholesterol_mg": 175,
            "sodium_mg": 3280,
            "total_carbohydrate_g": 133,
            "dietary_fiber_g": 10,
            "sugars_g": 21,
            "protein_g": 65
        },
        {
            "name": "Chicken Crispers",
            "serving_size": "1 order",
            "calories": 1320,
            "total_fat_g": 80,
            "saturated_fat_g": 14,
            "trans_fat_g": 0,
            "cholesterol_mg": 105,
            "sodium_mg": 3930,
            "total_carbohydrate_g": 104,
            "dietary_fiber_g": 7,
            "sugars_g": 14,
            "protein_g": 49
        },
        {
            "name": "Chips & Salsa",
            "serving_size": "1 basket",
            "calories": 910,
            "total_fat_g": 45,
            "saturated_fat_g": 6,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 3670,
            "total_carbohydrate_g": 121,
            "dietary_fiber_g": 12,
            "sugars_g": 14,
            "protein_g": 12
        }
    ],
    
    "TGI Friday's": [
        {
            "name": "Loaded Potato Skins",
            "serving_size": "1 order",
            "calories": 1430,
            "total_fat_g": 96,
            "saturated_fat_g": 48,
            "trans_fat_g": 0,
            "cholesterol_mg": 195,
            "sodium_mg": 2070,
            "total_carbohydrate_g": 93,
            "dietary_fiber_g": 10,
            "sugars_g": 3,
            "protein_g": 49
        },
        {
            "name": "Jack Daniel's Ribs (Full Rack)",
            "serving_size": "1 rack with sides",
            "calories": 1960,
            "total_fat_g": 94,
            "saturated_fat_g": 35,
            "trans_fat_g": 0,
            "cholesterol_mg": 325,
            "sodium_mg": 3810,
            "total_carbohydrate_g": 176,
            "dietary_fiber_g": 9,
            "sugars_g": 117,
            "protein_g": 98
        }
    ],
    
    # ========== ASIAN CHAINS ==========
    "Panda Express": [
        {
            "name": "Orange Chicken",
            "serving_size": "5.7 oz",
            "calories": 490,
            "total_fat_g": 23,
            "saturated_fat_g": 5,
            "trans_fat_g": 0,
            "cholesterol_mg": 80,
            "sodium_mg": 820,
            "total_carbohydrate_g": 51,
            "dietary_fiber_g": 2,
            "sugars_g": 19,
            "protein_g": 25
        },
        {
            "name": "Beijing Beef",
            "serving_size": "5.6 oz",
            "calories": 470,
            "total_fat_g": 26,
            "saturated_fat_g": 6,
            "trans_fat_g": 0,
            "cholesterol_mg": 55,
            "sodium_mg": 890,
            "total_carbohydrate_g": 46,
            "dietary_fiber_g": 3,
            "sugars_g": 24,
            "protein_g": 14
        },
        {
            "name": "Chow Mein",
            "serving_size": "9.4 oz",
            "calories": 510,
            "total_fat_g": 22,
            "saturated_fat_g": 4,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 1060,
            "total_carbohydrate_g": 65,
            "dietary_fiber_g": 5,
            "sugars_g": 9,
            "protein_g": 13
        },
        {
            "name": "Fried Rice",
            "serving_size": "9.3 oz",
            "calories": 520,
            "total_fat_g": 16,
            "saturated_fat_g": 3,
            "trans_fat_g": 0,
            "cholesterol_mg": 120,
            "sodium_mg": 850,
            "total_carbohydrate_g": 85,
            "dietary_fiber_g": 2,
            "sugars_g": 3,
            "protein_g": 11
        }
    ],
    
    "P.F. Chang's": [
        {
            "name": "Chicken Lettuce Wraps",
            "serving_size": "1 order",
            "calories": 710,
            "total_fat_g": 34,
            "saturated_fat_g": 6,
            "trans_fat_g": 0,
            "cholesterol_mg": 105,
            "sodium_mg": 2280,
            "total_carbohydrate_g": 62,
            "dietary_fiber_g": 7,
            "sugars_g": 34,
            "protein_g": 38
        },
        {
            "name": "Mongolian Beef",
            "serving_size": "1 dish",
            "calories": 790,
            "total_fat_g": 33,
            "saturated_fat_g": 9,
            "trans_fat_g": 0,
            "cholesterol_mg": 180,
            "sodium_mg": 4070,
            "total_carbohydrate_g": 68,
            "dietary_fiber_g": 4,
            "sugars_g": 45,
            "protein_g": 56
        }
    ],
    
    # ========== BREAKFAST CHAINS ==========
    "IHOP": [
        {
            "name": "Original Buttermilk Pancakes (Stack of 5)",
            "serving_size": "5 pancakes",
            "calories": 660,
            "total_fat_g": 17,
            "saturated_fat_g": 6,
            "trans_fat_g": 0,
            "cholesterol_mg": 75,
            "sodium_mg": 2260,
            "total_carbohydrate_g": 112,
            "dietary_fiber_g": 4,
            "sugars_g": 21,
            "protein_g": 17
        },
        {
            "name": "Colorado Omelette",
            "serving_size": "1 omelette with pancakes",
            "calories": 1380,
            "total_fat_g": 87,
            "saturated_fat_g": 30,
            "trans_fat_g": 0,
            "cholesterol_mg": 805,
            "sodium_mg": 2600,
            "total_carbohydrate_g": 89,
            "dietary_fiber_g": 6,
            "sugars_g": 19,
            "protein_g": 62
        }
    ],
    
    "Denny's": [
        {
            "name": "Grand Slam",
            "serving_size": "1 meal",
            "calories": 1150,
            "total_fat_g": 67,
            "saturated_fat_g": 23,
            "trans_fat_g": 0,
            "cholesterol_mg": 825,
            "sodium_mg": 2310,
            "total_carbohydrate_g": 91,
            "dietary_fiber_g": 5,
            "sugars_g": 9,
            "protein_g": 45
        },
        {
            "name": "Country Fried Steak & Eggs",
            "serving_size": "1 meal",
            "calories": 1380,
            "total_fat_g": 86,
            "saturated_fat_g": 24,
            "trans_fat_g": 0,
            "cholesterol_mg": 545,
            "sodium_mg": 2730,
            "total_carbohydrate_g": 93,
            "dietary_fiber_g": 5,
            "sugars_g": 5,
            "protein_g": 51
        }
    ],
    
    # ========== SEAFOOD CHAINS ==========
    "Red Lobster": [
        {
            "name": "Ultimate Feast",
            "serving_size": "1 meal",
            "calories": 1210,
            "total_fat_g": 68,
            "saturated_fat_g": 14,
            "trans_fat_g": 0,
            "cholesterol_mg": 460,
            "sodium_mg": 4630,
            "total_carbohydrate_g": 62,
            "dietary_fiber_g": 4,
            "sugars_g": 12,
            "protein_g": 73
        },
        {
            "name": "Cheddar Bay Biscuit",
            "serving_size": "1 biscuit",
            "calories": 160,
            "total_fat_g": 10,
            "saturated_fat_g": 2.5,
            "trans_fat_g": 0,
            "cholesterol_mg": 0,
            "sodium_mg": 380,
            "total_carbohydrate_g": 16,
            "dietary_fiber_g": 0,
            "sugars_g": 1,
            "protein_g": 3
        }
    ]
}