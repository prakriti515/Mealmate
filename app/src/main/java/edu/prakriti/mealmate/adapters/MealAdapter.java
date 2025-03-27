package edu.prakriti.mealmate.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.RecipeDetailActivity;
import edu.prakriti.mealmate.model.Meal;


public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private Context context;
    private List<Meal> mealList;
    private boolean deletePlan;

    public interface OnMealRemoveListener {
        void onMealRemove(Meal meal, int position);
    }

    private OnMealRemoveListener mealRemoveListener;

    public MealAdapter(Context context, List<Meal> mealList, boolean deletePlan, OnMealRemoveListener mealRemoveListener) {
        this.context = context;
        this.mealList = mealList;
        this.deletePlan = deletePlan;
        this.mealRemoveListener=mealRemoveListener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealList.get(position);
        holder.mealName.setText(meal.getMealName());
        //holder.mealImage.setImageResource(meal.getMealImage());
        Glide.with(context)
                .load(meal.getMealImage())
                .placeholder(R.drawable.input_background)
                .error(R.drawable.input_background)
                .into(holder.mealImage);        holder.mealCategory.setText(meal.getMealType());

        switch (meal.getMealType()) {
            case "Breakfast":
                holder.mealCategory.setText("🥪 Breakfast");
                holder.mealCategory.setBackgroundColor(ContextCompat.getColor(context, R.color.gradient_meal));
                holder.mealCategory.setTextColor(ContextCompat.getColor(context, R.color.white));

                break;
            case "Lunch":
                holder.mealCategory.setText("🍝 Lunch");
                holder.mealCategory.setBackgroundColor(ContextCompat.getColor(context, R.color.gradient_meal));
                holder.mealCategory.setTextColor(ContextCompat.getColor(context, R.color.white));

                break;
            case "Dinner":
                holder.mealCategory.setText("🍛 Dinner");
                holder.mealCategory.setBackgroundColor(ContextCompat.getColor(context, R.color.gradient_meal));
                holder.mealCategory.setTextColor(ContextCompat.getColor(context, R.color.white));

                break;
        }

        if (deletePlan) {
            holder.removeMealBtn.setVisibility(View.VISIBLE);
            holder.removeMealBtn.setOnClickListener(v -> {
                if (mealRemoveListener != null) {
                    mealRemoveListener.onMealRemove(meal, position);
                }
            });
        } else {
            holder.removeMealBtn.setVisibility(View.GONE);
        }

        holder.itemMealCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, RecipeDetailActivity.class);

                intent.putExtra("RECIPE", meal.getRecipe());

                // Start the activity
                context.startActivity(intent);
            }
        });


    }

    public void removeMeal(int position) {
        mealList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    public void addMeals(List<Meal> newMeals) {
        this.mealList.addAll(newMeals);
        notifyDataSetChanged();// Add new meals
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        ImageView mealImage, removeMealBtn;
        TextView mealName, mealCategory;
        RelativeLayout itemMealCard;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            mealImage = itemView.findViewById(R.id.mealImage);
            mealName = itemView.findViewById(R.id.mealName);
            mealCategory = itemView.findViewById(R.id.mealCategory);
            removeMealBtn = itemView.findViewById(R.id.removeMealBtn);
            itemMealCard = itemView.findViewById(R.id.itemMealCard);

        }
    }
}
