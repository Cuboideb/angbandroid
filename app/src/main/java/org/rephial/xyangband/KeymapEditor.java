package org.rephial.xyangband;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.Iterator;

public class KeymapEditor extends PopupWindow
        implements View.OnClickListener {

    GameActivity ctxt;
    View parent;
    View mainView;
    EditText actionEdit;
    ButtonRow row1;
    ButtonRow row2;
    ButtonRow curRow;

    int max = 0;

    class ButtonRow {
        ArrayList<String> actionLst = null;
        ViewGroup container = null;
        int curIdx = -1;
        int rowNumber = 0;

        public ButtonRow(String actions)
        {
            actionLst = new ArrayList<>();

            for (String s: actions.split("###")) {
                if (s.length() > 0) actionLst.add(s);
            }
        }

        public String getPrefix()
        {
            return "row:" + rowNumber + ":";
        }

        public String formatKeymaps()
        {
            String keymaps = "";
            String sep = "";
            for (String s : actionLst) {
                keymaps += (sep + s);
                sep = "###";
            }
            return keymaps;
        }
    }

    public static String[] getUserKeymaps()
    {
        String actionsRow1 = "";
        String actionsRow2 = "";
        //String raw = Preferences.getUserKeymaps();
        String raw = Preferences.getActiveProfile().getKeymaps();
        String[] parts = raw.split("#rowsep#");
        if (parts.length > 0) {
            actionsRow1 = parts[0];
        }
        if (parts.length > 1) {
            actionsRow2 = parts[1];
        }
        return new String[]{actionsRow1, actionsRow2};
    }

    public KeymapEditor(GameActivity p_ctxt, View p_parent) {
        super(p_ctxt);
        this.setFocusable(true);
        this.setWidth(LayoutParams.MATCH_PARENT);
        this.setHeight(LayoutParams.WRAP_CONTENT);

        ctxt = p_ctxt;
        parent = p_parent;

        String s = ctxt.getResources().getString(R.string.def_keymap_len);
        max = Integer.parseInt(s);

        mainView = ctxt.getLayoutInflater().inflate
                (R.layout.keymapeditor, null);

        this.setContentView(mainView);

        actionEdit = mainView.findViewById(R.id.input_text);
        actionEdit.setTypeface(ctxt.monoFont);

        String actions[] = getUserKeymaps();

        row1 = new ButtonRow(actions[0]);
        row1.rowNumber = 1;
        row1.container = mainView.findViewById(R.id.row1);

        row2 = new ButtonRow(actions[1]);
        row2.rowNumber = 2;
        row2.container = mainView.findViewById(R.id.row2);

        curRow = null;
        rebuildButtons(row1);
        rebuildButtons(row2);

        ImageButton btn = mainView.findViewById(R.id.add_button1);
        btn.setTag("action:add1");
        btn.setOnClickListener(this);

        btn = mainView.findViewById(R.id.add_button2);
        btn.setTag("action:add2");
        btn.setOnClickListener(this);

        btn = mainView.findViewById(R.id.delete_button);
        btn.setTag("action:delete");
        btn.setOnClickListener(this);

        /*
        btn = mainView.findViewById(R.id.update_button);
        btn.setTag("action:update");
        btn.setOnClickListener(this);
        */

        btn = mainView.findViewById(R.id.back_button);
        btn.setTag("action:backward");
        btn.setOnClickListener(this);

        btn = mainView.findViewById(R.id.forw_button);
        btn.setTag("action:forward");
        btn.setOnClickListener(this);

        Button btn2 = mainView.findViewById(R.id.run_button);
        btn2.setTag("action:run");
        btn2.setTypeface(ctxt.iconFont);
        btn2.setOnClickListener(this);

        btn2 = mainView.findViewById(R.id.esc_button);
        btn2.setTag("action:esc");
        btn2.setOnClickListener(this);

        btn2 = mainView.findViewById(R.id.spc_button);
        btn2.setTag("action:spc");
        btn2.setOnClickListener(this);

        btn2 = mainView.findViewById(R.id.tab_button);
        btn2.setTag("action:tab");
        btn2.setOnClickListener(this);

        btn2 = mainView.findViewById(R.id.opa_button);
        btn2.setTag("action:opa");
        btn2.setTypeface(ctxt.iconFont);
        btn2.setOnClickListener(this);

        btn2 = mainView.findViewById(R.id.ret_button);
        btn2.setTag("action:ret");
        btn2.setOnClickListener(this);
    }

    public void show()
    {
        this.showAtLocation(parent,
                Gravity.TOP | Gravity.LEFT,
                0, 0);
    }

    public void rebuildButtons(ButtonRow row)
    {
        if (row == row1) {
            row2.curIdx = -1;
        }
        else {
            row1.curIdx = -1;
        }
        rebuildButtonsAux(row1);
        rebuildButtonsAux(row2);
    }

    public void rebuildButtonsAux(ButtonRow row)
    {
        if (row == null) return;

        row.container.removeAllViews();

        Iterator<String> it = row.actionLst.iterator();

        while (it.hasNext()) {

            String action = it.next();

            String label = action;
            //if (label.length() > 3) label = label.substring(0,3);

            Button btn = (Button)ctxt.getLayoutInflater().inflate
                    (R.layout.popupbutton, null);
            btn.setText(label);
            btn.setTag(row.getPrefix()+action);
            btn.setTypeface(ctxt.monoFont);
            btn.setOnClickListener(this);

            row.container.addView(btn);

            if (row.container.indexOfChild(btn) == row.curIdx) {
                btn.setTextColor(Color.BLUE);
            }
        }
    }

    public void actionAdd(String cmd)
    {
        String action = actionEdit.getText().toString().trim();

        if (action.length() == 0) return;

        if (cmd.equals("action:add1")) curRow = row1;
        else curRow = row2;

        if (curRow.actionLst.contains(action)) return;

        if (curRow.curIdx < 0) {
            curRow.curIdx = curRow.actionLst.size();
        }
        else {
            ++curRow.curIdx;
        }

        curRow.actionLst.add(curRow.curIdx, action);

        rebuildButtons(curRow);
    }

    public void actionUpdate(boolean doRebuild)
    {
        String action = actionEdit.getText().toString().trim();

        if (action.length() == 0) return;

        if (curRow == null) return;

        if (curRow.actionLst.contains(action)) return;

        if (curRow.curIdx < 0) return;

        curRow.actionLst.set(curRow.curIdx, action);

        if (doRebuild) rebuildButtons(curRow);
    }

    public void actionDelete()
    {
        if (curRow == null) return;

        if (curRow.curIdx < 0) return;

        curRow.actionLst.remove(curRow.curIdx);

        curRow.curIdx = -1;

        rebuildButtons(curRow);
    }

    public void actionMoveItem(int dir)
    {
        // Save current
        actionUpdate(false);

        if (curRow == null) return;

        if (curRow.curIdx < 0) return;

        int other = curRow.curIdx + dir;

        if (other < 0 || other >= curRow.actionLst.size()) return;

        String action = curRow.actionLst.get(other);

        curRow.actionLst.set(other,
                curRow.actionLst.get(curRow.curIdx));

        curRow.actionLst.set(curRow.curIdx, action);

        curRow.curIdx = other;

        rebuildButtons(curRow);
    }

    public void closeSoftKeyboard()
    {
        View v = actionEdit;
        InputMethodManager imm = (InputMethodManager)ctxt
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public void dismiss() {

        // Save current
        actionUpdate(false);

        String keymaps = row1.formatKeymaps() + "#rowsep#" +
                row2.formatKeymaps();
        //Preferences.setUserKeymaps(keymaps);
        Preferences.getActiveProfile().setKeymaps(keymaps);
        Preferences.saveProfiles();

        ctxt.rebuildViews();

        closeSoftKeyboard();

        super.dismiss();
    }

    public void addText(String txt)
    {
        txt = actionEdit.getText()+txt;
        if (txt.length() <= max) assignText(txt);
    }

    public void assignText(String txt)
    {
        actionEdit.setText(txt);
        actionEdit.setSelection(actionEdit.getText().length());
    }

    @Override
    public void onClick(View v) {

        String tag = (String)v.getTag();

        if (tag.equals("action:add1") ||
                tag.equals("action:add2")) {
            actionAdd(tag);
            return;
        }

        /*
        if (tag.equals("action:update")) {
            actionUpdate();
            return;
        }
        */

        if (tag.equals("action:delete")) {
            actionDelete();
            return;
        }

        if (tag.equals("action:forward")) {
            actionMoveItem(1);
            return;
        }

        if (tag.equals("action:backward")) {
            actionMoveItem(-1);
            return;
        }

        if (tag.equals("action:close")) {
            this.dismiss();
            return;
        }

        if (tag.equals("action:run")) {
            assignText("run");
            return;
        }

        if (tag.equals("action:ret")) {
            addText("\\n");
            return;
        }

        if (tag.equals("action:esc")) {
            addText("\\e");
            return;
        }

        if (tag.equals("action:spc")) {
            addText("\\s");
            return;
        }

        if (tag.equals("action:tab")) {
            addText("\\t");
            return;
        }

        if (tag.equals("action:opa")) {
            assignText("opacity");
            return;
        }

        if (tag.startsWith("row:")) {

            // Save current
            actionUpdate(false);

            String action = tag.substring(6);
            assignText(action);

            if (tag.startsWith(row1.getPrefix())) {
                curRow = row1;
            }
            else {
                curRow = row2;
            }

            curRow.curIdx = curRow.actionLst.indexOf(action);
            rebuildButtons(curRow);
            return;
        }
    }
}
