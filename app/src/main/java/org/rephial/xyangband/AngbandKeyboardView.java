package org.rephial.xyangband;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

import org.rephial.xyangband.R;

import java.util.Arrays;
import java.util.List;

public class AngbandKeyboardView extends KeyboardView
{
	private GameActivity mContext;
	private Paint mPainter;

	private boolean mSymbolic = false;
	private boolean mControl = false;
	private boolean mRunning = false;
	private boolean mMiniKeyboard = false;
	private boolean mSemiOpaque = false;
	private boolean mBareMinimum = false;

	public int canvas_width = 0;
	public int canvas_height = 0;

	final int TEXT_SIZE = 58;
	final int LABEL_SIZE = 36;

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

	public boolean isBareMinimum() {
		return mBareMinimum;
	}

	public void setBareMinimum(boolean mBareMinimum) {
		this.mBareMinimum = mBareMinimum;
	}

	public boolean isSemiOpaque() {
		return mSemiOpaque;
	}

	public void setSemiOpaque(boolean mSemiOpaque) {
		this.mSemiOpaque = mSemiOpaque;
	}

	public void setSymbolic(boolean on) {
		mSymbolic = on;
	}

	public void setControl(boolean on) {
		mControl = on;
	}

	public void setMiniKeyboard(boolean on) {
		mMiniKeyboard = on;
	}

	public boolean getMiniKeyboard() {
		return mMiniKeyboard;
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
				(mRunning && key.codes[0] == -4)
				// || (mMiniKeyboard && key.codes[0] == -7000)
			) {
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

		if (label.equals("...")) {
			boolean shouldActivate = !this.isSemiOpaque();
			this.setMiniKeyboard(false);
			this.setBareMinimum(false);
			this.setSemiOpaque(shouldActivate);
			return true;
		}
		if (label.equals("Ctrl^")) {
			boolean shouldActivate = !this.isBareMinimum();
			this.setSemiOpaque(false);
			this.setMiniKeyboard(false);
			this.setBareMinimum(shouldActivate);
			return true;
		}
		if (!popupKey.repeatable) {
			List<String> ctrl = Arrays.asList(new String[] {"p","f"});

			if (popupKey.codes.length > 0 &&
					ctrl.contains(label.toString())) {
				mContext.getStateManager().addKey(94); // Add Ctrl
				mContext.getStateManager().addKey(popupKey.codes[0]);
			}

			// Add escape key
			mContext.getStateManager().addKey(57344);
			return true;
		}
		return super.onLongPress(popupKey);
	}

	@Override
	public void onDraw(Canvas canvas) {
//		super.onDraw(canvas);

		String[] temp = {"0","1","2","3","4","5","6","7","8","9",
				".","...","⏎","Ctrl^","Sym",
				"F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12"};
		List<String> miniKeyboard = Arrays.asList(temp);
		List<String> bareKeyboard = Arrays.asList(new String[] {"..."});
		List<String> keep_these = null;

		if (mBareMinimum) {
			keep_these = bareKeyboard;
		}
		else if (mMiniKeyboard) {
			keep_these = miniKeyboard;
		}

		int min_alpha = 30;
		float alpha_reduction = 0.35f;
		float pad_pct = 0.25f;

		boolean overlap = Preferences.getKeyboardOverlap();
		int alpha_pref = overlap ? (int)(255 * (Preferences.getKeyboardOpacity() / 100f)) : 255;

		Rect padding = new Rect(4, 4, 4, 4);

		final List<Key> keys = getKeyboard().getKeys();
		for (final Key key: keys) {
			Drawable keyBackground =
					mContext.getResources().getDrawable(R.drawable.keybg_background);
			setState(key, keyBackground);

			float pct = (float)key.x / this.canvas_width;
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
			else if (this.mSemiOpaque) {
				// Almost opaque
				alpha_fore = 250;
				alpha_back = 200;
			}
			// Hide some keys
			else if (keep_these != null && key.label != null) {
				String find = key.label.toString().trim();
				// Make almost invisible if not found
				if (!keep_these.contains(find)) {
					alpha_fore = 10;
					alpha_back = 30;
				}
			}
			// Make the middle more transparent?
			else if (at_center && (alpha_fore > min_alpha)
					&& Preferences.getIncreaseMiddleAlpha()) {
				alpha_fore = (int)(alpha_fore * (1.0f - alpha_reduction));
				if (alpha_fore < min_alpha) {
					alpha_fore = min_alpha;
				}
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
				if (label.length() > 2) {
					mPainter.setTextSize(LABEL_SIZE);
					mPainter.setTypeface(Typeface.DEFAULT_BOLD);
				} else {
					mPainter.setTextSize(TEXT_SIZE);
					mPainter.setTypeface(Typeface.DEFAULT);
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
