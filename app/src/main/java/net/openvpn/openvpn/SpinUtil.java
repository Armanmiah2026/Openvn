package net.openvpn.openvpn;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.json.JSONException;
import org.json.JSONObject;

public class SpinUtil {
    public static String[] get_spinner_list(Spinner spin) {
        ArrayAdapter<String> aa = (ArrayAdapter) spin.getAdapter();
        if (aa == null) {
            return null;
        }
        int len = aa.getCount();
        String[] ret = new String[len];
        for (int i = 0; i < len; i++) {
            ret[i] = aa.getItem(i);
        }
        return ret;
    }

    public static int get_spinner_count(Spinner spin) {
        ArrayAdapter<String> aa = (ArrayAdapter) spin.getAdapter();
        if (aa == null) {
            return 0;
        }
        return aa.getCount();
    }

    public static String get_spinner_list_item(Spinner spin, int position) {
        ArrayAdapter<JSONObject> aa = (ArrayAdapter) spin.getAdapter();
        if (aa == null) {
            return null;
        }
        try {
            return aa.getItem(position).getString("Name");
        } catch (Exception e) {
            return "";
        }

    }

    public static String get_spinner_selected_item(Spinner spin) {
        try {
            return ((JSONObject)spin.getSelectedItem()).getString("Name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void set_spinner_selected_item(Spinner spin, String selected_item) {
        if (selected_item != null) {
            String sel = get_spinner_selected_item(spin);
            if (!selected_item.equals(sel)) {
                ArrayAdapter<JSONObject> aa = (ArrayAdapter) spin.getAdapter();
                int len = aa.getCount();
                for (int pos = 0; pos < len; pos++) {
                    JSONObject jo = aa.getItem(pos);
                    String name = "";
                    try {
                        name = jo.getString("Name");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (selected_item.equals(name)) {
                        spin.setSelection(pos);
                    }
                }
            }
        }
    }
    public static void show_spinner(Context context, Spinner spin, String[] content) {
        if (content != null) {
            String[] live_content = get_spinner_list(spin);
            /*if (live_content == null || !Arrays.equals(content, live_content)) {
                Adapter.ServerAdapter aa = new Adapter.ServerAdapter(context, content);
                spin.setAdapter(aa);
            }*/
        }
    }
}
