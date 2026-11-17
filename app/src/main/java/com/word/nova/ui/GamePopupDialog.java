package com.word.nova.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.word.nova.R;
import com.word.nova.model.GameTheme;

/**
 * Word Nova's dedicated in-game custom popup dialog styled with the active GameTheme.
 * Replaces system AlertDialog boxes across confirmations, game messages, inputs, and level completion.
 */
public class GamePopupDialog extends Dialog {

    public interface OnInputConfirmListener {
        void onConfirm(String inputText);
    }

    private final GameTheme theme;
    private final String title;
    private final String message;
    private final String positiveText;
    private final Runnable positiveAction;
    private final String negativeText;
    private final Runnable negativeAction;
    private final String initialInput;
    private final OnInputConfirmListener inputConfirmListener;
    private final boolean cancelableFlag;

    private GamePopupDialog(
            @NonNull Context context,
            @NonNull GameTheme theme,
            @NonNull String title,
            @Nullable String message,
            @NonNull String positiveText,
            @Nullable Runnable positiveAction,
            @Nullable String negativeText,
            @Nullable Runnable negativeAction,
            @Nullable String initialInput,
            @Nullable OnInputConfirmListener inputConfirmListener,
            boolean cancelableFlag
    ) {
        super(context);
        this.theme = theme;
        this.title = title;
        this.message = message;
        this.positiveText = positiveText;
        this.positiveAction = positiveAction;
        this.negativeText = negativeText;
        this.negativeAction = negativeAction;
        this.initialInput = initialInput;
        this.inputConfirmListener = inputConfirmListener;
        this.cancelableFlag = cancelableFlag;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setContentView(R.layout.dialog_game_popup);
        setCancelable(cancelableFlag);
        setCanceledOnTouchOutside(cancelableFlag);

        View card = findViewById(R.id.popup_card);
        TextView tvTitle = findViewById(R.id.tv_popup_title);
        TextView tvMessage = findViewById(R.id.tv_popup_message);
        EditText etInput = findViewById(R.id.et_popup_input);
        Button btnNegative = findViewById(R.id.btn_popup_negative);
        Button btnPositive = findViewById(R.id.btn_popup_positive);

        ThemeHelper.styleOutlinedCard(card, theme.surfaceColor, theme.primaryButtonColor, 2, 18);
        tvTitle.setText(title);
        tvTitle.setTextColor(theme.primaryButtonColor);

        if (message != null && !message.isEmpty()) {
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(message);
            tvMessage.setTextColor(theme.textPrimaryColor);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        if (inputConfirmListener != null) {
            etInput.setVisibility(View.VISIBLE);
            if (initialInput != null) {
                etInput.setText(initialInput);
                etInput.setSelection(initialInput.length());
            }
            ThemeHelper.styleOutlinedCard(etInput, theme.boardColor, theme.primaryButtonColor, 1, 10);
            etInput.setTextColor(theme.textPrimaryColor);
        } else {
            etInput.setVisibility(View.GONE);
        }

        ThemeHelper.stylePrimaryButton(btnPositive, theme);
        btnPositive.setText(positiveText);
        btnPositive.setOnClickListener(v -> {
            String entered = etInput.getVisibility() == View.VISIBLE ? etInput.getText().toString() : "";
            dismiss();
            if (inputConfirmListener != null) {
                inputConfirmListener.onConfirm(entered);
            } else if (positiveAction != null) {
                positiveAction.run();
            }
        });

        if (negativeText != null && !negativeText.isEmpty()) {
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setText(negativeText);
            ThemeHelper.styleButton(
                    btnNegative,
                    theme.tileColor,
                    ThemeHelper.contrastTextOn(theme.tileColor),
                    12
            );
            btnNegative.setOnClickListener(v -> {
                dismiss();
                if (negativeAction != null) {
                    negativeAction.run();
                }
            });
        } else {
            btnNegative.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            int screenW = getContext().getResources().getDisplayMetrics().widthPixels;
            int maxW = ThemeHelper.dp(getContext(), 420);
            int width = Math.min((int) (screenW * 0.88f), maxW);
            getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public static class Builder {
        private final Context context;
        private final GameTheme theme;
        private String title = "";
        private String message = null;
        private String positiveText = "OK";
        private Runnable positiveAction = null;
        private String negativeText = null;
        private Runnable negativeAction = null;
        private String initialInput = null;
        private OnInputConfirmListener inputConfirmListener = null;
        private boolean cancelable = true;

        public Builder(@NonNull Context context, @NonNull GameTheme theme) {
            this.context = context;
            this.theme = theme;
        }

        public Builder setTitle(@NonNull String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(@Nullable String message) {
            this.message = message;
            return this;
        }

        public Builder setPositiveButton(@NonNull String text, @Nullable Runnable action) {
            this.positiveText = text;
            this.positiveAction = action;
            return this;
        }

        public Builder setNegativeButton(@NonNull String text, @Nullable Runnable action) {
            this.negativeText = text;
            this.negativeAction = action;
            return this;
        }

        public Builder setInput(@Nullable String initialValue, @NonNull OnInputConfirmListener listener) {
            this.initialInput = initialValue;
            this.inputConfirmListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public GamePopupDialog show() {
            GamePopupDialog dialog = new GamePopupDialog(
                    context,
                    theme,
                    title,
                    message,
                    positiveText,
                    positiveAction,
                    negativeText,
                    negativeAction,
                    initialInput,
                    inputConfirmListener,
                    cancelable
            );
            dialog.show();
            return dialog;
        }
    }
}
