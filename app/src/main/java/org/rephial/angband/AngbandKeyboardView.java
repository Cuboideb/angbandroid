package org.rephial.angband;

import java.util.List;

import android.content.Context;
import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class AngbandKeyboardView extends KeyboardView
{
	private Context mContext;
	private Paint mPainter;

	private boolean mSymbolic = false;
	private boolean mControl = false;
	private boolean mRunning = false;

	final int TEXT_SIZE = 58;
	final int LABEL_SIZE = 36;

	public AngbandKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mContext = context;

		mPainter = new Paint();
		mPainter.setColor(Color.WHITE);
        mPainter.setAntiAlias(true);
        mPainter.setTextAlign(Paint.Align.CENTER);

        // Make it transparent
        this.getBackground().setAlpha(0);
	}

	public void setSymbolic(boolean on) {
		mSymbolic = on;
	}

	public void setControl(boolean on) {
		mControl = on;
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

	@Override
	public void onDraw(Canvas canvas) {
//		super.onDraw(canvas);

		int alpha = Preferences.getKeyboardOverlap() ? (int)(255 * (Preferences.getKeyboardOpacity() / 100f)) : 255;

		Rect padding = new Rect(4, 4, 4, 4);

		final List<Key> keys = getKeyboard().getKeys();
		for (final Key key: keys) {
			Drawable keyBackground =
					mContext.getResources().getDrawable(R.drawable.keybg_background);
			setState(key, keyBackground);

			// Set the offset
			canvas.translate(key.x, key.y);

			// Draw the key
			keyBackground.setBounds(padding.left,
					padding.top,
					key.width - padding.right,
					key.height - padding.bottom);

			// Transparency
			keyBackground.setAlpha(alpha);

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
				//mPainter.setShadowLayer(0f, 0, 0, 0);

				if (alpha < 255) {
					mPainter.setShadowLayer(10f, 0, 0, Color.CYAN);
					mPainter.setAlpha(alpha);
				}

				// Draw the text
				canvas.drawText(label,
					(key.width - padding.left - padding.right) / 2
							+ padding.left,
					(key.height - padding.top - padding.bottom) / 2
							+ (mPainter.getTextSize() - mPainter.descent()) / 2 + padding.top,
					mPainter);

				// Remove drop shadow
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
