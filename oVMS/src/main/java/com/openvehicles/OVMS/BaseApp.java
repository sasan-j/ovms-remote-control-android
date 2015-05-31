package com.openvehicles.OVMS;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class BaseApp extends Application {
//	private static final String TAG = "BaseApp";
	private static BaseApp sInstance = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Fabric.with(this, new Crashlytics());
		sInstance = this;
	}
	
	public static BaseApp getApp() {
		return sInstance;
	}
	
}
