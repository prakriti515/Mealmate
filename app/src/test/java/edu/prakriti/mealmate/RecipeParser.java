package edu.prakriti.mealmate;


import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.prakriti.mealmate.model.Recipe;

public class RecipeParser {

    public static Recipe parseRecipe(DocumentSnapshot document) {
        Recipe recipe = new Recipe();
        recipe.setRecipeName(document.getString("recipeName"));
        recipe.setCookTime(document.getString("cookTime"));
        recipe.setPhotoUrl(document.getString("photoUrl"));
        recipe.setTimestamp(document.getLong("timestamp"));

        Map<String, List<String>> ingredientsMap = new HashMap<>();
        Map<String, Object> firestoreIngredients = (Map<String, Object>) document.get("ingredients");
        if (firestoreIngredients != null) {
            for (Map.Entry<String, Object> entry : firestoreIngredients.entrySet()) {
                ingredientsMap.put(entry.getKey(), (List<String>) entry.getValue());
            }
        }
        recipe.setIngredients(ingredientsMap);

        List<Map<String, Object>> instructionsList = (List<Map<String, Object>>) document.get("instructions");
        if (instructionsList != null) {
            recipe.setInstructions(instructionsList);
        }

        return recipe;
    }
}
