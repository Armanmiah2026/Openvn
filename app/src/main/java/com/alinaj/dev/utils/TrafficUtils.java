package com.alinaj.dev.utils;

import android.net.TrafficStats;

import java.util.ArrayList;
import java.util.List;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2024/05/11
 */

public class TrafficUtils {

    private static long totalUpload = 0;
    private static long totalDownload = 0;

    public static List<Long> getData() {

        List<Long> allData = new ArrayList<>();

        long newTotalDownload, incDownload, newTotalUpload, incUpload;

        if (totalDownload == 0) {
            totalDownload = TrafficStats.getTotalRxBytes();
        }

        if (totalUpload == 0) {
            totalUpload = TrafficStats.getTotalTxBytes();
        }

        newTotalDownload = TrafficStats.getTotalRxBytes();
        incDownload = newTotalDownload - totalDownload;

        newTotalUpload = TrafficStats.getTotalTxBytes();
        incUpload = newTotalUpload - totalUpload;

        totalDownload = newTotalDownload;
        totalUpload = newTotalUpload;

        allData.add(incDownload);
        allData.add(incUpload);

        return allData;
    }
}

