package com.alinaj.dev.psiphon;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2022/06/16
 */

public class Utils {

    private static boolean m_initializedSecureRandom = false;

    public static void initializeSecureRandom() {
        // Installs a new SecureRandom SPI which directly uses /dev/urandom. This addresses a flaw in the default
        // SecureRandom documented here: http://armoredbarista.blogspot.com.au/2013/03/randomly-failed-weaknesses-in-java.html
        // NOTE: this is now the SPI for all versions of Android, including 4.2+ where the flaw was addressed.

        if (!m_initializedSecureRandom) {
            new LinuxSecureRandom();
            m_initializedSecureRandom = true;
        }
    }

    public static boolean isRooted() {
        //Method 1 check for presence of 'test-keys' in the build tags 
        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        //Method 2 check for presence of Superuser app
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return true;
            }
        } catch (Exception e) {
        }

        //Method 3 check for presence of 'su' in the PATH
        String path = null;
        Map<String, String> env = System.getenv();

        if (env != null && (path = env.get("PATH")) != null) {
            String[] dirs = path.split(":");
            for (String dir : dirs) {
                String suPath = dir + "/" + "su";
                File suFile = new File(suPath);
                if (suFile != null && suFile.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getClientPlatformSuffix() {
        String suffix = "";
        if (Utils.isRooted()) {
            suffix += "_rooted";
        }
        suffix += "_playstore";
        return suffix;
    }

    public static String getLocalTimeString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        String dateStr = sdf.format(date);
        return dateStr;
    }
}
