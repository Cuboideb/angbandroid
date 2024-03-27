package org.rephial.xyangband;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
//import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.util.Log;
//import android.util.TypedValue;
import android.view.Gravity;
//import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
//import android.view.View.OnClickListener;
import android.view.GestureDetector;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
//import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.MotionEvent;
//import android.widget.PopupMenu;
import android.widget.PopupWindow;
//import android.widget.ScrollView;
//import android.widget.TableLayout;
import android.view.ViewGroup.LayoutParams;
//import android.widget.TableRow;
//import android.widget.TextView;
//import android.widget.HorizontalScrollView;
//import android.os.Handler;

import androidx.core.graphics.ColorUtils;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

class AdvButton extends View
{
	final public GameActivity context;
	public StateManager state;
	public String defaultValue;
	public String activeValue;
	public AdvKeyboard parent;
	public String keymap = "";
	public boolean alwaysVisible = false;
	public boolean keymapMode = false;
	public static int DEFAULT_BG = 0x4a4855;
	public static int TOGGLED_BG = 0x86bf36;
	public boolean pressed = false;

	public AdvButton(GameActivity p_context, AdvKeyboard p_parent,
		String p_txt)
	{
		super(p_context);

		parent = p_parent;

		context = p_context;
		state = context.state;

		activeValue = defaultValue = p_txt;
	}

	public void setShiftMode(int mode)
	{
		if (usingKeymap()) return;

		if (defaultValue.length() == 1) {
			char chr = defaultValue.charAt(0);
			if (Character.isAlphabetic(chr)) {
				if (mode == 0) {
					activeValue = defaultValue;
				}
				if (mode == 1) {
					activeValue = ""+Character.toUpperCase(chr);
				}
				if (mode == 2) {
					activeValue = "^"+Character.toUpperCase(chr);
				}
				invalidate();
			}
		}
	}

	public boolean neverKeymap()
	{
		return defaultValue.equals(InputUtils.Visibility)
			|| defaultValue.equals(InputUtils.Enter)
			|| defaultValue.equals(InputUtils.Escape)
			|| defaultValue.equals(InputUtils.Shift)
			//|| defaultValue.equals(InputUtils.BackSpace)
			//|| defaultValue.equals(" ")
			|| defaultValue.equals("+/-")
			|| defaultValue.equals("abc")
			|| defaultValue.equals("kmp")
			|| defaultValue.equals("lck")
			|| defaultValue.equals(InputUtils.Menu);
	}

	public boolean neverHidden()
	{
		if (alwaysVisible) return true;

		return defaultValue.equals(InputUtils.Visibility)
			|| defaultValue.equals(InputUtils.Enter)
			|| defaultValue.equals(InputUtils.Escape)
			//|| defaultValue.equals(" ")
			//|| defaultValue.equals(".")
			//|| defaultValue.equals("run")
			//|| defaultValue.equals("+/-")
			//|| defaultValue.equals("abc")
			//|| defaultValue.equals("kmp")
			//|| defaultValue.equals(InputUtils.Menu)
			;
	}

	public boolean usingKeymap()
	{
		return (keymapMode && keymap.length() > 0);
	}

	public boolean isVisible()
	{
		return parent.opacityMode != 1 || neverHidden();
	}

	public void execute()
	{
		String action = activeValue;

		if (!isVisible()) {
			parent.setOpacityMode(0);
			return;
		}

		if (action.equals("kmp")) {
			parent.toggleKeymapMode();
			return;
		}

		if (action.equals("lck")) {
			parent.toggleLock();
			return;
		}

		if (usingKeymap()) {
			action = keymap;
		}
		else {
			char code = InputUtils.codeFromName(action);
			if (code > 0) {
				state.addKey(code);
				parent.exitShiftMode();
				return;
			}
		}

		if (action.equals(InputUtils.Visibility)) {
			parent.changeOpacityMode();
			return;
		}

		if (action.equals(InputUtils.Shift)) {
			parent.changeShiftMode();
			return;
		}

		if (action.equals(InputUtils.Menu)) {
			context.handler.sendEmptyMessage(AngbandDialog.Action.
				OpenContextMenu.ordinal());
			return;
		}

		if (action.equals("+/-") || action.equals("abc")) {
			parent.changePage();
			return;
		}

		if (action.equals("run")) {
			state.setRunningMode(!state.getRunningMode());
			invalidate();
			context.refreshInputWidgets();
			return;
		}

		InputUtils.processAction(state, action);

		parent.exitShiftMode();
	}

	public static void closeSoftKeyboard(Activity ctxt, View v)
	{
		InputMethodManager imm = (InputMethodManager)ctxt
			.getSystemService(Activity.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	public void configure(String action, boolean visible)
	{
		if (!action.equals(keymap) ||
			visible != alwaysVisible) {
			keymap = action;
			alwaysVisible = visible;
			invalidate();
			parent.updateKey(defaultValue, action, visible);
			parent.persistKeymaps();
		}
	}

	public class OptionPopup extends PopupWindow
		implements View.OnClickListener
	{
		public EditText etext = null;
		public CheckBox ckAlwaysVisible = null;

		public int max = 0;

		public OptionPopup()
		{
			super(context);

			String s = context.getResources().getString(R.string.def_keymap_len);
			max = Integer.parseInt(s);

			setFocusable(true);
			setWidth(LayoutParams.WRAP_CONTENT);
			setHeight(LayoutParams.WRAP_CONTENT);

			LinearLayout content = (LinearLayout)
				context.getLayoutInflater().inflate
				(R.layout.adv_button_options, null);

			etext = (EditText)content.findViewById
				(R.id.keymap_action);

			assignText(keymap);

			ckAlwaysVisible = (CheckBox)content.findViewById
				(R.id.always_visible);
			ckAlwaysVisible.setChecked(alwaysVisible);

			Button btn = content.findViewById(R.id.esc_button);
			btn.setTag("action:esc");
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.spc_button);
			btn.setTag("action:spc");
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.tab_button);
			btn.setTag("action:tab");
			btn.setOnClickListener(this);

			btn = content.findViewById(R.id.ret_button);
			btn.setTag("action:ret");
			btn.setOnClickListener(this);

			setContentView(content);
		}

		public void addText(String txt)
		{
			txt = etext.getText()+txt;
			if (txt.length() <= max) assignText(txt);
		}

		public void assignText(String txt)
		{
			etext.setText(txt);
			etext.setSelection(etext.getText().length());
		}

		@Override
		public void onClick(View v) {

			String tag = (String)v.getTag();

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
		}

		@Override
		public void dismiss()
		{
			String action = etext.getText().toString().trim();

			closeSoftKeyboard(context, etext);

			configure(action, ckAlwaysVisible.isChecked());

			super.dismiss();
		}
	}

