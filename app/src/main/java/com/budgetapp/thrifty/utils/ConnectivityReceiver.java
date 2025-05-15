package com.budgetapp.thrifty.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtils.isOnline(context)) {
            OfflineAccountManager.syncUserData(context, null);
        }
    }
}