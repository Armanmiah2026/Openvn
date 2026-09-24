package com.alinaj.dev;

import android.app.*;
import android.view.*;
import com.alinaj.dev.R;

import android.widget.*;

import com.alinaj.dev.activities.*;


public class ServerSelectDialog
{
	private final ListView mListView;
	private final View v;
	private final OpenVPNClient context;
	private final AlertDialog mDialog;

	
	public ServerSelectDialog(final OpenVPNClient context)
	{
		this.context = context;
		v = LayoutInflater.from(context).inflate(R.layout.server_dialog, null);
		mDialog = new AlertDialog.Builder(context).create();
		mDialog.setView(v);

		
		mListView = (ListView)findViewById(R.id.server_list_dialog);
		mListView.setAdapter(context.mServerAdapter);
		mListView.postDelayed(new Runnable() {

				@Override
				public void run()
				{
					mListView.smoothScrollToPosition(context.mServerAdapter.getServerPosition());
					context.mServerAdapter.notifyDataSetChanged();
					// TODO: Implement this method
				}


			}, 500);
		
	}
	public void show()
	{
		mDialog.show();
	}
	private View findViewById(int id)
	{
		// TODO: Implement this method
		return v.findViewById(id);
	}
	public void closeDialog()
	{
		mDialog.dismiss();
	}
}