	public void showOptions()
	{
		OptionPopup win = new OptionPopup();

		int gravity = Gravity.TOP
			| Gravity.CENTER_HORIZONTAL;
		win.showAtLocation(this, gravity, 0, 10);
	}

	public boolean atCenter()
	{
		int[] loc = new int[2];
		getLocationInWindow(loc);
		int x = loc[0];

		float pad_pct = 0.33f;
		int midx = x + getWidth() / 2;
		float pct = (float)midx / Math.max(1, parent.winSize.x);
		boolean at_center = false;
		if (pct > pad_pct && pct < (1.0f - pad_pct)) {
			at_center = true;
		}

		return at_center;
	}

	public int calculateAlphaBg()
	{
		int min = 10;
		int max = 200;

		if (parent.opacityMode == 1 && !neverHidden()) {
			return min;
		}

		if (parent.opacityMode == 2) {
			return max;
		}

		// Normal
		return 80;
	}

	public int calculateAlphaFg()
	{
		int min = TermView.MIN_OPACITY;
		int max = 255;

		if (parent.opacityMode == 1 && !neverHidden()) {
			return min;
		}

		if (parent.opacityMode == 2) {
			return max;
		}

		int p = Preferences.getKeyboardOpacity();
		p = min + p * (max-min) / 100;

		if (atCenter()) {
			p = (p * Preferences.getMiddleOpacity()) / 100;
		}

		return Math.max(p, min);
	}

	public void setBgColor(Paint back)
	{
		if (pressed) {
			back.setColor(TOGGLED_BG);
			//back.setColor(DEFAULT_BG);
		}
		else if (defaultValue.equals(" ")) {
			back.setColor(TOGGLED_BG);
		}
		else if (usingKeymap()) {
			back.setColor(DEFAULT_BG);
		}
		else if (defaultValue.equals("kmp") && keymapMode) {
			back.setColor(TOGGLED_BG);
		}
		else if (defaultValue.equals("run")
			&& state.getRunningMode()) {
			back.setColor(TOGGLED_BG);
		}
		else if (defaultValue.equals("lck") && parent.locked) {
			back.setColor(TOGGLED_BG);
		}
		else {
			back.setColor(DEFAULT_BG);
		}
	}

	public void setFgColor(Paint fore)
	{
		if (usingKeymap()) {
			fore.setColor(TOGGLED_BG);
		}
		else {
			fore.setColor(Color.WHITE);
		}
	}

	public void setPressed(boolean value)
	{
		if (value != pressed) {
			pressed = value;
			invalidate();
		}
	}

	public String getLabel()
	{
		String label = activeValue;
		if (usingKeymap() && Preferences.getShowAdvKeymaps()) {
			label = keymap;
		}
		return label;
	}

	protected void onDraw(Canvas canvas)
	{
		Rect bounds = new Rect();

		getDrawingRect(bounds);

		Paint back = parent.back;
		Paint fore = parent.fore;

		back.setColor(Color.BLACK);
		back.setAlpha(10);
		canvas.drawRect(bounds, back);

		int pad = parent.getPad();

		bounds.top += pad;
		bounds.left += pad;
		bounds.bottom -= pad;
		bounds.right -= pad;

		String label = getLabel();
		if (label.length() > 3) label = label.substring(0, 3);
		if (label.length() == 1) fore = parent.foreBold;

		setBgColor(back);
		back.setAlpha(calculateAlphaBg());
		canvas.drawRect(bounds, back);

		/*
		back.setColor(0xAAAAAA);
		back.setAlpha(calculateAlphaBg());
		canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, back);
		*/

		int tw = getWidth() - pad * 2;
		int th = getHeight() - pad * 2;

		float w2 = fore.measureText(label);
		float padx = Math.max((tw - w2) / 2, 0);
		float h2 = fore.descent() - fore.ascent();
		float pady = Math.max((th - h2) / 2, 0)	+ fore.descent();

		/*
		// Move the label to the top
		if (pressed) {
			// ascent is negative
			pady = th + fore.ascent();
		}
		*/

		setFgColor(fore);
		fore.setShadowLayer(10f, 0, 0, Color.CYAN);
		fore.setAlpha(calculateAlphaFg());

		canvas.drawText(label,
			bounds.left + padx,
			bounds.top + th - pady,
			fore);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec,
		int heightMeasureSpec)
	{
		int w = parent.btnWidth;
		int h = parent.btnHeight;

		setMeasuredDimension(w, h);
	}

	public void longPress()
	{
		if (!neverKeymap()) {
			showOptions();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}
}
