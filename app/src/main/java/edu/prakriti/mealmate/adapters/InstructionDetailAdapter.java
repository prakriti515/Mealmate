package edu.prakriti.mealmate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import edu.prakriti.mealmate.R;

public class InstructionDetailAdapter extends RecyclerView.Adapter<InstructionDetailAdapter.InstructionViewHolder> {

    private static final String TAG = "InstructionAdapter";
    private final Context context;
    private final List<Map<String, Object>> instructions;

    public InstructionDetailAdapter(Context context, List<Map<String, Object>> instructions) {
        this.context = context;
        this.instructions = instructions;
        
        // Validate instructions during adapter creation
        validateInstructions();
    }
    
    /**
     * Validates the instructions list to ensure all items are properly formatted
     */
    private void validateInstructions() {
        if (instructions == null) {
            Log.e(TAG, "Instructions list is null");
            return;
        }
        
        Log.d(TAG, "Validating " + instructions.size() + " instructions");
        
        for (int i = 0; i < instructions.size(); i++) {
            Map<String, Object> step = instructions.get(i);
            if (step == null) {
                Log.e(TAG, "Instruction at position " + i + " is null");
                continue;
            }
            
            // Check for required fields
            if (!step.containsKey("stepNumber")) {
                Log.w(TAG, "Instruction at position " + i + " is missing stepNumber");
            } else if (step.get("stepNumber") == null) {
                Log.w(TAG, "Instruction at position " + i + " has null stepNumber");
            }
            
            if (!step.containsKey("instruction")) {
                Log.w(TAG, "Instruction at position " + i + " is missing instruction text");
            } else if (step.get("instruction") == null) {
                Log.w(TAG, "Instruction at position " + i + " has null instruction text");
            }
        }
    }

    @NonNull
    @Override
    public InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_instruction_step, parent, false);
        
        // Ensure the view fills the parent ViewPager2
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            view.setLayoutParams(layoutParams);
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        
        Log.d(TAG, "Created ViewHolder with match_parent dimensions");
        return new InstructionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstructionViewHolder holder, int position) {
        try {
            if (position >= instructions.size()) {
                Log.e(TAG, "Position " + position + " is out of bounds (" + instructions.size() + ")");
                bindFallbackData(holder, position);
                return;
            }
            
            Map<String, Object> step = instructions.get(position);
            if (step == null) {
                Log.e(TAG, "Instruction at position " + position + " is null");
                bindFallbackData(holder, position);
                return;
            }
            
            // Set step number with improved formatting and error handling
            int stepNumber;
            Object stepNumberObj = step.get("stepNumber");
            
            if (stepNumberObj instanceof Long) {
                stepNumber = ((Long) stepNumberObj).intValue();
                Log.d(TAG, "Step number is Long: " + stepNumber);
            } else if (stepNumberObj instanceof Integer) {
                stepNumber = (Integer) stepNumberObj;
                Log.d(TAG, "Step number is Integer: " + stepNumber);
            } else if (stepNumberObj instanceof String) {
                try {
                    stepNumber = Integer.parseInt((String) stepNumberObj);
                    Log.d(TAG, "Step number is String, parsed to: " + stepNumber);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse step number from String: " + stepNumberObj);
                    stepNumber = position + 1; // Fallback to position + 1
                }
            } else {
                // If stepNumber is not available, use position + 1
                Log.w(TAG, "Step number is missing or unknown type: " + 
                      (stepNumberObj != null ? stepNumberObj.getClass().getName() : "null"));
                stepNumber = position + 1;
            }
            
            holder.stepNumber.setText("Step " + stepNumber + " of " + instructions.size());
            
            // Set instruction text with error handling
            String instruction = "";
            Object instructionObj = step.get("instruction");
            
            if (instructionObj != null) {
                instruction = instructionObj.toString();
                Log.d(TAG, "Instruction text found: " + instruction);
            } else {
                // If instruction is missing, provide a default message
                Log.w(TAG, "Instruction text is null or missing for step " + stepNumber);
                instruction = "No details available for this step";
            }
            
            holder.instructionText.setText(instruction);
            
            // Add visual indication of current step
            holder.itemView.setElevation(8f); // Add elevation for a card-like effect
            
            // Add progress indicator
            float progress = (float) position / (float) Math.max(instructions.size() - 1, 1);
            holder.progressIndicator.setProgress((int) (progress * 100));
            
            // Set current step count for reference
            holder.stepCount.setText((position + 1) + "/" + instructions.size());
        } catch (Exception e) {
            // Handle any unexpected exceptions to prevent crashes
            Log.e(TAG, "Error binding instruction at position " + position, e);
            bindFallbackData(holder, position);
        }
    }
    
    /**
     * Binds fallback data when there's an error with the actual instruction data
     */
    private void bindFallbackData(InstructionViewHolder holder, int position) {
        holder.stepNumber.setText("Step " + (position + 1));
        holder.instructionText.setText("Error loading step details - please try again later");
        holder.stepCount.setText((position + 1) + "/" + Math.max(instructions.size(), 1));
        holder.progressIndicator.setProgress(0);
    }

    @Override
    public int getItemCount() {
        return instructions != null ? instructions.size() : 0;
    }

    public static class InstructionViewHolder extends RecyclerView.ViewHolder {
        TextView stepNumber, instructionText, stepCount;
        ImageView stepImage;
        ProgressBar progressIndicator;

        public InstructionViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumber = itemView.findViewById(R.id.stepNumber);
            instructionText = itemView.findViewById(R.id.instructionText);
            stepCount = itemView.findViewById(R.id.stepCount);
            progressIndicator = itemView.findViewById(R.id.progressIndicator);
        }
    }
}