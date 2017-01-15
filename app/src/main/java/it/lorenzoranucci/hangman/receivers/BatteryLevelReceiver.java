package it.lorenzoranucci.hangman.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import it.lorenzoranucci.hangman.R;


public class BatteryLevelReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String lowerBattery = context.getString(R.string.lower_battery_string);
        Toast.makeText(context, lowerBattery, Toast.LENGTH_LONG).show();
    }
}
