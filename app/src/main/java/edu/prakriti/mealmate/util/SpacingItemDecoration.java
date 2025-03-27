package edu.prakriti.mealmate.util;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Item decoration for adding equal spacing between RecyclerView items
 */
public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
    
    private final int spacing;
    
    public SpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }
    
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        
        // Add top spacing for all items except the first one
        if (parent.getChildAdapterPosition(view) > 0) {
            outRect.top = spacing;
        }
        
        // Add bottom spacing for all items
        outRect.bottom = spacing;
        outRect.left = spacing;
        outRect.right = spacing;
    }
} 