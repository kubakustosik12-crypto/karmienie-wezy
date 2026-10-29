package pl.markii.wezy;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;

/** Codzienne przypomnienie + ponowne planowanie po restarcie telefonu / zmianie czasu. */
public class ReminderReceiver extends BroadcastReceiver {
    static final String ACTION_REMIND = "pl.markii.wezy.REMIND";
    static final String CHANNEL = "karmienie";

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_REMIND.equals(intent.getAction())) notifyDue(c);
        schedule(c);
        SnakeWidget.updateAll(c);
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_REMIND);
        PendingIntent pi = PendingIntent.getBroadcast(c, 2, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        int h = Store.reminderHour(c);
        if (h < 0) { am.cancel(pi); return; }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime at = now.toLocalDate().atTime(h, 0);
        if (!at.isAfter(now)) at = at.plusDays(1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi);
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Karmienie", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Przypomnienia o karmieniu węży");
        nm.createNotificationChannel(ch);
    }

    static int notifId(String id) { return 1000 + (id.hashCode() & 0xffff); }

    static void cancel(Context c, String id) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(notifId(id));
    }

    static void notifyDue(Context c) {
        ensureChannel(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || !nm.areNotificationsEnabled()) return;
        JSONObject all = Store.snakes(c);
        PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Iterator<String> it = all.keys();
        while (it.hasNext()) {
            String id = it.next();
            JSONObject s = all.optJSONObject(id);
            if (s == null || Store.inShed(s)) continue;
            long d = Store.daysLeft(s);
            if (d == Store.NO_DATE || d > 0) continue;
            String name = Store.str(s, "short");
            if (name.isEmpty()) name = id;
            String title = d == 0 ? name + " – dziś karmienie"
                    : name + " – " + (-d) + (d == -1 ? " dzień" : " dni") + " po terminie";
            Notification n = new Notification.Builder(c, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(title)
                    .setContentText("Ofiara: " + Store.str(s, "prey"))
                    .setColor(0xFF3F6B34)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(c, R.drawable.ic_stat), "✓ Nakarmiony",
                            FeedReceiver.pending(c, id)).build())
                    .build();
            nm.notify(notifId(id), n);
        }
    }
}
