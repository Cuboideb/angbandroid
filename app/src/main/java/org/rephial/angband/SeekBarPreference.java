package org.rephial.angband;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.util.AttributeSet;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener
{
    private SeekBar mSeekBar;
	private TextView mSummary;

    private int mProgress;   
	private int mMin;
	private int mMax;

	public SeekBarPreference(Context context) {
		this(context, null, android.R.attr.preferenceStyle);
	}

	public SeekBarPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setLayoutResource(R.layout.preferenceseekbar);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyle, 0);
		mMin = a.getInt(R.styleable.SeekBarPreference_min, 0);
		mMax = a.getInt(R.styleable.SeekBarPreference_max, 100);
		mProgress = getPersistedInt(a.getInt(R.styleable.SeekBarPreference_defaultVal, (mMax - mMin) / 2));
		a.recycle();
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		mSummary = (TextView)view.findViewById(android.R.id.summary);
		mSeekBar = (SeekBar)view.findViewById(R.id.seekbar);
		mSeekBar.setProgress(mProgress);
		mSeekBar.setOnSeekBarChangeListener(this);
	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mProgress = progress;
		mSummary.setText(String.format("(%d)", mProgress));
	}

	@Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        mProgress = restoreValue ? getPersistedInt(mProgress) : (Integer)defaultValue;
    }

	// Do nothing
	public void onStartTrackingTouch(SeekBar seekBar) {	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		persistInt(mProgress);
	}

	public int getProgress() {
		// Fake constraints
		float percent = mProgress / 100f;
		float fakeProgress = (mMax - mMin) * percent;
		return Math.round(fakeProgress);
	}
}