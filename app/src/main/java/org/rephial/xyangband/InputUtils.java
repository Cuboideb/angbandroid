package org.rephial.xyangband;

import java.util.List;
import java.util.ArrayList;

class InputUtils
{
    public static String Visibility = "⎘";
	public static String Menu = "▤";
	public static String Shift = "⇧";
	public static String BackSpace = "⌫";
	public static String Enter = "⏎";
	public static String Escape = "⎋";

	public static char KC_TAB = 0x9D;
    public static char KC_BACKSPACE = 0x9F;
	public static char KC_ENTER = 0x9C;
    public static char KC_ESCAPE = 0xE000;

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
        KC_ENTER, KC_BACKSPACE, KC_ESCAPE,
        0
    };

    public static String keynames[] = {
        "tab",
        "F1", "F2", "F3", "F4", "F5", "F6",
        "F7", "F8", "F9", "F10", "F11", "F12",
        Enter, BackSpace, Escape,
        ""
    };

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

        for (int i = 0; keycodes[i] != 0; i++) {
            if (keycodes[i] == c) {
                return keynames[i];
            }
        }

        return Character.toString(c);
    }

    public static char codeFromName(String p_name)
    {
    	int i = 0;
        for (String name: keynames) {
            if (name.equals(p_name)) {
                return keycodes[i];
            }
            i++;
        }
        return 0;
    }

    public static char getKeyCode(String printable)
    {
        if (printable.length() == 0) {
            return 0;
        }

        int i = 0;
        for (String name: keynames) {
            if (name.equals(printable)) {
                return keycodes[i];
            }
            i++;
        }

        if (isKtrl(printable)) {
            return KTRL(printable.charAt(1));
        }

        return printable.charAt(0);
    }

    public static List<Integer>	parseCodeKeys(String txt)
    {
        int i = 0;
        int n = txt.length();
        int ch0;
        char next;
        ArrayList<Integer> lst = new ArrayList<>();

        while (i < n) {
            ch0 = txt.charAt(i);
            next = 0;

            // We have another char
            if ((i+1) < n) next = txt.charAt(i+1);
                            
            if (ch0 == '^' && Character.isAlphabetic(next)) {
                ch0 = KTRL(next);
                i += 1;
            }
            else if (ch0 == '\\' &&
                Character.toLowerCase(next) == 'n') {
                ch0 = KC_ENTER;
                i += 1;
            }
            else if (ch0 == '\\' &&
                Character.toLowerCase(next) == 't') {
                ch0 = KC_TAB;
                i += 1;
            }
            else if (ch0 == '\\' &&
                Character.toLowerCase(next) == 's') {
                ch0 = ' ';
                i += 1;
            }
            else if (ch0 == '\\' &&
                Character.toLowerCase(next) == 'e') {
                ch0 = KC_ESCAPE;
                i += 1;
            }
            else if (ch0 == '\\') {
                // ignore, and next char too
                ch0 = 0;
                i += 1;
            }            

            if (ch0 > 0) lst.add(ch0);
            i += 1;
        }
        return lst;
    }
}