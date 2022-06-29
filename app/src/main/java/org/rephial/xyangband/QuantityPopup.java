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

    public int selected = 0;

    public boolean locked = false;

    public TextView msgView = null;

    public QuantityPopup(GameActivity p_context, String message,
                         int maxValue, int initialValue) {
        super(p_context);

        context = p_context;

        setFocusable(true);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        ViewGroup content = (ViewGroup) context.getLayoutInflater().inflate
                (R.layout.quantity_layout, null);

        setContentView(content);

        msgView = content.findViewById(R.id.message);
        //msgView.setTextSize(msgView.getTextSize()*1.5f);
        msgView.setText(message);

        valueBar = content.findViewById(R.id.valueBar);

        valuePicker = content.findViewById(R.id.valuePicker);

        valuePicker.setMinValue(0);
        valuePicker.setMaxValue(maxValue);
        valuePicker.setValue(initialValue);

        updateValueBar(valuePicker.getValue());

        valueBar.setOnSeekBarChangeListener(this);

        valuePicker.setOnValueChangedListener(this);

        Button button = content.findViewById(R.id.okButton);
        button.setOnClickListener(this);

        button = content.findViewById(R.id.allButton);
        button.setOnClickListener(this);

        button = content.findViewById(R.id.oneButton);
        button.setOnClickListener(this);
    }

    public void configure(String message, int maxValue, int initialValue)
    {
        locked = true;

        selected = 0;

        valuePicker.setMaxValue(maxValue);
        valuePicker.setValue(initialValue);

        updateValueBar(valuePicker.getValue());

        locked = false;

        msgView.setText(message);

        this.getContentView().requestLayout();
    }

    @Override
    public void onClick(View v) {
        Button button = (Button)v;
        if (button.getText().equals("All")) {
            valuePicker.setValue(valuePicker.getMaxValue());
            //updateValueBar(valuePicker.getValue());
        }

        if (button.getText().equals("One")) {
            valuePicker.setValue(1);
            //updateValueBar(valuePicker.getValue());
        }

        selected = valuePicker.getValue();

        this.dismiss();
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
        String str = Integer.toString(selected);
        List<Integer> list = InputUtils.parseCodeKeys(str);
        StateManager state = context.getStateManager();
        for (Integer keycode: list) {
            if (keycode > 0) {
                state.addKey(keycode);
            }
        }
        state.addKey(state.getKeyEnter());

        super.dismiss();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal)
    {
        if (locked) return;

        locked = true;

        updateValueBar(newVal);

        locked = false;
    }
}
