package com.alinaj.dev.activities;

import android.os.*;
import com.alinaj.dev.service.OpenVPNService.*;
import android.widget.*;
import java.util.*;
import com.alinaj.dev.adapter.Adapter.LogAdapter;
import android.widget.AdapterView.*;
import android.view.*;
import android.text.*;
import com.alinaj.dev.R;
public class LogActivity extends OpenVPNClientBase implements OnItemLongClickListener
{

	private ListView logView;

	private ArrayList<LogMsg> listLog;

	private LogAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log_activity);
		
		logView = findViewById(R.id.log_list);
		listLog = new ArrayList<LogMsg>();
		adapter = new LogAdapter(this, listLog);
		logView.setAdapter(adapter);
		logView.setOnItemLongClickListener(this);
		doBindService();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> p1, View p2, int p3, long p4)
	{
		if (listLog.size() > 0) {
			LogMsg lm = listLog.get(p3);
			copy(lm.line + "\n");
			showToast("Log Copied!");
		}
		// TODO: Implement this method
		return true;
	}

	private void copy(String line)
	{
		((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setText(line);
		// TODO: Implement this method
	}
	@Override
	public void log(LogMsg lm)
	{
		listLog.add(lm);
		adapter.notifyDataSetChanged();
		logView.post(new Runnable() {

				@Override
				public void run()
				{
					logView.smoothScrollToPosition(listLog.size());
					// TODO: Implement this method
				}
				
			
		});
		// TODO: Implement this method
		super.log(lm);
	}

	@Override
	protected void post_bind()
	{
		ArrayDeque<LogMsg> log_hist = log_history();
		if (log_hist != null) {
			Iterator<LogMsg> it = log_hist.iterator();
			while (it.hasNext()) {
				listLog.add(it.next());
				adapter.notifyDataSetChanged();
			}
		}
		// TODO: Implement this method
		super.post_bind();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.log_menu, menu);
		// TODO: Implement this method
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        int itemId = item.getItemId();
        if (itemId == R.id.menu_copy) {
            copy(getFullLog());
            showToast("Log Copied!");
        } else if (itemId == R.id.menu_delete) {
            deleteLogs();
        }
		// TODO: Implement this method
		return super.onOptionsItemSelected(item);
	}

	private void deleteLogs()
	{
		if (listLog.size() > 0) {
			listLog.clear();
			ArrayDeque<LogMsg> hist = log_history();
			if (hist != null) {
				hist.clear();
			}
			adapter.notifyDataSetChanged();
			showToast("Log Deleted!");
		}
		// TODO: Implement this method
	}
	public String getFullLog()
	{
		StringBuilder sb = new StringBuilder();
		if (listLog.size() > 0) {
			for (LogMsg lm: listLog) {
				sb.append(lm.line +"\n");
			}
		}
		return sb.toString();
	}
	@Override
	protected void onDestroy()
	{
		doUnbindService();
		// TODO: Implement this method
		super.onDestroy();
	}
}
