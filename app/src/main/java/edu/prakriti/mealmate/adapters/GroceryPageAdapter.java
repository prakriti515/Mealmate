package edu.prakriti.mealmate.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import edu.prakriti.mealmate.fragments.GroceryListFragment;


public class GroceryPageAdapter extends FragmentStateAdapter {

    private final List<String> groceryTypes;
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();
    private int currentPosition = 0;

    public GroceryPageAdapter(@NonNull FragmentActivity fragmentActivity, List<String> groceryTypes) {
        super(fragmentActivity);
        this.groceryTypes = groceryTypes;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = GroceryListFragment.newInstance(groceryTypes.get(position));
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return groceryTypes.size();
    }
    
    /**
     * Returns the currently visible fragment
     */
    public Fragment getCurrentFragment() {
        return fragmentMap.get(currentPosition);
    }
    
    /**
     * Sets the current fragment position
     */
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
    }
}
