package com.alinaj.dev.utils;
import android.os.*;

import com.alinaj.dev.activities.*;

public class SpinnerLoadingAsync extends AsyncTask<Void, Void, Boolean>
{

	private SpinnerLoadingAsync.OnSpinnerLoadingListener Listener;

	private final OpenVPNClient context;

	public interface OnSpinnerLoadingListener
	{
		void onSpinnerLoaded();
	}
	public void setSpinnerLoadingListener(OnSpinnerLoadingListener OnSpinnerLoadingListener)
	{
		Listener = OnSpinnerLoadingListener;
	}
	public SpinnerLoadingAsync(OpenVPNClient context)
	{
		this.context = context;
	}
	@Override
	protected Boolean doInBackground(Void[] p1)
	{
        // TODO: Implement this method
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		if (result) {
			Listener.onSpinnerLoaded();
		}
		// TODO: Implement this method
		super.onPostExecute(result);
	}
}
