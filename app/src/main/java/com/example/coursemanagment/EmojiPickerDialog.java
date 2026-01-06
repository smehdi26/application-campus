package com.example.coursemanagment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import androidx.annotation.NonNull;

public class EmojiPickerDialog extends Dialog {
    private EmojiPickerListener listener;
    private String[] emojis = {
        "👍", "❤️", "😂", "😮", "😢", "🔥", "👏", "💯",
        "🤔", "😍", "🎉", "🚀", "💪", "✨", "🙏", "😊",
        "😡", "😱", "🤗", "😎", "🙈", "💔", "🎊", "👌"
    };

    public EmojiPickerDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create custom layout with header
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(Color.WHITE);

        // Header
        TextView headerText = new TextView(getContext());
        headerText.setText("Pick an emoji");
        headerText.setTextSize(18);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setTextColor(Color.parseColor("#1A1A1A"));
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 20);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        mainLayout.addView(headerText, headerParams);

        // Grid Layout
        GridLayout gridLayout = new GridLayout(getContext());
        gridLayout.setColumnCount(6);
        gridLayout.setRowCount(4);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        gridParams.setMargins(0, 0, 0, 0);
        
        for (String emoji : emojis) {
            Button btn = new Button(getContext());
            btn.setText(emoji);
            btn.setTextSize(24);
            btn.setPadding(12, 12, 12, 12);
            btn.setBackgroundColor(Color.TRANSPARENT);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            btn.setLayoutParams(params);
            
            btn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmojiSelected(emoji);
                }
                dismiss();
            });
            
            gridLayout.addView(btn);
        }
        
        mainLayout.addView(gridLayout, gridParams);
        setContentView(mainLayout);
        
        // Set dialog size
        getWindow().setLayout(
                (int)(getContext().getResources().getDisplayMetrics().widthPixels * 0.85),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    public void setListener(EmojiPickerListener listener) {
        this.listener = listener;
    }

    public interface EmojiPickerListener {
        void onEmojiSelected(String emoji);
    }
}
