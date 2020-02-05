package org.rephial.xyangband;

public class AngbandApp extends android.app.Application
{
    @Override public void onCreate() {
		super.onCreate();
		ScorePublisher.Init(this);
	}
 
    @Override public void onTerminate() {
		super.onTerminate();
		ScorePublisher.Destroy();
    }
}
