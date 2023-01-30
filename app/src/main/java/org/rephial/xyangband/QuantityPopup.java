package org.rephial.xyangband;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class QuantityPopup extends PopupWindow
        implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        NumberPicker.OnValueChangeListener {

    public GameActivity context;

    public SeekBar valueBar = null;

    public NumberPicker valuePicker = null;

    public boolean locked = false;

    public ViewGroup root = null;

    public QuantityPopup(GameActivity p_context, String message,
                         int maxValue, int initialValue) {
        super(p_context);

        setOutsideTouchable(true);
        setFocusable(false);

        context = p_context;

        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        root = (ViewGroup) context.getLayoutInflater().inflate
                (R.layout.quantity_layout, null);

        setContentView(root);

        valueBar = root.findViewById(R.id.valueBar);

        valuePicker = root.findViewById(R.id.valuePicker);

        valuePicker.setMinValue(0);

        configure(message, maxValue, initialValue);

        valueBar.setOnSeekBarChangeListener(this);

        valuePicker.setOnValueChangedListener(this);

        Button button = root.findViewById(R.id.allButton);
        button.setOnClickListener(this);

        button = root.findViewById(R.id.oneButton);
        button.setOnClickListener(this);

        button = root.findViewById(R.id.tenButton);
        button.setOnClickListener(this);
    }

    public void configure(String message, int maxValue, int initialValue)
    {
        locked = true;

        valuePicker.setMaxValue(maxValue);
        valuePicker.setValue(initialValue);

        Button button = root.findViewById(R.id.allButton);
        button.setText("" + maxValue);

        button = root.findViewById(R.id.tenButton);
        button.setVisibility(maxValue <= 10 ? View.GONE: View.VISIBLE);

        updateValueBar(valuePicker.getValue());

        locked = false;
    }

    @Override
    public void onClick(View v) {

        if (locked) return;

        Button button = (Button)v;

        locked = true;

        if (button.getId() == R.id.allButton) {
            valuePicker.setValue(valuePicker.getMaxValue());
        }

        if (button.getId() == R.id.oneButton) {
            valuePicker.setValue(1);
        }

        if (button.getId() == R.id.tenButton) {
            valuePicker.setValue(10);
        }

        updateValueBar(valuePicker.getValue());

        sendValue(true);

        locked = false;

        dismiss();
    }

    public void sendValue(boolean finish)
    {
        StateManager state = context.getStateManager();

        String str = Integer.toString(valuePicker.getValue());
        List<Integer> list = InputUtils.parseCodeKeys(str);

        // For some variants, go to the right
        int keyRight = state.getKeyRightForQuantity();
        if (keyRight > 0) {
            for (int i = 0; i < 15; i++) {
                state.addKey(keyRight);
            }
        }

        // Erase all past input
        for (int i = 0; i < 15; i++) {
            state.addKey(state.getKeyBackspace());
        }

        for (Integer keycode : list) {
            if (keycode > 0) {
                state.addKey(keycode);
            }
        }

        if (finish) state.addKey(state.getKeyEnter());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar,
                                  int progress, boolean fromUser)
    {
        if (locked) return;

        locked = true;

        int range = valuePicker.getMaxValue() - valuePicker.getMinValue();
        int value = valuePicker.getMinValue() + progress * range / 100;
        valuePicker.setValue(value);

        sendValue(false);

        locked = false;
    }

    public void updateValueBar(int newVal)
    {
        int range = valuePicker.getMaxValue() - valuePicker.getMinValue();
        int progress = 100 * newVal / range;
        valueBar.setProgress(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void dismiss()
    {
        super.dismiss();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal)
    {
        if (locked) return;

        locked = true;

        updateValueBar(newVal);

        sendValue(false);

        locked = false;
    }
}
