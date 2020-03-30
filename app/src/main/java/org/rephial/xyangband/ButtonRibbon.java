package org.rephial.xyangband;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class ButtonRibbon implements OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    static int count = 1;
    static Typeface fontCmd = null;

    View rootView = null;
    GameActivity context = null;
    StateManager state = null;
    LinearLayout atLeft = null;
    LinearLayout atCenter = null;
    LinearLayout atRight = null;
    boolean fastMode = false;
    int fixedCount = 0;
    ArrayList<Command> commands = null;
    boolean shifted = false;
    boolean controlDown = false;
    boolean commandMode = false;
    int alphaLevel = 2;
    ArrayList<ButtonRibbon> siblings = null;

    public static char KC_TAB = 0x9D;
    public static char KC_BACKSPACE = 0x9F;

    public static char KC_ARROW_DOWN = 0x80;
    public static char KC_ARROW_LEFT = 0x81;
    public static char KC_ARROW_RIGHT = 0x82;
    public static char KC_ARROW_UP = 0x83;

    public static char KC_F1 = 0x84;
    public static char KC_F2 = 0x85;
    public static char KC_F3 = 0x86;
    public static char KC_F4 = 0x87;
    public static char KC_F5 = 0x88;
    public static char KC_F6 = 0x89;
    public static char KC_F7 = 0x8A;
    public static char KC_F8 = 0x8B;
    public static char KC_F9 = 0x8C;
    public static char KC_F10 = 0x8D;
    public static char KC_F11 = 0x8E;
    public static char KC_F12 = 0x8F;

    public static char keycodes[] = {
            KC_TAB,
            KC_F1, KC_F2, KC_F3, KC_F4, KC_F5, KC_F6,
            KC_F7, KC_F8, KC_F9, KC_F10, KC_F11, KC_F12,
            0
    };

    public static String keynames[] = {
            "tab",
            "F1", "F2", "F3", "F4", "F5", "F6",
            "F7", "F8", "F9", "F10", "F11", "F12",
            ""
    };

    public enum CmdLocation {
        Fixed,
        Dynamic,
        Keymaps
    }

    public static char KTRL(char c)
    {
        return (char)(c & 0x1F);
    }

    public static char UN_KTRL(char c)
    {
        if (c < 0x01 || c > 0x1F) {
            return c;
        }
        return (char)(c + 64);
    }

    public static boolean isKtrl(String str)
    {
        return str.length() == 2 && str.charAt(0) == '^' &&
                Character.isAlphabetic(str.charAt(1));
    }

    public static String printableChar(char c)
    {
        if (c >= 1 && c <= 0x1F) {
            return "^" + UN_KTRL(c);
        }

        if (c == KC_TAB) {
            return "tab";
        }

        return Character.toString(c);
    }

    public class Command {
        public String label;
        public String action;
        public char charValue;
        public boolean isAlpha;
        public CmdLocation location;
        public Button btn;

        public Command(String p_label, String p_action,
               CmdLocation p_location, Button p_btn)
        {
            label = p_label;
            action = p_action;
            charValue = 0;
            location = p_location;
            btn = p_btn;
            isAlpha = false;

            // Single character
            if (label.length() == 1) {
                charValue = label.charAt(0);
                label = printableChar(charValue);
            }

            if (action.length() == 0) {
                action = label;
            }

            setButtonText(label);

            // Just a simple alphabetic character
            if (Character.isAlphabetic(charValue) && action == label) {
                isAlpha = true;
            }
        }

        public void setControlDown(boolean set)
        {
            String text;

            if (isAlpha) {
                if (set) {
                    text = "^" + label.toUpperCase();
                    action = text;
                }
                else {
                    action = text = label;
                }
                setButtonText(text);
                btn.invalidate();
            }
        }

        public void setShift(boolean set) {
            // Lower letter ?
            if (isAlpha) {
                if (Character.isUpperCase(charValue)) {
                    set = !set;
                }
                if (set) {
                    action = label.toUpperCase();
                }
                else {
                    action = label.toLowerCase();
                }
                setButtonText(action);
                btn.invalidate();
            }
        }

        public boolean isCommand()
        {
            return action.length() > 4 && action.startsWith("CMD_");
        }

        public char getCommand(boolean translate)
        {
            if (isCommand()) {
                char cmd = action.charAt(4);
                if (translate && state.isRoguelikeKeyboard()) {
                    // Query the core about the rogue key
                    String q = "rogue_key_" + cmd;
                    cmd = (char)state.nativew.gameQueryInt(1, new String[]{q});
                    //String aux = printableChar(cmd);
                    //Log.d("Angband", "Rogue: " + aux);
                }
                return cmd;
            }
            return 0;
        }

        public char fixIcon()
        {
            char raw = getCommand(false);
            if (raw == 'h') raw = 0xF9;
            if (raw == 'U') raw = 0xF5;
            if (raw == 'n') raw = 0xD2;
            if (raw == '~') raw = 0xEF;
            if (raw == '+') raw = 0x42;
            return raw;
        }

        public void setIconTooltip()
        {
            // Set tooltip text
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                char cmd = getCommand(true);
                btn.setTooltipText(printableChar(cmd));
            }
        }

        public void setButtonText(String txt)
        {
            // Set icon
            if (isCommand()) {
                if (Preferences.getIconsEnabled() &&
                        ButtonRibbon.fontCmd != null) {
                    btn.setTypeface(ButtonRibbon.fontCmd);
                    setIconTooltip();
                    txt = Character.toString(fixIcon());
                }
                else {
                    txt = printableChar(getCommand(true));
                }
                btn.setText(txt);
            }
            // Set bold
            else if (txt.length() == 1) {
                SpannableString spanString = new SpannableString(txt);
                spanString.setSpan(new StyleSpan(Typeface.BOLD),
                        0, spanString.length(), 0);
                btn.setText(spanString);
            }
            // Normal text
            else {
                btn.setText(String.format("%.3s", txt));
            }
        }

        public boolean isKeymap()
        {
            return action.startsWith("KEYMAP_");
        }

        public int getTrigger()
        {
            try {
                String code = action.substring("KEYMAP_".length());
                return Integer.parseInt(code);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    public ButtonRibbon(GameActivity p_context, StateManager p_state,
                        boolean p_fastMode) {
        context = p_context;
        state = p_state;
        fastMode = p_fastMode;

        rootView = context.getLayoutInflater().inflate(R.layout.buttonribbon, null);

        atLeft = rootView.findViewById(R.id.at_left);
        atCenter = rootView.findViewById(R.id.at_center);
        atRight = rootView.findViewById(R.id.at_right);

        commands = new ArrayList();
        siblings = new ArrayList();

        alphaLevel = Preferences.getRibbonAlpha();

        if (fontCmd == null) {
            try {
                fontCmd = Typeface.createFromAsset(context.getAssets(),"ui-cmd.ttf");
            } catch (Exception ex) {
                Log.d("Angband", "Can't load cmd font");
                fontCmd = null;
            }
        }

        // Fixed keys
        if (fastMode) {
            makeCommand("⎋", "Esc", CmdLocation.Fixed);
            makeCommand("⏎", "Ret", CmdLocation.Fixed);
            makeCommand("⌫", "BackSpace", CmdLocation.Fixed);

            rebuildKeymaps();
        }
        else {
            makeCommand("▤", "Kbd", CmdLocation.Fixed);
            makeCommand("⇧", "Sft", CmdLocation.Fixed);
            makeCommand("^", "Ctrl", CmdLocation.Fixed);
        }

        setCommandMode(true);
    }

    public void addSibling(ButtonRibbon ribbon)
    {
        siblings.add(ribbon);
    }

    public void setCommandMode(boolean set)
    {
        if (fastMode || set == commandMode) {
            return;
        }

        removeCommands(atCenter);

        if (set) {
            // Hide shift and ctrl
            atLeft.getChildAt(1).setVisibility(View.GONE);
            atLeft.getChildAt(2).setVisibility(View.GONE);

            String txt = ".iUmhfvngdR+b,l[]C~LM=?";
            for (char c: txt.toCharArray()) {
                String label = Character.toString(c);
                String action = "CMD_" + c;
                makeCommand(label, action, CmdLocation.Dynamic);
            }
        }
        else {
            // Show shift and ctrl
            atLeft.getChildAt(1).setVisibility(View.VISIBLE);
            atLeft.getChildAt(2).setVisibility(View.VISIBLE);

            setKeys("abcdefghijklmnopqrstuvwxyz " +
                    "012456789.,*'~!#$%&<>|" +
                    "/\\=[]{}@+-_:;\"", CmdLocation.Dynamic);
            // Other keys
            for (int i = 0; keynames[i].length() > 0; i++) {
                makeCommand(keynames[i], Character.toString(keycodes[i]),
                        CmdLocation.Dynamic);
            }
        }

        atLeft.requestLayout();
        atLeft.invalidate();
        atCenter.invalidate();

        commandMode = set;
        shifted = false;
        controlDown = false;
    }

    public Button makeButton(String text, String action) {
        Button btn = (Button)context.getLayoutInflater().inflate(R.layout.singlebutton, null);
        btn.setText(text);
        btn.setOnClickListener(this);
        if (action.equals("Ret")) {
            int w = btn.getMinWidth();
            btn.setWidth((int)(w * 1.5f));
        }
        return btn;
    }

    public void removeCommands(ViewGroup parent)
    {
        if (parent.getChildCount() == 0) {
            return;
        }

        for (int i = 0; i < parent.getChildCount(); i++) {
            Button btn = (Button)parent.getChildAt(i);
            Command cmd = findCommand(btn);
            commands.remove(cmd);
        }

        parent.removeAllViews();
        parent.invalidate();
    }

    public void setShift(boolean set)
    {
        if (fastMode || commandMode ||
                (set == shifted && !controlDown)) {
            return;
        }

        for (Command cmd: commands) {
            cmd.setShift(set);
        }
        shifted = set;
        controlDown = false;
    }

    public void setControlDown(boolean set)
    {
        if (fastMode || commandMode ||
                (set == controlDown && !shifted)) {
            return;
        }

        for (Command cmd: commands) {
            cmd.setControlDown(set);
        }
        controlDown = set;
        shifted = false;
    }

    public void makeCommand(String label, String action, CmdLocation loc)
    {
        Button btn = makeButton(label, action);
        Command cmd = new Command(label, action, loc, btn);
        commands.add(cmd);
        updateAlphaCmd(cmd);
        if (loc == CmdLocation.Fixed) {
            atLeft.addView(btn);
        }
        else if (loc == CmdLocation.Keymaps) {
            atRight.addView(btn);
        }
        else {
            atCenter.addView(btn);
        }
    }

    public void clearFastKeys() {
        if (fastMode) {
            removeCommands(atCenter);
        }
    }

    public void rebuildKeymaps()
    {
        removeCommands(atRight);
        atRight.invalidate();

        String coreKeymaps = Preferences.getCoreKeymaps();
        if (coreKeymaps.length() == 0) {
            return;
        }

        String keymaps[] = coreKeymaps.split("###");
        Arrays.sort(keymaps);
        for (String str: keymaps) {
            String parts[] = str.split("##");
            if (parts.length != 2) {
                continue;
            }
            String label = parts[0];
            String action = "KEYMAP_" + parts[1];
            makeCommand(label, action, CmdLocation.Keymaps);
        }
    }

    public void addFKeys(CmdLocation loc)
    {
        for (int i = 0; keynames[i].length() > 0; i++) {
            if (keycodes[i] >= KC_F1 && keycodes[i] <= KC_F12) {
                makeCommand(keynames[i], Character.toString(keycodes[i]),
                        loc);
            }
        }
    }

    public void setKeys(String keys, CmdLocation loc) {
        clearFastKeys();

        if (fastMode && loc == CmdLocation.Dynamic) {
            if (keys.length() > 0) {
                removeCommands(atRight);

                String pattern = "[^fkeys$]";
                if (keys.contains(pattern)) {
                    addFKeys(loc);
                    // Remove the pattern
                    keys = keys.replace(pattern, "");
                }
            }
            else if (atRight.getChildCount() == 0) {
                rebuildKeymaps();
            }
        }

        for (char c: keys.toCharArray()) {
            String label = Character.toString(c);
            makeCommand(label, "", loc);
        }
    }

    public Command findCommand(Button btn)
    {
        for (Command cmd: commands) {
            if (cmd.btn == btn) {
                return cmd;
            }
        }
        return null;
    }

    public void showMenu()
    {
        PopupMenu popup = new PopupMenu(context, rootView);

        popup.inflate(R.menu.ribbon);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void updateAlphaCmd(Command cmd)
    {
        int alphaBg = 50;
        int alphaFg = (255 * Preferences.getKeyboardOpacity()) / 100;
        int color = Color.WHITE;

        if (cmd.action.equals("Ret")) {
            alphaBg = 100;
        }

        if (alphaLevel == 1) {
            alphaBg = 10;
            alphaFg = 30;
        }
        if (alphaLevel == 3) {
            alphaBg = 200;
            alphaFg = 255;
            color = Color.parseColor("#000044");
        }
        color = ColorUtils.setAlphaComponent(color, alphaFg);
        cmd.btn.setTextColor(color);
        cmd.btn.getBackground().setAlpha(alphaBg);
    }

    public void updateAlpha()
    {
        Preferences.setRibbonAlpha(alphaLevel);

        for (Command cmd: commands) {
            updateAlphaCmd(cmd);
            cmd.btn.invalidate();
        }

        for (ButtonRibbon ribbon: siblings) {
            ribbon.alphaLevel = this.alphaLevel;
            for (Command cmd: ribbon.commands) {
                updateAlphaCmd(cmd);
                cmd.btn.invalidate();
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case (R.id.toggle_ribbon):
                setCommandMode(!commandMode);
                return true;
            case (R.id.show_keyboard):
                context.toggleKeyboard();
                return true;
            case (R.id.lower_opacity):
                alphaLevel = alphaLevel - 1;
                if (alphaLevel < 1) alphaLevel = 2;
                updateAlpha();
                return true;
            case (R.id.raise_opacity):
                alphaLevel = alphaLevel + 1;
                if (alphaLevel > 3) alphaLevel = 2;
                updateAlpha();
                return true;
            case (R.id.show_pref):
                context.handler.sendEmptyMessage(AngbandDialog.Action.
                        OpenContextMenu.ordinal());
                return true;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        Command cmd = findCommand((Button)v);
        if (cmd == null) {
            return;
        }
        String action = cmd.action;

        if (cmd.isKeymap()) {
            state.addKey(cmd.getTrigger());
            return;
        }

        if (cmd.isCommand()) {
            action = Character.toString(cmd.getCommand(true));
            //Log.d("Angband", "Command: " +
            //        printableChar(action.charAt(0)));
        }

        if (action.equals("Kbd")) {
            showMenu();
        }
        else if (action.equals("Esc")) {
            state.addKey(state.getKeyEsc());
        }
        else if (action.equals("Ret")) {
            state.addKey(state.getKeyEnter());
        }
        else if (action.equals("Sft")) {
            setShift(!shifted);
        }
        else if (action.equals("Ctrl")) {
            setControlDown(!controlDown);
        }
        else if (action.equals("BackSpace")) {
            state.addKey(KC_BACKSPACE);
        }
        else if (action.equals("tab")) {
            state.addKey(KC_TAB);
        }
        else if (isKtrl(action)) {
            int key = KTRL(action.charAt(1));
            state.addKey(key);
        }
        else if (action.length() == 1) {
            state.addKey(action.charAt(0));
        }
    }
}