package it.lorenzoranucci.hangman;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;


public class BatteryLevelReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String locale = context.getResources().getConfiguration().locale.getDisplayName();
        String lowerBattery = context.getString(R.string.lower_battery_string);
        Toast.makeText(context, lowerBattery, Toast.LENGTH_LONG).show();
        Log.e("", lowerBattery);
    }
}
