package edu.prakriti.mealmate.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import edu.prakriti.mealmate.R;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final ColorDrawable background;
    private final Paint clearPaint;
    private final int deleteColor;
    private final int checkColor;
    private final Drawable deleteIcon;
    private final Drawable checkIcon;

    public SwipeToDeleteCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        background = new ColorDrawable();
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        deleteColor = Color.parseColor("#f44336"); // Red
        checkColor = Color.parseColor("#4CAF50"); // Green
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        checkIcon = ContextCompat.getDrawable(context, R.drawable.ic_check);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // Do nothing, we only care about swiping
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // This method will be implemented by the class that uses this callback
        // through the onSwiped method of ItemTouchHelper.SimpleCallback
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = dX == 0f && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        // Draw red background for left swipe (delete)
        if (dX < 0) {
            background.setColor(deleteColor);
            background.setBounds(
                    itemView.getRight() + (int) dX,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom()
            );
            background.draw(c);

            // Calculate position for delete icon
            int iconMargin = (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
            int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.draw(c);
        } 
        // Draw green background for right swipe (purchase)
        else if (dX > 0) {
            background.setColor(checkColor);
            background.setBounds(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getLeft() + (int) dX,
                    itemView.getBottom()
            );
            background.draw(c);

            // Calculate position for check icon
            int iconMargin = (itemHeight - checkIcon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemHeight - checkIcon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + checkIcon.getIntrinsicHeight();
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = itemView.getLeft() + iconMargin + checkIcon.getIntrinsicWidth();
            
            checkIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            checkIcon.draw(c);
        }
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if (c != null) {
            c.drawRect(left, top, right, bottom, clearPaint);
        }
    }
} 