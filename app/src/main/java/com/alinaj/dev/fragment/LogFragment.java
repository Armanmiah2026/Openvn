package com.alinaj.dev.fragment;

import android.app.*;
import android.view.*;
import android.os.*;
import android.widget.*;
import java.util.*;
import com.alinaj.dev.service.OpenVPNService;
import com.alinaj.dev.service.OpenVPNService.*;
import com.alinaj.dev.adapter.Adapter.*;
import android.content.*;
import com.alinaj.dev.R;

public class LogFragment extends Fragment implements OpenVPNService.EventReceiver
{
	private OpenVPNService mBoundService = null;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((LocalBinder) service).getService();
            mBoundService.client_attach(LogFragment.this);
            post_bind();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

	private View v;
	private ListView logView;
	private ArrayList<OpenVPNService.LogMsg> listLog;
	private LogAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		v = LayoutInflater.from(getActivity()).inflate(R.layout.log_activity, null);
		logView = (ListView)findViewById(R.id.log_list);
		listLog = new ArrayList<LogMsg>();
		adapter = new LogAdapter(getActivity(), listLog);
		logView.setAdapter(adapter);
		//logView.setOnItemLongClickListener(this);
		doBindService();
		// TODO: Implement this method
		return v;
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
					logView.smoothScrollToPosition(listLog.size() -1);
					// TODO: Implement this method
				}


			});
		// TODO: Implement this method

	}

	@Override
	public PendingIntent get_configure_intent(int i)
	{
		// TODO: Implement this method
		return null;
	}

	@Override
	public void event(OpenVPNService.EventMsg eventMsg)
	{
		// TODO: Implement this method
	}

	protected void post_bind()
	{
		ArrayDeque<LogMsg> log_hist = mBoundService.log_history();
		if (log_hist != null) {
			Iterator<LogMsg> it = log_hist.iterator();
			while (it.hasNext()) {
				listLog.add(it.next());
				adapter.notifyDataSetChanged();
			}
		}
		// TODO: Implement this method

	}

	@Override
	public void onDestroy()
	{
		try {
			doUnbindService();
		} catch (Exception e) {

		}
		// TODO: Implement this method
		super.onDestroy();
	}
	protected void doBindService()
	{
        getActivity().bindService(new Intent(getActivity(), OpenVPNService.class).setAction(OpenVPNService.ACTION_BIND), this.mConnection, 65);
    }
	protected void doUnbindService()
	{

        if (this.mBoundService != null) {
            this.mBoundService.client_detach(this);
            getActivity().unbindService(this.mConnection);
            this.mBoundService = null;
        }

    }
	private View findViewById(int id)
	{
		return v.findViewById(id);
	}
}
