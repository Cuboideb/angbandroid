package org.rephial.xyangband;

import org.rephial.xyangband.StateManager.CoreCommand;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;

public class ButtonRibbon implements OnClickListener,
		PopupMenu.OnMenuItemClickListener {

	static int count = 1;
	static Typeface fontCmd = null;

	ViewGroup rootView = null;
	GameActivity context = null;
	StateManager state = null;
	LinearLayout atLeft = null;
	LinearLayout dynamic1 = null;
	LinearLayout dynamic2 = null;
	LinearLayout atRight = null;
	boolean fastMode = false;
	//int fixedCount = 0;
	ArrayList<Command> commands = null;
	boolean shifted = false;
	boolean controlDown = false;
	boolean commandMode = false;
	int alphaLevel = 2;

	public static ArrayList<ButtonRibbon> instances = new ArrayList<>();

	PopupWindow autoListWin = null;
	TableLayout autoListTable = null;
	boolean autoListSolid = false;

	public static int popupBackAlpha = 255;
	public static int popupButtonColor = Color.BLACK;

	public String[] userKeymaps = null;

	public enum CmdLocation {
		FixedL,
		Dynamic1,
		Dynamic2,
		FixedR
	}

	public enum CmdType {
		KeyCode,
		GameCommand,
		KeyMap
	}

	public enum CmdModifier {
		None,
		Shift,
		Control
	}

	public ButtonRibbon(GameActivity p_context, StateManager p_state, boolean p_fastMode) {
		context = p_context;
		state = p_state;
		fastMode = p_fastMode;

		rootView = (ViewGroup)context.getLayoutInflater().inflate(R.layout.buttonribbon, null);

		//rootView.findViewById(R.id.scrollv).setFocusable(false);

		atLeft = rootView.findViewById(R.id.at_left);
		dynamic1 = rootView.findViewById(R.id.dynamic1);
		dynamic2 = rootView.findViewById(R.id.dynamic2);
		atRight = rootView.findViewById(R.id.at_right);

		commands = new ArrayList();

		alphaLevel = state.opaqueWidgets ? 3: 2;

		fontCmd = context.iconFont;

		//makeCommand(" ", "PageLeft", CmdLocation.FixedL);
		//makeCommand(" ", "PageRight", CmdLocation.FixedR);

		//if (cloneMode) return;

		// Fixed keys
		if (fastMode) {

			rebuildTopFixed("default");

			userKeymaps = KeymapEditor.getUserKeymaps();

			rebuildKeymaps();
		}
		else {
			makeCommand("⎘", "show_keys", CmdLocation.FixedL);
			makeCommand("⇧", "Sft", CmdLocation.FixedL);
			//makeCommand("▤", "Kbd", CmdLocation.FixedL);
			//makeCommand("✦", "do_cmd_list", CmdLocation.FixedL);
			//makeCommand("^", "Ctrl", CmdLocation.FixedL);

			restoreCommandMode();
		}

		instances.add(this);
	}

	// modes: default, yes_no
	public void rebuildTopFixed(String mode)
	{
		CmdLocation loc = CmdLocation.FixedL;

		removeCommands(atLeft);

		makeCommand("⎋", "Esc", loc);

		if (mode.equals("yes_no")) {
			makeCommand("n", "n", loc);
			makeCommand("y", "y", loc);
		}
		else {
			makeCommand("⏎", "Ret", loc);
			makeCommand("⌫", "BackSpace", loc);
		}
	}

	public void restoreCommandMode()
	{
		setCommandMode(Preferences.getCommandMode());
	}

	public ViewGroup getGroup(CmdLocation loc)
	{
		if (loc == CmdLocation.Dynamic1) return dynamic1;
		if (loc == CmdLocation.Dynamic2) return dynamic2;
		if (loc == CmdLocation.FixedL) return atLeft;
		return atRight;
	}

	/*
	public void clonify(ButtonRibbon from, CmdLocation loc,
		int first)
	{
		ViewGroup grp = getGroup(loc);
		removeCommands(grp);
		int x0 = 0;
		int i = 0;
		for (Command aux: from.commands) {
			if (aux.location != loc) continue;
			makeCommand(aux.label, aux.action, loc);
			if (i < first) {
				if (aux.btn.getWidth() > 0) {
					x0 += aux.btn.getWidth();
				}
				else {
					x0 += aux.btn.getMinWidth();
				}
			}
			++i;
		}
		final int x = x0;
		final HorizontalScrollView hsv =
			rootView.findViewById(R.id.scrollv);
		hsv.postDelayed(new Runnable() {
			@Override
			public void run() {
			   hsv.scrollTo(x, 0);
			}
		}, 300L);
	}
	*/

	public void setCommandMode(boolean set)
	{
		if (fastMode) {
			return;
		}

		// Already created and equal, do nothing
		if (dynamic1.getChildCount() > 0 && set == commandMode) {
			return;
		}

		removeCommands(dynamic1);

		makeCommand("▤", "Kbd", CmdLocation.Dynamic1);
		makeCommand("✦", "do_cmd_list", CmdLocation.Dynamic1);

		if (set) {
			// Hide shift and ctrl
			atLeft.getChildAt(1).setVisibility(View.GONE);
			//atLeft.getChildAt(4).setVisibility(View.GONE);

			String txt = ".iUmhfvngdR+ewb,azul[]C~LM=?";
			for (char c: txt.toCharArray()) {
				String label = Character.toString(c);
				String action = label;
				makeCommand(label, action, CmdLocation.Dynamic1, CmdType.GameCommand);
			}
		}
		else {
			// Show shift and ctrl
			atLeft.getChildAt(1).setVisibility(View.VISIBLE);
			//atLeft.getChildAt(4).setVisibility(View.VISIBLE);

			setKeys("abcdefghijklmnopqrstuvwxyz " +
					"0123456789.,*'?~!#$%&<>|^" +
					"/\\=()[]{}@+-_:;\"", CmdLocation.Dynamic1);
			// Other keys
			for (int i = 0; InputUtils.keynames[i].length() > 0; i++) {
				makeCommand(InputUtils.keynames[i],
					Character.toString(InputUtils.keycodes[i]),
					CmdLocation.Dynamic1);
			}
		}

		atLeft.requestLayout();
		atLeft.invalidate();
		dynamic1.invalidate();

		commandMode = set;
		shifted = false;
		controlDown = false;
	}

	public static void resizeButtonAux(Button btn, float pct)
	{
		int w = btn.getMinWidth();

		w = (int)(w * pct);
		// Hack, set both to get it working
		btn.setMinimumWidth(w);
		btn.setMinWidth(w);

		float fw = btn.getTextSize();
		btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, fw * pct);
	}

	public static void resizeButton(Button btn)
	{
		int mult = Preferences.getRibbonButtonMult();

		if (mult < 45 || mult > 55) {

			float pct = 0.5f + (mult / 100f);
			pct = Math.min(pct, 1.5f);
			pct = Math.max(pct, 0.5f);

			resizeButtonAux(btn, pct);
		}
	}

	public Button makeButton(String text, String action) {
		Button btn = (Button)context.getLayoutInflater().inflate
				(R.layout.ribbon_command, null);

		btn.setText(text);
		btn.setOnClickListener(this);
		btn.setFocusable(false);

		resizeButton(btn);

		if (action.equals("Ret")) {
			int w = btn.getMinWidth();
			w = (int)(w * 1.5f);
			btn.setWidth(w);
		}

		if (action.equals("PageRight")
			|| action.equals("PageLeft")) {
			int w = btn.getMinWidth();
			w = (int)(w * 0.75f);
			btn.setMinimumWidth(w);
			btn.setMinWidth(w);
			btn.setWidth(w);
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

	public void setShiftAux(boolean set)
	{
		if (commandMode || 	(set == shifted && !controlDown)) {
			return;
		}

		for (Command cmd: commands) {
			cmd.setShift(set);
		}
		shifted = set;
		controlDown = false;
	}

	public static void setShift(boolean set)
	{
		for (ButtonRibbon ribbon: instances) {
			ribbon.setShiftAux(set);
		}
	}

	public void setControlDownAux(boolean set)
	{
		if (commandMode || (set == controlDown && !shifted)) {
			return;
		}

		for (Command cmd: commands) {
			cmd.setControlDown(set);
		}
		controlDown = set;
		shifted = false;
	}

	public static void setControlDown(boolean set)
	{
		for (ButtonRibbon ribbon: instances) {
			ribbon.setControlDownAux(set);
		}
	}

	public void setTriStateShiftAux(boolean set)
	{
		if (commandMode) {
			return;
		}

		if (!set) {
			shifted = false;
			controlDown = false;
		}
		else if (shifted) {
			controlDown = true;
			shifted = false;
		}
		else if (controlDown) {
			controlDown = false;
		}
		else {
			shifted = true;
		}

		for (Command cmd: commands) {
			if (controlDown) {
				cmd.setControlDown(true);
			}
			else {
				cmd.setShift(shifted);
			}
		}
	}

	public static void setTriStateShift(boolean set)
	{
		for (ButtonRibbon ribbon: instances) {
			ribbon.setTriStateShiftAux(set);
		}
	}

	public void makeCommand(String label, String action, CmdLocation loc)
	{
		makeCommand(label, action, loc, CmdType.KeyCode);
	}

	public void makeCommand(String label, String action, CmdLocation loc, CmdType type)
	{
		Button btn = makeButton(label, action);
		Command cmd = new Command(label, action, loc, btn, type);
		commands.add(cmd);
		updateAlphaCmd(cmd);
		getGroup(loc).addView(btn);
	}

	public void removeAutoList()
	{
		//Log.d("Angband", "Remove Auto List");

		if (autoListWin != null) {
			autoListWin.dismiss();
		}

		autoListWin = null;
		autoListTable = null;
		autoListSolid = false;
	}

	public void clearFastKeys() {
		if (fastMode) {

			ViewGroup grp = atLeft;
			for (int i = 0; i < grp.getChildCount(); i++) {
				Button btn = (Button)grp.getChildAt(i);
				Command cmd = findCommand(btn);
				if (cmd == null) continue;
				if (cmd.charValue == 'n' || cmd.charValue == 'y') {
					rebuildTopFixed("default");
					break;
				}
			}

			removeCommands(dynamic1);

			removeAutoList();
		}
	}

	public void rebuildUserKeymaps(String src, CmdLocation loc)
	{
		String keymaps[] = src.split("###");
		//Arrays.sort(keymaps);
		for (String str: keymaps) {

			if (str.length() == 0) continue;

			String label = str.replace("\\s", "")
					.replace("\\S", "")
					.replace("\\e", "")
					.replace("\\E", "")
					.replace("\\n", "")
					.replace("\\N", "");

			if (label.length() > 3) label = str.substring(0, 3);
			String action = str;
			makeCommand(label, action, loc, CmdType.KeyMap);
		}
	}

	public void rebuildKeymaps()
	{
		if (userKeymaps == null || userKeymaps.length != 2) return;

		CmdLocation[] vloc = {
			CmdLocation.Dynamic1,
			CmdLocation.Dynamic2
		};
		for (int i = 0; i < 2; i++) {
			CmdLocation loc = vloc[i];
			ViewGroup grp = getGroup(loc);

			removeCommands(grp);
			grp.invalidate();

			rebuildUserKeymaps(userKeymaps[i], loc);
		}

		/*
		String coreKeymaps = Preferences.getCoreKeymaps();
		if (coreKeymaps.length() > 0) {

			String keymaps[] = coreKeymaps.split("###");
			Arrays.sort(keymaps);
			for (String str: keymaps) {
				String parts[] = str.split("##");
				if (parts.length != 2) {
					continue;
				}
				String label = parts[0];
				String action = "KEYMAP_" + parts[1];
				makeCommand(label, action, loc);
			}
		}
		*/
	}

	public void addFKeys(CmdLocation loc)
	{
		for (int i = 0; InputUtils.keynames[i].length() > 0; i++) {
			if (InputUtils.keycodes[i] >= InputUtils.KC_F1
				&& InputUtils.keycodes[i] <= InputUtils.KC_F12) {
				makeCommand(InputUtils.keynames[i],
					Character.toString(InputUtils.keycodes[i]), loc);
			}
		}
	}

	public void setFastKeys(String keys)
	{
		setKeys(keys, CmdLocation.Dynamic1);
	}

	public void setKeys(String keys, CmdLocation loc) {

		boolean showList = false;
		boolean enableSmall = false;

		clearFastKeys();

		if (fastMode && loc == CmdLocation.Dynamic1) {

			if (keys.length() > 0) {

				if (keys.length() > 6) {
					showList = true;
				}

				String pattern = "${fkeys}";
				if (keys.contains(pattern)) {
					addFKeys(loc);
					// Remove the pattern
					keys = keys.replace(pattern, "");
					// Request auto_show
					showList = true;
				}

				pattern = "${yes_no}";
				if (keys.contains(pattern)) {
					rebuildTopFixed("yes_no");
					// Remove the pattern
					keys = keys.replace(pattern, "");
				}

				enableSmall = true;
			}
			else {
				rebuildKeymaps();
				// Hack - Adjust d-pad position
				if (context.term != null) {
					context.term.postInvalidate();
				}
			}
		}

		for (char c: keys.toCharArray()) {
			String action = Character.toString(c);
			makeCommand("", action, loc);
		}

		if (Preferences.getEnableTouch() && !Preferences.getTouchRight()) {
			showList = false;
		}

		if (!Preferences.getShowAutoList()) {
			showList = false;
		}

		if (showList) {
			showAutoList(rootView);
		}

		if (enableSmall) {
			ViewGroup grp = getGroup(loc);
			int n = grp.getChildCount();
			float pct = Preferences.getReducedButtonWeight();
			if (pct > 0.0f && n > 0 && !canFitButtons(n)) {
				for (int i = 0; i < n; i++) {
					Button btn = (Button)grp.getChildAt(i);
					resizeButtonAux(btn, pct);
					btn.invalidate();
				}
				//grp.requestLayout();
			}
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
		int min = TermView.MIN_OPACITY;
		int max = 255;
		int alphaFg = min + Preferences.getKeyboardOpacity() * (max-min) / 100;
		int color = Color.WHITE;

		if (cmd.action.equals("Ret")) {
			alphaBg = 100;
		}

		if (alphaLevel == 1) {
			alphaFg = 30;
			alphaBg = 10;
		}
		if (alphaLevel == 2 && (cmd.action.equals("PageRight")
			|| cmd.action.equals("PageLeft"))) {
			alphaBg = 200;
		}
		if (alphaLevel == 3) {
			color = 0x000044;
			alphaFg = 255;
			alphaBg = 200;
		}

		if (cmd.action.toUpperCase().equals("RUN") &&
			state.getRunningMode()) {
			if (alphaLevel == 3) {
				color = 0x006400;
			}
			else {
				color = AdvButton.TOGGLED_BG;
			}
		}

		color = ColorUtils.setAlphaComponent(color, alphaFg);
		cmd.btn.setTextColor(color);
		cmd.btn.getBackground().setAlpha(alphaBg);
	}

	public static void updateAlpha(int value)
	{
		for (ButtonRibbon ribbon: instances) {
			ribbon.alphaLevel = value;
			for (Command cmd: ribbon.commands) {
				ribbon.updateAlphaCmd(cmd);
				cmd.btn.invalidate();
			}
		}
	}

	public void toggleCommandMode()
	{
		setCommandMode(!commandMode);
		Preferences.setCommandMode(commandMode);
	}

	public void changeOpacity(int step)
	{
		alphaLevel = alphaLevel + step;
		if (alphaLevel < 1) alphaLevel = 2;
		if (alphaLevel > 3) alphaLevel = 2;
		ButtonRibbon.updateAlpha(alphaLevel);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch(item.getItemId()) {
			case (R.id.toggle_ribbon):
				toggleCommandMode();
				return true;
			case (R.id.show_keyboard):
				context.toggleKeyboard();
				return true;
			case (R.id.lower_opacity):
				changeOpacity(-1);
				return true;
			case (R.id.raise_opacity):
				changeOpacity(+1);
				return true;
			case (R.id.show_pref):
				context.handler.sendEmptyMessage(AngbandDialog.Action.
						OpenContextMenu.ordinal());
				return true;
		}

		return false;
	}

	public void showCommandList(View parentView) {

		// Hack
		if (state.coreCommands.isEmpty()) {
			state.buildCommandList();
		}

		final PopupWindow win = new PopupWindow(context);
		win.setFocusable(true);
		win.setWidth(LayoutParams.WRAP_CONTENT);
		win.setHeight(LayoutParams.WRAP_CONTENT);

		ScrollView scroll = new ScrollView(context);
		scroll.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT
		));

		win.setContentView(scroll);

		TableLayout table = new TableLayout(context);
		//table.setShrinkAllColumns(true);
		table.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT
		));

		scroll.addView(table);

		TableRow trow = null;

		int maxRowItems = 4;
		/*
		if (context.landscapeNow()) {
			maxRowItems = 6;
		}
		*/

		OnClickListener clickListener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer tag = (Integer)v.getTag();
					int key = tag.intValue();
					state.addKey(key);
					win.dismiss();
				}
			};

		for (CoreCommand cmd: state.coreCommands) {
			int key = cmd.getKey();
			String trigger = InputUtils.printableChar((char)key);
			String desc = cmd.desc;

			//Log.d("Angband", trigger + " | " + desc);

			if (trow == null || trow.getChildCount() == maxRowItems) {
				trow = new TableRow(context);
				trow.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT
				));
				table.addView(trow);
			}

			Button btn = (Button)context.getLayoutInflater().inflate
					(R.layout.popupbutton, null);
			//Button btn = new Button(context);
			btn.setText(trigger);
			btn.setTag(new Integer(key));
			btn.setOnClickListener(clickListener);
			btn.setTypeface(context.monoBoldFont);
			btn.getBackground().setAlpha(popupBackAlpha);
			btn.setTextColor(popupButtonColor);
			trow.addView(btn);

			TextView txt = new TextView(context);
			txt.setText(desc);
			txt.setClickable(true);
			txt.setTag(new Integer(key));
			txt.setOnClickListener(clickListener);
			trow.addView(txt);
		}

		if (trow != null && trow.getChildCount() == 0) {
			table.removeView(trow);
		}

		if (table.getChildCount() == 0) return;

		//win.showAsDropDown(parentView);
		//win.update(0, 0, win.getWidth(), win.getHeight());
		win.showAtLocation(parentView,
				Gravity.LEFT | Gravity.TOP,
				0,0);
	}

	public void showDynamicKeys(View parentView) {
		CmdLocation loc = CmdLocation.Dynamic1;

		final PopupWindow win = new PopupWindow(context);
		win.setFocusable(true);
		win.setWidth(LayoutParams.WRAP_CONTENT);
		win.setHeight(LayoutParams.WRAP_CONTENT);

		ScrollView scroll = new ScrollView(context);
		scroll.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT
		));

		win.setContentView(scroll);

		TableLayout table = new TableLayout(context);
		table.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT
		));

		scroll.addView(table);

		TableRow trow = null;

		int maxRowItems = 3;
		if (context.landscapeNow()) {
			maxRowItems = 6;
		}

		OnClickListener clickListener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer tag = (Integer)v.getTag();
					int savedIdx = tag.intValue();
					// Indirection
					Command cmd = commands.get(savedIdx);
					if (cmd != null) cmd.btn.performClick();
					win.dismiss();
				}
			};

		int idx = -1;
		int alphaFg = 220;
		int alphaBg = 50;

		for (Command cmd: this.commands) {
			++idx;

			if (cmd.location != loc) {
				continue;
			}

			if (trow == null || trow.getChildCount() == maxRowItems) {
				trow = new TableRow(context);
				trow.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT
				));
				table.addView(trow);
			}

			Button btn = (Button)context.getLayoutInflater().inflate
				(R.layout.keypopupbutton, null);
			btn.getBackground().setAlpha(popupBackAlpha);
			btn.setTextColor(popupButtonColor);
			//Button btn = new Button(context);
			btn.setText(cmd.btn.getText());
			btn.setTag(new Integer(idx));
			btn.setOnClickListener(clickListener);
			btn.setTypeface(cmd.btn.getTypeface());
			trow.addView(btn);
		}

		if (trow != null && trow.getChildCount() == 0) {
			table.removeView(trow);
		}

		if (table.getChildCount() == 0) return;

		int gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
		int y = 0;
		win.showAtLocation(parentView, gravity, 0, y);
	}

	public void toggleAutoListOpacity()
	{
		if (autoListWin == null || autoListTable == null) return;

		autoListSolid = !autoListSolid;

		//Log.d("Angband", "Auto List Opacity");

		int i, j;

		for (i = 0; i < autoListTable.getChildCount(); i++) {

			TableRow row = (TableRow)autoListTable.getChildAt(i);

			for (j = 0; j < row.getChildCount(); j++) {

				Button btn = (Button)row.getChildAt(j);

				int color = Color.WHITE;
				int alphaFg = 30;
				int alphaBg = 30;

				if (autoListSolid) {
					alphaFg = 255;
					alphaBg = 255;
					color = 0x000044;
				}

				color = ColorUtils.setAlphaComponent(color, alphaFg);
				btn.setTextColor(color);
				btn.getBackground().setAlpha(alphaBg);
			}
		}
	}

	/*
	public void doPageScroll(String action)
	{
		final HorizontalScrollView hsv =
			rootView.findViewById(R.id.scrollv);
		hsv.pageScroll(action.equals("PageLeft") ?
			View.FOCUS_LEFT: View.FOCUS_RIGHT);
	}
	*/

	public boolean canFitButtons(int amount)
	{
		int kbdH = context.getKeyboardHeight();
		Point winSize = context.getWinSize();
		// Calculate the size of a button
		// We have 2 or 3 rows of buttons
		int btnSize = kbdH / (dynamic2.getChildCount() > 0 ? 3: 2);
		// Add fixed items at the left
		amount += 5;
		// Wide enough ?
		return btnSize * amount <= winSize.x;
	}

	public void showAutoList(View parentView) {

		removeAutoList();

		CmdLocation loc = CmdLocation.Dynamic1;

		int minRowItems = 3;
		float screenPct = 0.5f;
		if (context.landscapeNow()) {
			screenPct = 0.65f;
		}

		Point winSize = context.getWinSize();

		int kbdH = context.getKeyboardHeight();
		int maxH = winSize.y - kbdH;
		// Calculate the size of a button
		// We have 2 or 3 rows of buttons
		int btnSize = kbdH / (dynamic2.getChildCount() > 0 ? 3: 2);
		int winH = 0;

		ArrayList<Command> the_list = new ArrayList<>();
		int i = 0;
		int j = 0;
		int[] sourceIdx = new int[200];
		for (Command cmd: commands) {
			if (cmd.location == loc && j < sourceIdx.length) {
				the_list.add(cmd);
				sourceIdx[j++] = i;
			}
			i++;
		}

		if (the_list.isEmpty()) return;

		// Add the fixed keys at the left
		int listW = btnSize * (the_list.size() + 5);
		// There is plenty of space
		if (listW < winSize.x) {
			return;
		}

		// Check if we can add more columns
		int maxRowItems = minRowItems;
		while ((maxRowItems * btnSize) < (int)(winSize.x * screenPct)) {
			++maxRowItems;
		}
		maxRowItems = Math.max(maxRowItems-1, minRowItems);

		// Dummy entry, just to have another one
		the_list.add(the_list.get(0));

		int n = the_list.size();

		int lastIdx = n - 1;

		int rows = (n / maxRowItems);
		if (rows * maxRowItems < n) ++rows;

		winH = rows * btnSize;

		if (winH > 0 && winH < maxH) {
			winH = LayoutParams.WRAP_CONTENT;
		}
		else {
			winH = maxH;
		}

		final PopupWindow win = new PopupWindow(context);

		win.setOutsideTouchable(false);
		//win.setTouchModal(false);
		win.setFocusable(true);
		win.getBackground().setAlpha(0);

		win.setWidth(LayoutParams.WRAP_CONTENT);
		win.setHeight(winH);

		ScrollView scroll = new ScrollView(context);
		scroll.setLayoutParams(new LayoutParams(
			LayoutParams.WRAP_CONTENT,
			LayoutParams.MATCH_PARENT
		));

		win.setContentView(scroll);

		TableLayout table = new TableLayout(context);
		table.setLayoutParams(new LayoutParams(
			LayoutParams.WRAP_CONTENT,
			LayoutParams.MATCH_PARENT));
		scroll.addView(table);

		TableRow trow = null;

		OnClickListener clickListener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer tag = (Integer)v.getTag();
					int savedIdx = tag.intValue();

					if (savedIdx == -1 || !autoListSolid) {
					//if (savedIdx == -1) {
						toggleAutoListOpacity();
						return;
					}

					// Indirection
					Command cmd = commands.get(savedIdx);
					if (cmd != null) cmd.btn.performClick();
				}
			};

		int idx = -1;

		for (Command cmd: the_list) {
			++idx;

			if (trow == null || trow.getChildCount() == maxRowItems) {
				trow = new TableRow(context);
				trow.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));
				trow.setGravity(Gravity.CENTER_VERTICAL);
				table.addView(trow);
			}

			Button btn = (Button)context.getLayoutInflater().inflate
				(R.layout.ribbon_command, null);

			int alphaFg = 30;
			int alphaBg = 30;
			int color = Color.WHITE;

			//Button btn = new Button(context);
			if (idx == lastIdx) {
				alphaFg = 120;
				alphaBg = 60;
				if (fontCmd != null) {
					btn.setTypeface(fontCmd);
				}
				btn.setText("l");
				btn.setTag(new Integer(-1));
			}
			else {
				btn.setText(cmd.btn.getText());
				int realIdx = sourceIdx[idx];
				btn.setTag(new Integer(realIdx));
				btn.setTypeface(cmd.btn.getTypeface());
			}
			btn.setOnClickListener(clickListener);
			btn.setGravity(Gravity.CENTER);

			color = ColorUtils.setAlphaComponent(color, alphaFg);
			btn.setTextColor(color);
			btn.getBackground().setAlpha(alphaBg);

			resizeButton(btn);

			trow.addView(btn);
		}

		if (trow != null && trow.getChildCount() == 0) {
			table.removeView(trow);
		}

		if (table.getChildCount() == 0) {
			return;
		}

		int gravity = Gravity.LEFT | Gravity.TOP;
		int y = 0;

		if (winH == LayoutParams.WRAP_CONTENT && kbdH > 0) {
			gravity = Gravity.LEFT | Gravity.BOTTOM;
			y = kbdH;
		}

		autoListWin = win;
		autoListTable = table;

		//toggleAutoListOpacity();

		win.showAtLocation(parentView, gravity, 0, y);
	}

	@Override
	public void onClick(View v) {
		Command cmd = findCommand((Button)v);
		if (cmd == null) {
			return;
		}
		String action = cmd.getAction();

		if (cmd.isKeymap()) {
			if (action.toUpperCase().equals("RUN")) {
				state.setRunningMode(!state.getRunningMode());
				context.refreshInputWidgets();
				updateAlphaCmd(cmd);
			}
			else if (action.toUpperCase().equals("OPACITY")) {
				context.runOpacityPopup();
			}
			else {
				InputUtils.processAction(state, action);
				ButtonRibbon.setTriStateShift(false);
			}
			return;
		}

		/*
		if (action.equals("PageRight") ||
			action.equals("PageLeft")) {
			doPageScroll(action);
			return;
		}
		*/

		if (cmd.isGameCommand()) {
			action = Character.toString(cmd.getCommand(true));
			//Log.d("Angband", "Command: " +
			//        printableChar(action.charAt(0)));
		}

		if (action.equals("Kbd")) {
			//showMenu();
			context.handler.sendEmptyMessage(AngbandDialog.Action.
					OpenContextMenu.ordinal());
		}
		else if (action.equals("do_cmd_list")) {
			showCommandList(v);
		}
		else if (action.equals("Esc")) {
			state.addKey(state.getKeyEsc());
		}
		else if (action.equals("Ret")) {
			state.addKey(state.getKeyEnter());
		}
		else if (action.equals("Sft")) {
			//setShift(!shifted);
			ButtonRibbon.setTriStateShift(true);
		}
		else if (action.equals("Ctrl")) {
			//setControlDown(!controlDown);
			ButtonRibbon.setTriStateShift(true);
		}
		else if (action.equals("BackSpace")) {
			state.addKey(InputUtils.KC_BACKSPACE);
		}
		else if (action.equals("show_keys")) {
			showDynamicKeys(v);
		}
		else if (action.equals("tab")) {
			state.addKey(InputUtils.KC_TAB);
			//setShift(false);
			ButtonRibbon.setTriStateShift(false);
		}
		else if (InputUtils.isKtrl(action)) {
			int key = InputUtils.KTRL(action.charAt(1));
			state.addKey(key);
			//setShift(false);
			ButtonRibbon.setTriStateShift(false);
		}
		else if (action.length() == 1) {
			state.addKey(action.charAt(0));
			//setShift(false);
			ButtonRibbon.setTriStateShift(false);
		}
	}

	public class Command implements Button.OnLongClickListener {
		public String label;
		public String action;
		public char charValue;
		public boolean isAlpha;
		public CmdLocation location;
		public Button btn;
		public CmdType type;
		public CmdModifier modifier;

		public Command(String p_label, String p_action,
			   CmdLocation p_location, Button p_btn, CmdType p_type)
		{
			label = p_label;
			action = p_action;
			charValue = 0;
			location = p_location;
			btn = p_btn;
			isAlpha = false;
			type = p_type;
			modifier = CmdModifier.None;

			// Single character
			if (action.length() == 1 && !isGameCommand()) {
				charValue = action.charAt(0);

				if (label.length() == 0) {
					// It can be a control character or F1-12 key
					label = InputUtils.printableChar(charValue);
				}

				// Just a simple alphabetic character
				if (Character.isAlphabetic(charValue) && label.equals(action)) {
					isAlpha = true;
				}
			}

			if (label.length() == 0) {
				label = action;
			}

			if (isKeymap()) {
				btn.setOnLongClickListener(this);
			}

			updateButtonText();
		}

		public void setControlDown(boolean set)
		{
			CmdModifier mod = CmdModifier.None;

			if (isAlpha && set) {
				mod = CmdModifier.Control;
			}

			if (modifier != mod) {
				modifier = mod;
				updateButtonText();
			}
		}

		public void setShift(boolean set) {
			CmdModifier mod = CmdModifier.None;

			if (isAlpha && set) {
				mod = CmdModifier.Shift;
			}

			if (modifier != mod) {
				modifier = mod;
				updateButtonText();
			}
		}

		public boolean isGameCommand()
		{
			return type == CmdType.GameCommand;
		}

		public char getCommand(boolean translate)
		{
			if (isGameCommand()) {
				char cmd = action.charAt(0);

				if (translate) {
					// Original keyset
					CoreCommand core = state.findCommand(cmd, 0);
					if (core != null) {
						// Convert, or not
						cmd = (char)core.getKey();
					}
				}

				return cmd;
			}
			return 0;
		}

		public char fixIcon(char p_chr)
		{
			char raw = (p_chr > 0) ? p_chr: getCommand(false);
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
				btn.setTooltipText(InputUtils.printableChar(cmd));
			}
		}

		public void convertToIcon(char p_chr)
		{
			btn.setTypeface(ButtonRibbon.fontCmd);
			//setIconTooltip();
			btn.setOnLongClickListener(this);
			String txt = Character.toString(fixIcon(p_chr));
			btn.setText(txt);
		}

		public String getAction()
		{
			if (isAlpha && modifier == CmdModifier.Shift) {
				if (Character.isLowerCase(charValue)) {
					return "" + Character.toUpperCase(charValue);
				}
				else {
					return "" + Character.toLowerCase(charValue);
				}
			}
			if (isAlpha && modifier == CmdModifier.Control) {
				return "^" + Character.toUpperCase(charValue);
			}
			return action;
		}

		public void updateButtonText()
		{
			String txt = label;

			// Set icon
			if (isGameCommand()) {
				if (Preferences.getIconsEnabled() && ButtonRibbon.fontCmd != null) {
					convertToIcon((char)0);
					// Done!
					return;
				}
				else {
					txt = InputUtils.printableChar(getCommand(true));
					// Keep going
				}
			}
			else if (isAlpha) {
				txt = getAction();
			}

			if (action.toUpperCase().equals("RUN")) {
				convertToIcon('.');
				return;
			}
			else if (action.toUpperCase().equals("OPACITY")) {
				convertToIcon('l');
				return;
			}

			// Set bold
			if (txt.length() == 1) {
				btn.setTypeface(context.monoBoldFont);
				/*
				SpannableString spanString = new SpannableString(txt);
				spanString.setSpan(new StyleSpan(Typeface.BOLD),
						0, spanString.length(), 0);
				btn.setText(spanString);
				*/
				btn.setText(txt);
			}
			// Normal text
			else {
				btn.setTypeface(context.monoFont);
				btn.setText(String.format("%.3s", txt));
			}
		}

		public boolean isKeymap()
		{
			return type == CmdType.KeyMap;
		}

		@Override
		public boolean onLongClick(View v) {

			// Special case
			if (isKeymap()) {
				context.showKeymapEditor();
				return true;
			}

			char cmd = getCommand(false);
			String desc = "";

			CoreCommand core = state.findCommand(cmd, 0);
			if (core != null) {
				cmd = (char)core.getKey();
				desc = core.desc;
			}

			String txt = InputUtils.printableChar(cmd);

			if (desc == null) desc = "";
			String msg = "Execute command: " + txt + "\n\n" + desc;
			context.questionAlert(msg,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							btn.performClick();
						}
					});

			return true;
		}
	}
}
