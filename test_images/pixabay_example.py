
import requests

API_KEY = 'your-pixabay-api-key'
BASE_URL = 'https://pixabay.com/api/'

def search_food_images(query, per_page=10):
    params = {
        'key': API_KEY,
        'q': query,
        'image_type': 'photo',
        'category': 'food',
        'per_page': per_page
    }
    
    response = requests.get(BASE_URL, params=params)
    return response.json()

# Example searches:
# tacos = search_food_images('tacos')
# burgers = search_food_images('hamburger')
# salads = search_food_images('salad healthy')
