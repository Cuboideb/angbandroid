package org.rephial.xyangband;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;

import java.util.List;

public class FastKeysPopupV2 extends PopupWindow
        implements View.OnClickListener {

    public GameActivity context;

    public ViewGroup root = null;

    public GridLayout grid = null;

    public GridLayout dpad = null;

    public String keys = "";

    public static boolean dpadVisible = false;

    public FastKeysPopupV2(GameActivity p_context, String p_keys) {
        super(p_context);

        setOutsideTouchable(true);
        setFocusable(false);

        context = p_context;

        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        root = (ViewGroup) context.getLayoutInflater().inflate
                (R.layout.fast_keys_layout_v2, null);

        setContentView(root);

        grid = root.findViewById(R.id.grid);

        dpad = root.findViewById(R.id.dpad);
        dpad.setVisibility(dpadVisible ? View.VISIBLE: View.GONE);

        for (int i = 0; i < dpad.getChildCount(); i++) {
            Button btn = (Button)dpad.getChildAt(i);
            btn.setOnClickListener(this);
            btn.setText(InputUtils.arrowFromIndex(i));
        }

        Button btnDPad = root.findViewById(R.id.dpad_btn);
        btnDPad.setText("DPad");
        ButtonRibbon.resizeButtonAux(btnDPad, 0.6f);
        btnDPad.setOnClickListener(this);

        for (int i = 0; i < grid.getChildCount(); i++) {
            Button btn = (Button)grid.getChildAt(i);
            btn.setOnClickListener(this);
        }

        configure(p_keys);
    }

    public void configureButtons()
    {
        int numButtons = grid.getChildCount();

        while (numButtons < keys.length()) {
            Button btn = (Button)context.getLayoutInflater().inflate
                    (R.layout.fast_key_button, null);
            grid.addView(btn);
            btn.setOnClickListener(this);
            btn.requestLayout();
            ++numButtons;
        }

        for (int i = 0; i < numButtons; i++) {
            String txt = "";
            if (i < keys.length()) {
                txt = InputUtils.printableChar(keys.charAt(i));
            }
            Button btn = (Button)grid.getChildAt(i);
            btn.setText(txt);
            int visibility = txt.length() > 0 ? View.VISIBLE: View.GONE;
            if (visibility != btn.getVisibility()) {
                btn.setVisibility(visibility);
                btn.requestLayout();
            }
        }
    }

    public void run()
    {
        String position = Preferences.getFastKeysPopupPosition();

        if (position.equals("Hidden")) return;

        int gravity = Gravity.TOP;
        int pad = (int)context.toDips(5);
        int y = pad;
        int x = pad;

        root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        TermView term = context.term;

        if (position.contains("Bottom")) {
            y = term.getHeight()
                - root.getMeasuredHeight()
                - term.getVerticalGap()
                - pad;
        }

        if (position.contains("Left")) {
            gravity |= Gravity.LEFT;
        }

        if (position.contains("Right")) {
            gravity |= Gravity.RIGHT;
        }

        //log("term h: " + term.getHeight());
        //log("root h: " + quantPopup.root.getMeasuredHeight());
        //log("vgap: " + term.getVerticalGap());
        //log("y: " + y);
        if (!this.isShowing()) {
            this.showAtLocation(term, gravity, x, y);
        }
        else {
            this.update(x, y, -1, -1);
        }
    }

    public void configure(String p_keys)
    {
        p_keys = p_keys.replace("${yes_no}", "ny");

        if (!p_keys.equals(keys)) {
            keys = p_keys;
            configureButtons();
        }

        final ScrollView sv = root.findViewById(R.id.scroll_view);
        /*
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.scrollTo(0, 0);
            }
        });
        */
        sv.scrollTo(0, 0);
    }

    @Override
    public void onClick(View v) {

        String str = ((Button)v).getText().toString();

        int idx = dpad.indexOfChild(v);

        if (str.equals("DPad")) {
            dpadVisible = !dpadVisible;
            dpad.setVisibility(dpadVisible ? View.VISIBLE: View.GONE);
            dpad.requestLayout();
            // Change location
            //run();
        }
        else if (idx != -1) {
            int key = TermView.directionKeyFromIndex(idx);
            context.state.addDirectionKey(key);
        }
        else if (str.length() > 0) {
            sendValue(str);
        }
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

    @Override
    public void dismiss()
    {
        // Do nothing
    }

    // The "real" dismiss
    public void close()
    {
        super.dismiss();
    }
}
