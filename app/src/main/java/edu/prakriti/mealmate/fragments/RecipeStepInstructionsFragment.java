package edu.prakriti.mealmate.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.adapters.InstructionAdapter;
import edu.prakriti.mealmate.model.InstructionStep;

public class RecipeStepInstructionsFragment extends Fragment implements InstructionAdapter.OnDeleteClickListener {
    
    private static final String TAG = "InstructionsFragment";
    
    private RecyclerView rvInstructions;
    private TextInputEditText etInstructionDesc, etInstructionTime;
    private Button btnAddInstruction;
    private CheckBox cbAddToFavorites;
    
    private List<InstructionStep> instructionsList = new ArrayList<>();
    private InstructionAdapter adapter;
    
    private InstructionsListener listener;
    
    public interface InstructionsListener {
        void onInstructionsProvided(List<InstructionStep> instructions, boolean addToFavorites);
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof InstructionsListener) {
            listener = (InstructionsListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement InstructionsListener");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_step_instructions, container, false);
        
        Log.d(TAG, "Creating instructions step view");
        
        // Initialize views
        rvInstructions = view.findViewById(R.id.rv_instructions);
        etInstructionDesc = view.findViewById(R.id.et_instruction_desc);
        etInstructionTime = view.findViewById(R.id.et_instruction_time);
        btnAddInstruction = view.findViewById(R.id.btn_add_instruction);
        cbAddToFavorites = view.findViewById(R.id.cb_add_to_favorites);
        
        // Set up RecyclerView
        rvInstructions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InstructionAdapter(instructionsList, this);
        rvInstructions.setAdapter(adapter);
        
        // Set up add instruction button
        btnAddInstruction.setOnClickListener(v -> addInstruction());
        
        return view;
    }
    
    private void addInstruction() {
        try {
            String description = etInstructionDesc.getText() != null ? etInstructionDesc.getText().toString().trim() : "";
            
            if (TextUtils.isEmpty(description)) {
                Log.w(TAG, "Empty instruction description - showing error message");
                showMessage("Please enter instruction description");
                return;
            }
            
            Log.d(TAG, "Adding new instruction: " + description);
            
            // Create new instruction and add to list
            int stepNumber = instructionsList.size() + 1;
            InstructionStep step = new InstructionStep(stepNumber, description);
            instructionsList.add(step);
            adapter.notifyItemInserted(instructionsList.size() - 1);
            
            // Log the current instructions list
            Log.d(TAG, "Instructions list now has " + instructionsList.size() + " steps");
            for (int i = 0; i < instructionsList.size(); i++) {
                InstructionStep currentStep = instructionsList.get(i);
                Log.d(TAG, "Step " + (i+1) + ": " + currentStep.getInstruction());
            }
            
            // Clear input fields
            etInstructionDesc.setText("");
            etInstructionTime.setText("");
            etInstructionDesc.requestFocus();
            
            // Show confirmation message
            showMessage("Instruction added successfully");
            
            // Update the activity with the new instructions list
            updateActivity();
        } catch (Exception e) {
            Log.e(TAG, "Error adding instruction", e);
            showMessage("Error adding instruction: " + e.getMessage());
        }
    }
    
    @Override
    public void onDeleteClick(int position) {
        if (position < 0 || position >= instructionsList.size()) {
            Log.e(TAG, "Invalid position for deletion: " + position);
            showMessage("Error deleting instruction");
            return;
        }
        
        try {
            Log.d(TAG, "Deleting instruction at position " + position + ": " + 
                  instructionsList.get(position).getInstruction());
            
            // Remove the instruction
            instructionsList.remove(position);
            
            // Update step numbers for remaining instructions
            for (int i = 0; i < instructionsList.size(); i++) {
                instructionsList.get(i).setStepNumber(i + 1);
            }
            
            // Update the adapter
            adapter.notifyDataSetChanged();
            
            // Log the updated list
            Log.d(TAG, "After deletion, instructions list has " + instructionsList.size() + " steps");
            for (int i = 0; i < instructionsList.size(); i++) {
                InstructionStep step = instructionsList.get(i);
                Log.d(TAG, "Step " + (i+1) + ": " + step.getInstruction());
            }
            
            // Show confirmation message
            showMessage("Instruction removed");
            
            // Update the activity with the modified instructions list
            updateActivity();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting instruction", e);
            showMessage("Error deleting instruction: " + e.getMessage());
        }
    }
    
    /**
     * Updates the parent activity with the current instructions list
     */
    private void updateActivity() {
        if (listener != null) {
            Log.d(TAG, "Updating activity with " + instructionsList.size() + " instructions");
            listener.onInstructionsProvided(instructionsList, cbAddToFavorites.isChecked());
        } else {
            Log.w(TAG, "Cannot update activity - listener is null");
        }
    }
    
    /**
     * Displays a message to the user
     */
    private void showMessage(String message) {
        // Try to show a Snackbar first, fall back to Toast if view is not available
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called - sending " + instructionsList.size() + " instructions to activity");
        
        // Log the instructions being provided
        for (int i = 0; i < instructionsList.size(); i++) {
            InstructionStep step = instructionsList.get(i);
            Log.d(TAG, "Step " + (i+1) + ": " + step.getInstruction());
        }
        
        // Pass data to activity when fragment is paused
        if (listener != null) {
            listener.onInstructionsProvided(instructionsList, cbAddToFavorites.isChecked());
        } else {
            Log.w(TAG, "Cannot provide instructions to activity - listener is null");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - current instruction count: " + instructionsList.size());
    }
} 