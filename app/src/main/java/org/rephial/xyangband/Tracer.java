package org.rephial.xyangband;

import android.util.Log;

class Tracer
{
	String[] messages = null;
	int next = 0;
	int lastDumped = -1;
	Object lock = null;

	public static Tracer singleton = null;
	static {
		singleton = new Tracer();
	}

	protected Tracer()
	{
		messages = new String[1000];

		lock = new Object();
	}

	public int getNext()
	{
		synchronized (lock) {
			return next++;
		}
	}

	public void trace(String message)
	{
		int idx = getNext();
		if (idx < messages.length) {
			messages[idx] = message;
		}
	}

	public void dump()
	{
		synchronized (lock) {
			for(int i = lastDumped + 1; i < messages.length; i++) {
				String msg = messages[i];
				if (msg != null && msg.length() > 0) {
					lastDumped = i;
					Log.d("Angband", "Trace-" + i + ": " + msg);
				}
			}
		}
	}
}