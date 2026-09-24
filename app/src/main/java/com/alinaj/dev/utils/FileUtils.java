package com.alinaj.dev.utils;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileUtils {

    public static String readFromRaw(Context context, int resId) {
        InputStream in = context.getResources().openRawResource(resId);
        Scanner scanner = new Scanner(in, StandardCharsets.UTF_8).useDelimiter("\\A");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.next());
        }
        scanner.close();
        return sb.toString();
    }

    public static boolean saveTextFile(File file, String contents) {
        try {

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter writer = new FileWriter(file, false);
            writer.write(contents);

            writer.close();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
