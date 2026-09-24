package com.alinaj.dev.thread;

import android.os.CountDownTimer;

public class RetryingThread {

	protected static CountDownTimer mCountDownTimer;

	public static boolean thVerifyIsRunning = false;

	protected int retryingCount = 0;
	public RetryingThreadListener mListener;
	public interface RetryingThreadListener {
		void onRetrying(int i);
	}

	public RetryingThread(RetryingThreadListener mListener){
		this.mListener = mListener;
	}

	public static void stopVerifyingAccount(){
		if (mCountDownTimer!=null){
			mCountDownTimer.cancel();
			mCountDownTimer = null;
			thVerifyIsRunning = false;
		}
	}
	public void startVerifyingAccount(){
		stopVerifyingAccount();
		retryingCount++;
		mCountDownTimer = new CountDownTimer(20000,1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				thVerifyIsRunning = true;
			}
			@Override
			public void onFinish() {
				thVerifyIsRunning = false;
				if (retryingCount==1){
					mListener.onRetrying(1);
				} else if (retryingCount==2){
					mListener.onRetrying(2);
					retryingCount = 0;
				}
				startVerifyingAccount();
			}
		}.start();
	}



}


