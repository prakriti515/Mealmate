package edu.prakriti.mealmate;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import edu.prakriti.mealmate.R;

public class CustomProgressDialog {
    private Dialog dialog;
    private Context context;
    private CircularProgressIndicator progressIndicator;

    public CustomProgressDialog(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.custom_progress_dialog, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        // Set transparent background for dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set dialog window attributes
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        dialog.getWindow().setAttributes(params);

        // Find Progress Indicator and Set Indeterminate Mode
        progressIndicator = view.findViewById(R.id.progressIndicator);
        progressIndicator.setIndeterminate(true);
    }

    public void show() {
        if (context != null && dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    public boolean isRefreshing() {
        return dialog != null && dialog.isShowing();
    }
}
