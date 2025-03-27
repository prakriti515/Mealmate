package edu.prakriti.mealmate.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IngredientConstants {

    // Category List
    public static final List<String> CATEGORY_LIST = Arrays.asList(
            "🛢️ Oils",
            "🍎 Fruits",
            "🥬 Vegetables",
            "🌾 Grains & Legumes",
            "🍗 Proteins",
            "🧀 Dairy",
            "🌿 Herbs & Spices",
            "💧 Essentials",
            "🥫 Condiments"
    );

    private static final Map<String, List<String>> INGREDIENT_MAP;

    static {
        INGREDIENT_MAP = new HashMap<>();
        INGREDIENT_MAP.put("🛢️ Oils", Arrays.asList("🫒 Olive Oil", "🧈 Butter", "🥥 Coconut Oil", "🥄 Ghee", "🛢️ Vegetable Oil"));
        INGREDIENT_MAP.put("🍎 Fruits", Arrays.asList("🍏 Apple", "🍌 Banana", "🍊 Orange", "🍓 Strawberries", "🍇 Grapes", "🥭 Mango", "🍍 Pineapple", "🍋 Lemon/Lime"));
        INGREDIENT_MAP.put("🥬 Vegetables", Arrays.asList("🥕 Carrot", "🥦 Broccoli", "🌿 Spinach", "🍅 Tomato", "🧅 Onion", "🧄 Garlic", "🌶 Bell Pepper", "🥒 Zucchini", "🥬 Cabbage", "🥬 Kale", "🥗 Lettuce", "🥔 Cauliflower"));
        INGREDIENT_MAP.put("🌾 Grains & Legumes", Arrays.asList("🍚 Rice", "🍞 Bread", "🌾 Wheat", "🥣 Oats", "🌰 Lentils", "🫘 Chickpeas", "🌽 Corn", "🥜 Peanuts"));
        INGREDIENT_MAP.put("🍗 Proteins", Arrays.asList("🍗 Chicken", "🥩 Beef", "🐖 Pork", "🐟 Fish", "🍳 Eggs", "🌱 Tofu", "🫘 Beans"));
        INGREDIENT_MAP.put("🧀 Dairy", Arrays.asList("🥛 Milk", "🍦 Yogurt", "🧀 Cheese", "🧈 Butter", "🥥 Coconut Milk", "🌱 Soy/Oat Milk"));
        INGREDIENT_MAP.put("🌿 Herbs & Spices", Arrays.asList("🫚 Cinnamon", "🌿 Basil", "🌿 Oregano", "🌿 Thyme", "🌿 Rosemary", "🧂 Salt", "🌶 Chili Powder", "🟠 Turmeric", "🫚 Ginger", "🟤 Cumin", "🟡 Cardomom"));
        INGREDIENT_MAP.put("💧 Essentials", Arrays.asList("💧 Water", "🧊 Ice", "🧂 Salt", "🍯 Honey", "🍚 Sugar"));
        INGREDIENT_MAP.put("🥫 Condiments", Arrays.asList("🥫 Ketchup", "🥫 Mustard", "🥫 Mayo", "🥫 Soy Sauce", "🥫 Hot Sauce"));
    }

    public static Map<String, List<String>> getIngredientMap() {
        return new HashMap<>(INGREDIENT_MAP);
    }
}
