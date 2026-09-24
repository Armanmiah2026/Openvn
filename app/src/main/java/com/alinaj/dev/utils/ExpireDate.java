package com.alinaj.dev.utils;
import android.os.*;
import java.net.*;
import java.io.*;
import org.json.*;
//import com.squareup.okhttp.*;

public class ExpireDate extends AsyncTask<String, String, String>
{

	private ExpireDate.ExpireDateListener listener;

	private String urlString;
	public interface ExpireDateListener
	{
		void onExpireDate(String expire_date);
		void onDeviceNotMatch(String message);
		void onAuthFailed(String message);
		void onError(String error);
	}
	public ExpireDate()
	{
		super();
	}
	public void setUrl(String urlString)
	{
		this.urlString = urlString;
	}
	public void setExpireDateListener(ExpireDateListener ExpireDateListener)
	{
		listener = ExpireDateListener;
	}
	public void start()
	{
		execute(urlString);
	}
	@Override
	protected String doInBackground(String[] p1)
	{
		try {
			StringBuilder sb = new StringBuilder();
			String url = p1[0];
			URL mURL = new URL(url);
			HttpURLConnection con = (HttpURLConnection)mURL.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(10000);
			con.setReadTimeout(10000);
			con.connect();

			InputStream input = con.getInputStream();
			Reader reader = new BufferedReader(new InputStreamReader(input));
			char[] buf = new char[1024];
			while (true) {
				int read = reader.read(buf);
				if (read <= 0) {
					break;
				}
				sb.append(buf, 0, read);
			}
			return sb.toString();
		} catch (Exception e) {
			return "error " + e.getMessage();
		}

		/*try {
		 OkHttpClient client = new OkHttpClient();
		 Request request = new Request.Builder()
		 .url(p1[0])
		 .build();

		 Response response = client.newCall(request).execute();
		 String str = response.body().string();
		 return str;
		 } catch (Exception e) {
		 return "error " + e.getMessage();
		 }*/
		// TODO: Implement this method
	}

	@Override
	protected void onPreExecute()
	{
		// TODO: Implement this method
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(String result)
	{
		// TODO: Implement this method
		super.onPostExecute(result);
		if (result != null) {
			if (result.startsWith("error")) {
				listener.onError(result);
			} else {
				try {
					JSONObject js = new JSONObject(result);
					if (js.getString("device_match").equals("none")) {
						listener.onAuthFailed("Authentication Failed");
						return;
					}
					if (js.getString("device_match").equals("false")) {
						listener.onDeviceNotMatch("This pin is use in another device");
						return;
					}
					listener.onExpireDate(js.getString("expiry"));
				} catch (Exception e) {
					listener.onError("Expire Date: "+e.getMessage());
				}
			}
		}
	}
}
