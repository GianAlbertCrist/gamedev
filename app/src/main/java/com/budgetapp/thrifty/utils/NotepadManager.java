package com.budgetapp.thrifty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class NotepadManager {
    private static final String PREFS_NAME = "notepad_prefs";
    private static final String KEY_CONTENT = "notepad_content";
    private final SharedPreferences prefs;

    public NotepadManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String loadNote() {
        return prefs.getString(KEY_CONTENT, "");
    }

    public void saveNote(String content) {
        prefs.edit().putString(KEY_CONTENT, content).apply();
    }

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
