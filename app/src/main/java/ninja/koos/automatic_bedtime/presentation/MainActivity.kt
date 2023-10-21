/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package ninja.koos.automatic_bedtime.presentation

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Switch
import android.widget.TimePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.navigation.WearNavigator
import com.google.android.horologist.composables.TimePicker
import ninja.koos.automatic_bedtime.R
import ninja.koos.automatic_bedtime.presentation.theme.AutomaticbedtimeTheme
import receiver.AlarmReceiver
import java.time.LocalTime
import java.util.Calendar
import kotlin.math.log


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }







        


    }
}

@Composable
fun WearApp() {

    val navController = rememberNavController(remember { WearNavigator() })
    NavHost(navController = navController, startDestination = "greeting") {
        composable("greeting") { Greeting(navController) }
        composable("setTime") { SetTime(navController) }
        /*...*/
    }

//    AutomaticbedtimeTheme {
//        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
//         * version of LazyColumn for wear devices with some added features. For more information,
//         * see d.android.com/wear/compose.
//         */
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(MaterialTheme.colors.background),
//            verticalArrangement = Arrangement.Center
//        ) {
//            Greeting(greetingName = greetingName)
//        }
//    }
}

@Composable
fun SetTime(navController: NavController) {
    val context : Context = LocalContext.current

    // Get the last saved time from settings
    val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    val enableTime = sharedPreferences.getString("enableTime", "22:00")
    val localTime = LocalTime.parse(enableTime)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TimePicker(
            showSeconds = false,
            time = java.time.LocalTime.of(localTime.hour, localTime.minute, 0),
            onTimeConfirm = { localtime ->
                Log.i("MainActivity", "Time set to $localtime")
                val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

                // Save the time to the shared preferences
                sharedPreferences.edit().putString("enableTime", localtime.toString()).apply()
                sharedPreferences.edit().putBoolean("onEnabled", true).apply()

                configureAlarm(context, localtime, true)
                Log.i("MainActivity", "Alarm configured to $localtime")

                // Go back to the main screen
                navController.popBackStack()

            }
        )
    }
}


@Composable
fun Greeting( navController: NavController) {

    val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    var enabled = sharedPreferences.getBoolean("onEnabled", false)
    val enableTime = sharedPreferences.getString("enableTime", "22:00")
    val localTime = LocalTime.parse(enableTime)
    Log.i("Configured time", localTime.toString())

    val context : Context = LocalContext.current


    // Create toggle switch
    var checked by remember { mutableStateOf(true) }
    checked = enabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ToggleChip(
            checked = checked,
            onCheckedChange = {
                checked = it
                enabled = it
                Log.i("MainActivity", "Enabled set to $it")

                // If it is being enabled then open the time picker
                if (it) {
                    navController.navigate("setTime")
                } else {
                    sharedPreferences.edit().putBoolean("onEnabled", false).apply()
                    enabled = it;
                    configureAlarm(context, LocalTime.of(0, 0, 0), false)
                }



            },
            label = { Text("Enabled\n$enableTime", maxLines = 2) },
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked),
                    contentDescription = "test"
                )
            }
        )
    }




}


fun configureAlarm(context: Context, time: LocalTime, enabled: Boolean) {
    Log.i("MainActivity", "Configuring alarm")
    // Set up a daily alarm if enabled, else cancel the alarm
    if (enabled) {
        Log.i("MainActivity", "Configuring alarm")
        // Set up the alarm manager to run the service every 5 minutes
        var alarmMgr: AlarmManager;
        var alarmIntent: PendingIntent;

        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        var intent = Intent(context, AlarmReceiver::class.java);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        // Set the alarm to occur daily at the specified time
        var calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, time.hour);
        calendar.set(Calendar.MINUTE, time.minute);
        calendar.set(Calendar.SECOND, 0)

        // Set alarm to fire in 10 seconds
//        calendar.setTimeInMillis(System.currentTimeMillis() + 10000);

        alarmMgr.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);

        // For testing make an alarm that repeats every 10 seconds
//        alarmMgr.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 10000, alarmIntent);

        Log.i("MainActivity", "Alarm set for ${calendar.time}")
    } else {
        Log.i("MainActivity", "Disabling alarm")
        // Cancel the alarm
        var alarmMgr: AlarmManager;
        var alarmIntent: PendingIntent;

        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        var intent = Intent(context, AlarmReceiver::class.java);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        alarmMgr.cancel(alarmIntent);
        Log.i("MainActivity", "Alarm disabled")
    }



    // Set up the alarm manager to run the service every 5 minutes
//    var alarmMgr: AlarmManager;
//    var alarmIntent: PendingIntent;
//
//    alarmMgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
//    var intent = Intent(context, AlarmReceiver::class.java);
//    alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE);
//
//    // Set the alarm to start at 10 seconds from now
//    var calendar: Calendar = Calendar.getInstance()
//    calendar.setTimeInMillis(System.currentTimeMillis());
//    calendar.add(Calendar.SECOND, 10);


// setRepeating() lets you specify a precise custom interval--in this case,
// 1 day
//    alarmMgr.setExact(
//        AlarmManager.RTC, calendar.getTimeInMillis(), alarmIntent);
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}