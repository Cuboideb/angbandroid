package org.rephial.xyangband;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AngbandKeyboardView extends KeyboardView
{
	private GameActivity mContext;
	private Paint mPainter;

	private boolean mSymbolic = false;
	private boolean mControl = false;
	private boolean mRunning = false;
	private boolean mHideSomeKeys = false;
	private boolean mHideAllKeys = false;
	private boolean mOpaqueKeyboard = false;

	public int canvas_width = 0;
	public int canvas_height = 0;

	final int TEXT_SIZE = 52;
	final int LABEL_SIZE = 32;

	public AngbandKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mContext = (GameActivity)context;

		mPainter = new Paint();
		mPainter.setColor(Color.WHITE);
        mPainter.setAntiAlias(true);
        mPainter.setTextAlign(Paint.Align.CENTER);

        // Make it transparent
        this.getBackground().setAlpha(0);

        this.setPreviewEnabled(false);
	}

	public boolean getHideAllKeys() {
		return mHideAllKeys;
	}

	public void setHideAllKeys(boolean on) {
		this.mHideAllKeys = on;
	}

	public boolean getOpaqueKeyboard() {
		return mOpaqueKeyboard;
	}

	public void setOpaqueKeyboard(boolean on) {
		this.mOpaqueKeyboard = on;
	}

	public void setSymbolic(boolean on) {
		mSymbolic = on;
	}

	public void setControl(boolean on) {
		mControl = on;
	}

	public void setHideSomeKeys(boolean on) { mHideSomeKeys = on; }

	public boolean getHideSomeKeys() {
		return mHideSomeKeys;
	}

	public void setRunning(boolean on) {
		mRunning = on;
	}

	public boolean isRunning() {
		return mRunning;
	}

	private String fixCase(CharSequence label) {
		if (isShifted() &&
				label != null &&
				label.length() == 1 &&
				Character.isLowerCase(label.charAt(0))) {
			return label.toString().toUpperCase();
		} else {
			return label.toString();
		}
	}

	/*
	 * This is a terrible hack.  It would probably be better to write
	 * a whole new keyboard implementation than do things like this
	 * but I don't know how.  -AS-
	 */
	private void setState(Key key, Drawable keyBackground) {
		int[] nativeStates = key.getCurrentDrawableState();
		int[] newStates = { 0, 0 };
		int newLength = 0;

		if ((mSymbolic && key.codes[0] == -2) ||
				(mControl && key.codes[0] == -6) ||
				(mRunning && key.codes[0] == -4)) {
			newStates[0] = android.R.attr.state_checkable;
			newStates[1] = android.R.attr.state_checked;
			newLength = 2;
		}

		if (nativeStates.length + newLength > 0) {
			int[] combined = new int[nativeStates.length + newLength];
			if (nativeStates.length > 0) {
				System.arraycopy(nativeStates, 0, combined, 0, nativeStates.length);
			}
			if (newLength > 0) {
				System.arraycopy(newStates, 0, combined, nativeStates.length, newLength);
			}

			keyBackground.setState(combined);
		}
	}

	public boolean keyShouldHide(String label)
	{
		if (label == null || label.length() == 0) {
			return false;
		}

		String[] temp = {"0","1","2","3","4","5","6","7","8","9",
				".","...","⏎","Ctrl^","Sym","RUN"," ","Esc",
				"F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12"};
		List<String> miniKeyboard = new ArrayList<>(Arrays.asList(temp));
		List<String> emptyKeyboard = Arrays.asList(new String[] {"..."});

		List<String> keep_these = null;

		// Remove this if keyboard is not qwerty
		if (this.getKeyboard() != this.mContext.virtualKeyboard.kbLayoutQwerty) {
			miniKeyboard.remove(".");
		}

		if (this.getHideAllKeys()) {
			keep_these = emptyKeyboard;
		}
		else if (this.getHideSomeKeys()) {
			keep_these = miniKeyboard;
		}

		if (!label.equals(" ")) {
			label = label.trim();
		}

		if (keep_these != null && !keep_these.contains(label)) {
			return true;
		}

		return false;
	}

	public String getLabelFromKeyCode(int keycode) {
		String label = "";
		List<Keyboard.Key> lst = this.getKeyboard().getKeys();
		for (Keyboard.Key key: lst) {
			if (key.label != null && key.codes.length > 0) {
				int i;
				for (i = 0; i < key.codes.length; i++) {
					if (key.codes[i] == keycode) {
						return key.label.toString();
					}
				}
			}
		}
		return "";
	}

	public void toggleHideSomeKeys()
	{
		boolean shouldHide = !this.getHideSomeKeys();
		if (this.getOpaqueKeyboard() || this.getHideAllKeys()) {
			shouldHide = false;
		}
		this.requestHideSomeKeys(shouldHide);
	}

	public void requestHideSomeKeys(boolean on)
	{
		this.setOpaqueKeyboard(false);
		this.setHideAllKeys(false);
		this.setHideSomeKeys(on);
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		this.canvas_width = w;
		this.canvas_height = h;
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected boolean onLongPress(Key popupKey) {
		if (popupKey.label == null) {
			super.onLongPress(popupKey);
		}

		String label = popupKey.label.toString();

		// Hide almost all keys
		if (label.equals("...")) {
			boolean shouldActivate = !this.getHideAllKeys();
			this.setOpaqueKeyboard(false);
			this.setHideSomeKeys(false);
			this.setHideAllKeys(shouldActivate);
			return true;
		}
		// Show opaque keyboard
		if (label.equals("Ctrl^")) {
			boolean shouldActivate = !this.getOpaqueKeyboard();
			this.setHideSomeKeys(false);
			this.setHideAllKeys(false);
			this.setOpaqueKeyboard(shouldActivate);
			return true;
		}

		// Non repeatable (exclude Return)
		if (!popupKey.repeatable && !label.equals("⏎")) {
			HashMap<String, Integer> map = new HashMap<>();
			// Some common key combinations
			map.put("p", 16); // Ctrl^P (recall messages)
			map.put("f", 6);  // Ctrl^F (recall level feeling)
			map.put("o", 15); // Ctrl^O (recall last message)
			map.put("u", 85); // "U" (use item)
			map.put("q", 57344); // ESCAPE
			map.put("~", 57344); // ESCAPE

			if (map.containsKey(label)) {
				mContext.getStateManager().addKey(map.get(label).intValue());
				return true;
			}

			// Hide some keys
			this.requestHideSomeKeys(true);
			return true;
		}

		return super.onLongPress(popupKey);
	}

	@Override
	public void onDraw(Canvas canvas) {
//		super.onDraw(canvas);

		float pad_pct = 0.33f;

		float alpha_reduction = Preferences.getMiddleOpacity() / 100.0f;
		if (alpha_reduction < 0.0f) alpha_reduction = 0.0f;
		if (alpha_reduction > 1.0f) alpha_reduction = 1.0f;

		boolean overlap = Preferences.getKeyboardOverlap();
		int alpha_pref = overlap ? (int)(255 * (Preferences.getKeyboardOpacity() / 100f)) : 255;

		Rect padding = new Rect(4, 4, 4, 4);

		final List<Key> keys = getKeyboard().getKeys();
		for (final Key key: keys) {
			Drawable keyBackground;
			// Special color for spacebar
			if (key.label != null && key.label.equals(" ")) {
				keyBackground = mContext.getResources().getDrawable(R.drawable.keybg_bright);
			}
			else {
				keyBackground = mContext.getResources().getDrawable(R.drawable.keybg_background);
			}
			setState(key, keyBackground);

			int midx = key.x + key.width / 2;
			float pct = (float)midx / this.canvas_width;
			boolean at_center = false;
			if (pct > pad_pct && pct < (1.0f - pad_pct)) {
				at_center = true;
			}

			// Set the offset
			canvas.translate(key.x, key.y);

			// Draw the key
			keyBackground.setBounds(padding.left,
					padding.top,
					key.width - padding.right,
					key.height - padding.bottom);

			int alpha_fore = alpha_pref;
			int alpha_back = 80;

			// Default
			if (!overlap) {
				alpha_fore = 255;
				alpha_back = 255;
			}
			// See the keyboard
			else if (this.getOpaqueKeyboard()) {
				// Almost opaque
				alpha_fore = 250;
				alpha_back = 200;
			}
			// Hide some keys
			else if (keyShouldHide(key.label.toString())) {
				alpha_fore = 10;
				alpha_back = 30;
			}
			// Make the middle more transparent?
			else if (at_center && alpha_reduction < 1.0f) {
				// Apply
				alpha_fore = (int)(alpha_fore * alpha_reduction);
				// Halve background opacity
				alpha_back = (int) (alpha_back * 0.5f);
			}

			if (alpha_fore < 0) alpha_fore = 0;
			if (alpha_fore > 255) alpha_fore = 255;

			// Draw background
			// Apply transparency and reset
			keyBackground.setAlpha(alpha_back);
			keyBackground.draw(canvas);
			keyBackground.setAlpha(255);

			if (key.label != null) {
				String label = fixCase(key.label);

				// For characters, use large font. For labels like "Done", use small font.
				if (label.length() > 1) {
					mPainter.setTextSize(LABEL_SIZE);
					mPainter.setTypeface(Typeface.DEFAULT_BOLD);
					//mPainter.setTypeface(Typeface.DEFAULT);
				} else {
					mPainter.setTextSize(TEXT_SIZE);
					mPainter.setTypeface(Typeface.DEFAULT_BOLD);
					//mPainter.setTypeface(Typeface.DEFAULT);
				}

				// Draw a drop shadow for the text
				// Use transparency if needed
				if (overlap) {
					mPainter.setShadowLayer(10f, 0, 0, Color.CYAN);
					mPainter.setAlpha(alpha_fore);
				}

				// Draw the text
				canvas.drawText(label,
					(key.width - padding.left - padding.right) / 2
							+ padding.left,
					(key.height - padding.top - padding.bottom) / 2
							+ (mPainter.getTextSize() - mPainter.descent()) / 2 + padding.top,
					mPainter);

				// Remove drop shadow and alpha
				mPainter.setShadowLayer(0, 0, 0, 0);
				mPainter.setAlpha(255);
			} else if (key.icon != null) {
				final int drawableX = (key.width - padding.left - padding.right
								- key.icon.getIntrinsicWidth()) / 2 + padding.left;
				final int drawableY = (key.height - padding.top - padding.bottom
						- key.icon.getIntrinsicHeight()) / 2 + padding.top;
				canvas.translate(drawableX, drawableY);
				key.icon.setBounds(0, 0,
						key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
				key.icon.draw(canvas);
				canvas.translate(-drawableX, -drawableY);
			}

			// Reset translation
			canvas.translate(-key.x, -key.y);
		}
	}

}
