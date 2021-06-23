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
import android.widget.SeekBar;
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

public class OpacityPopup extends PopupWindow
		implements View.OnClickListener,
		SeekBar.OnSeekBarChangeListener
{
	public CheckBox ckOpaque = null;

	public SeekBar alphaBar = null;

	public GameActivity context;

	public OpacityPopup(GameActivity p_context)
	{
		super(p_context);

		context = p_context;		

		setFocusable(true);
		setWidth(LayoutParams.WRAP_CONTENT);
		setHeight(LayoutParams.WRAP_CONTENT);

		ViewGroup content = (ViewGroup)context.getLayoutInflater().inflate
			(R.layout.opacity_layout, null);

		setContentView(content);

		alphaBar = content.findViewById(R.id.opacity);

		alphaBar.setProgress(Preferences.getKeyboardOpacity());

		alphaBar.setOnSeekBarChangeListener(this);

		ckOpaque = content.findViewById(R.id.opaque);		
		ckOpaque.setChecked(context.state.opaqueWidgets);
		ckOpaque.setOnClickListener(this);
	}

	public void updateGame()
	{
		Preferences.setKeyboardOpacity(alphaBar.getProgress());

		context.state.opaqueWidgets = ckOpaque.isChecked();

		context.refreshInputWidgets();
	}

	@Override
	public void onClick(View v) {
		updateGame();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, 
		int progress, boolean fromUser) {
		updateGame();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}

	@Override
	public void dismiss()
	{
		super.dismiss();
	}
}