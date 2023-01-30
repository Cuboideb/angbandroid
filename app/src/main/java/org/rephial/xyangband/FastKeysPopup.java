package org.rephial.xyangband;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import java.util.List;

public class FastKeysPopup extends PopupWindow
        implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {

    public GameActivity context;

    public SeekBar valueBar = null;

    public boolean locked = false;

    public ViewGroup root = null;

    public Button[] buttons = null;

    public int maxButtons = 6;

    public String keys = "";

    // We include a close button to cancel the popup
    // while targeting is active
    public static boolean visible = true;

    public FastKeysPopup(GameActivity p_context, String p_keys) {
        super(p_context);

        setOutsideTouchable(true);
        setFocusable(false);

        context = p_context;

        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        root = (ViewGroup) context.getLayoutInflater().inflate
                (R.layout.fast_keys_layout, null);

        setContentView(root);

        valueBar = root.findViewById(R.id.valueBar);

        valueBar.setOnSeekBarChangeListener(this);

        buttons = new Button[maxButtons];

        for (int i = 0; i < maxButtons; i++) {
            int id = context.getResources().getIdentifier("btn"+(i+1),
                        "id",
                        context.getPackageName());
            Button button = root.findViewById(id);
            button.setOnClickListener(this);
            buttons[i] = button;
        }

        Button button2 = root.findViewById(R.id.btn_hide);
        button2.setOnClickListener(this);

        configure(p_keys);
    }

    public void configureButtons()
    {
        int idx = getIndex();
        for (int i = 0, j = idx; i < maxButtons; i++, j++) {
            String txt = "";
            if (j < keys.length()) {
                txt = InputUtils.printableChar(keys.charAt(j));
            }
            buttons[i].setText(txt);
        }
    }

    public void run(TermView term)
    {
        int gravity = Gravity.RIGHT | Gravity.TOP;
        int pad = (int)context.toDips(20);
        root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int y = term.getHeight()
                - root.getMeasuredHeight()
                - term.getVerticalGap()
                - pad;
        //log("term h: " + term.getHeight());
        //log("root h: " + quantPopup.root.getMeasuredHeight());
        //log("vgap: " + term.getVerticalGap());
        //log("y: " + y);
        this.showAtLocation(term, gravity, 0, y);
    }

    public void configure(String p_keys)
    {
        if (locked) return;

        locked = true;

        p_keys = p_keys.replace("[^yes_no$]", "ny");

        if (!p_keys.equals(keys)) {
            keys = p_keys;
            valueBar.setProgress(0);
            configureButtons();
        }

        locked = false;
    }

    public static boolean isVisible()
    {
        return visible;
    }

    public static void unhide()
    {
        visible = true;
    }

    @Override
    public void onClick(View v) {

        if (locked) return;

        locked = true;

        String str = ((Button)v).getText().toString();

        if (str.equals("hide")) {
            visible = false;
            close();
        }
        else if (str.length() > 0) {
            sendValue(str);
        }

        locked = false;
    }

    public void sendValue(String str)
    {
        StateManager state = context.getStateManager();
        List<Integer> list = InputUtils.parseCodeKeys(str);

        for (Integer keycode : list) {
            if (keycode > 0) {
                state.addKey(keycode);
            }
        }
    }

    public int getIndex()
    {
        int progress = valueBar.getProgress();
        int maxValue = keys.length() - 1;
        int minValue = 0;
        int range = maxValue - minValue;
        int value = minValue + progress * range / 100;
        return value;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar,
                                  int progress, boolean fromUser)
    {
        if (locked) return;

        locked = true;

        configureButtons();

        locked = false;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void dismiss()
    {
        // Do nothing
    }

    public void close()
    {
        super.dismiss();
    }
}
