package receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

class AlarmReceiver: BroadcastReceiver()  {
    override fun onReceive(p0: Context, p1: Intent?) {
        Log.i("AlarmReceiver", "Alarm received.")
        // Check if we have permission to change secure settings
        val result: Int =
            p0.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        if (result == PackageManager.PERMISSION_GRANTED) {
            Log.i("AlarmReceiver", "Permission granted.")
            Settings.Global.putInt(p0.contentResolver, "bedtime_mode", 1);
        } else {
            Log.e("AlarmReceiver", "Permission denied.")
        }



    }
}