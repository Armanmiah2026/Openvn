package com.alinaj.dev.json;

import android.os.*;
import android.content.*;
import android.util.Log;

import org.json.*;
import java.io.*;
import java.net.*;
import com.alinaj.dev.utils.Utils.*;

public class JsonManager
{
    public static class ServerDuration extends AsyncTask<String, String, String>
    {

        private String urlString;
        public interface DurationListener
        {
            void onShowDuration(Duration duration);
            void onError(String error);
        }
        public class Duration
        {
            public String premium = "--/--";
            public String vip = "--/--";
            public String priv = "--/--";
        }
        private ServerDuration.DurationListener Listener;
        private String user;
        private String pass;

        public ServerDuration()
        {
        }
        public void setListener(DurationListener Listener)
        {
            this.Listener = Listener;
        }
        public void setURL(String urlString)
        {
            this.urlString = urlString;
        }
        public void setUserPass(String user, String pass)
        {
            this.user = user;
            this.pass = pass;
        }
        public void start()
        {
            try {
                execute(String.format("http://ffastvpn.cf/mydur.php?username=%s&password=%s", user, pass));
            } catch (Exception e) {

            }
        }
        @Override
        protected String doInBackground(String[] p1)
        {
            try {
                URL url = new URL(p1[0]);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setRequestMethod("GET");
                con.connect();

                InputStream input = con.getInputStream();
                StringBuilder sb = new StringBuilder();
                Reader reader = new BufferedReader(new InputStreamReader(input));
                char[] buff = new char[1024];
                while (true) {
                    int read = reader.read(buff, 0, buff.length);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(buff, 0, read);
                }
                return sb.toString();
            } catch (Exception e) {
                return "error: " + e.getMessage();
            }
            // TODO: Implement this method
            //return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            try {
                if (!result.startsWith("error")) {
                    JSONObject obj = new JSONObject(result);
                    Duration duration = new Duration();
                    duration.premium = getDuration(Integer.parseInt(obj.getString("premium")));
                    duration.vip = getDuration(Integer.parseInt(obj.getString("vip")));
                    duration.priv = getDuration(Integer.parseInt(obj.getString("private")));
                    Listener.onShowDuration(duration);
                } else {
                    Listener.onError(result);
                }
            } catch (Exception e) {
                Listener.onError("Error: " + e.getMessage());
            }
            // TODO: Implement this method
            super.onPostExecute(result);
        }
        public String getDuration(long diff)
        {
            long different = diff * 1000;
            long secondsInMilli = 1000 ;
            long minutesInMilli = secondsInMilli * 60 ;
            long hoursInMilli = minutesInMilli * 60 ;
            long daysInMilli = hoursInMilli * 24 ;
            long elapsedDays = different / daysInMilli ;
            different = different % daysInMilli ;
            long elapsedHours = different / hoursInMilli;
            different = different % hoursInMilli;
            long elapsedMinutes = different / minutesInMilli ;
            //different = different % minutesInMilli ;
            //long elapsedSeconds = different / secondsInMilli ;
            return String.format("%d days,%d hours,%d minutes", elapsedDays , elapsedHours, elapsedMinutes);
        }
    }
    public static class AppUpdate extends AsyncTask<String, String, String>
    {

        private String url;
        private JsonManager.AppUpdate.OnAppUpdateListener listener;
        private final Context contx;

        public interface OnAppUpdateListener
        {
            void onAppShowUpdate(String newVersion);
            void onAppNoUpdateAvailable(String oldVersion);
        }
        public AppUpdate(Context contx)
        {
            this.contx = contx;
        }
        public void setURL(String url)
        {
            this.url = url;
        }
        public void setUpdateListener(OnAppUpdateListener OnUpdateListener)
        {
            listener = OnUpdateListener;
        }
        public void start()
        {
            try {
                execute(url);
            } catch (Exception e) {

            }
        }
        @Override
        protected String doInBackground(String[] p1)
        {
            try {
                URL murl = new URL(p1[0]);
                HttpURLConnection con = (HttpURLConnection)murl.openConnection();
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setRequestMethod("GET");
                con.connect();

                StringBuilder sb = new StringBuilder();
                char[] buff = new char[1024];
                Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while (true) {
                    int read = reader.read(buff, 0, buff.length);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(buff, 0, read);
                }
                String str = sb.toString();
                return str;
            } catch (Exception e) {

            }
            // TODO: Implement this method
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            // TODO: Implement this method
            super.onPostExecute(result);
        }
        private boolean versionCompare(String NewVersion, String OldVersion) {
            String[] vals1 = NewVersion.split("\\.");
            String[] vals2 = OldVersion.split("\\.");
            int i = 0;

            // set index to first non-equal ordinal or length of shortest version string
            while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
                i++;
            }
            // compare first non-equal ordinal number
            if (i < vals1.length && i < vals2.length) {
                int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
                return Integer.signum(diff) > 0;
            }

            // the strings are equal or one string is a substring of the other
            // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
            return Integer.signum(vals1.length - vals2.length) > 0;
        }
    }
    public static class ServerUpdate extends AsyncTask<String, String, String>
    {

        private String currentVersion;

        public void setCurrentVersion(String string)
        {
            currentVersion = string;
            // TODO: Implement this method
        }
        public interface OnUpdateListener
        {
            void onShowUpdate(JSONObject obj);
            void onNoUpdateAvailable(String oldVersion);
            void onUpdateError(String error);
        }
        private final Context context;
        private String url;
        private OnUpdateListener listener;
        public ServerUpdate(Context context)
        {
            this.context = context;
        }

        public void setURL(String url)
        {
            this.url = url;
        }
        public void setUpdateListener(OnUpdateListener OnUpdateListener)
        {
            listener = OnUpdateListener;
        }
        public void start()
        {
            try {
                execute(url);
            } catch (Exception e) {

            }
        }
        @Override
        protected String doInBackground(String[] s)
        {
            try {
                URL murl = new URL(s[0]);
                HttpURLConnection con = (HttpURLConnection)murl.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                StringBuilder sb = new StringBuilder();
                char[] buff = new char[1024];
                Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while (true) {
                    int read = reader.read(buff, 0, buff.length);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(buff, 0, read);
                }
                String str = Parser.parseToString(sb.toString().replace(" ", "").replace("\n", ""));
                return str;
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result.startsWith("Error")) {
            listener.onUpdateError(result);
            } else {
                try {
                    File file = new File(context.getFilesDir(), "Servers.js");
                    String oldVersion = currentVersion;
                    JSONObject js = new JSONObject(result);
                    String newVersion = js.getString("Version");
                    
                    if (versionCompare(newVersion, oldVersion)) {
                        OutputStream out = new FileOutputStream(file);
                        out.write(result.getBytes());
                        out.flush();
                        out.close();

                        listener.onShowUpdate(js);
                    } else {
                        listener.onNoUpdateAvailable(oldVersion);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onUpdateError(e.getMessage());
                }
            }
            // TODO: Implement this method
            super.onPostExecute(result);
        }
        private boolean versionCompare(String NewVersion, String OldVersion) {
            String[] vals1 = NewVersion.split("\\.");
            String[] vals2 = OldVersion.split("\\.");
            int i = 0;

            // set index to first non-equal ordinal or length of shortest version string
            while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
                i++;
            }
            // compare first non-equal ordinal number
            if (i < vals1.length && i < vals2.length) {
                int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
                return Integer.signum(diff) > 0;
            }

            // the strings are equal or one string is a substring of the other
            // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
            return Integer.signum(vals1.length - vals2.length) > 0;
        }
    }
}
