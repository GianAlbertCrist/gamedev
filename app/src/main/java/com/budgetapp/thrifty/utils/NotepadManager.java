package com.budgetapp.thrifty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Manages the notepad functionality, including saving and loading notes
 * using SharedPreferences for local storage.
 */
public class NotepadManager {
    private static final String PREFS_NAME = "notepad_prefs";
    private static final String KEY_CONTENT = "notepad_content";
    private final SharedPreferences prefs;

    public NotepadManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Loads the saved note content
     * @return The saved note content or empty string if none exists
     */
    public String loadNote() {
        return prefs.getString(KEY_CONTENT, "");
    }

    /**
     * Saves the note content to SharedPreferences
     * @param content The content to save
     */
    public void saveNote(String content) {
        prefs.edit().putString(KEY_CONTENT, content).apply();
    }

    /**
     * Sets up auto-save functionality for the provided EditText
     * @param editText The EditText to set up auto-save for
     */
    public void setupAutoSave(final EditText editText) {
        // Load existing content
        editText.setText(loadNote());

        // Set up TextWatcher for auto-save
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Save after each text change
                saveNote(s.toString());
            }
        });
    }
}
