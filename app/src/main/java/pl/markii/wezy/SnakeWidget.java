package pl.markii.wezy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.ZoneId;

public class SnakeWidget extends AppWidgetProvider {
    private static final int C_BOA = 0xFFE98A60;
    private static final int C_PYT = 0xFFD9B84E;
    private static final int C_TEXT = 0xFFFFFFFF;
    private static final int C_DUE = 0xFFFF8A80;
    private static final int C_SHED = 0xFF9FB0F0;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) update(c, m, id);
        scheduleMidnight(c);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        for (int id : ids) Store.removeWidget(c, id);
    }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, SnakeWidget.class));
        for (int id : ids) update(c, m, id);
        if (ids.length > 0) scheduleMidnight(c);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        String which = Store.widgetSnake(c, id);
        JSONObject all = Store.snakes(c);
        RemoteViews v;
        if ("boa".equals(which) || "pyton".equals(which)) {
            v = new RemoteViews(c.getPackageName(), R.layout.widget_single);
            fill(c, v, all.optJSONObject(which), which, R.id.name, R.id.days, R.id.sub, R.id.feed);
        } else {
            v = new RemoteViews(c.getPackageName(), R.layout.widget_both);
            fill(c, v, all.optJSONObject("boa"), "boa", R.id.boa_name, R.id.boa_days, R.id.boa_sub, R.id.boa_feed);
            fill(c, v, all.optJSONObject("pyton"), "pyton", R.id.pyt_name, R.id.pyt_days, R.id.pyt_sub, R.id.pyt_feed);
        }
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.root, open);
        m.updateAppWidget(id, v);
    }

    private static void fill(Context c, RemoteViews v, JSONObject s, String key,
                             int nameId, int daysId, int subId, int feedId) {
        boolean boa = "boa".equals(key);
        v.setTextColor(nameId, boa ? C_BOA : C_PYT);
        v.setViewVisibility(feedId, View.GONE);
        if (s == null) {
            v.setTextViewText(nameId, boa ? "Boa" : "Pyton");
            v.setTextViewText(daysId, "—");
            v.setTextColor(daysId, C_TEXT);
            v.setTextViewText(subId, "Otwórz aplikację");
            return;
        }
        String name = Store.str(s, "short");
        v.setTextViewText(nameId, name.isEmpty() ? (boa ? "Boa" : "Pyton") : name);
        String prey = Store.str(s, "prey");
        if (Store.inShed(s)) {
            v.setTextViewText(daysId, "~");
            v.setTextColor(daysId, C_SHED);
            v.setTextViewText(subId, "Wylinka · nie karm");
            return;
        }
        long d = Store.daysLeft(s);
        String days, sub;
        if (d == Store.NO_DATE) { days = "—"; sub = "Ustaw datę karmienia"; }
        else if (d > 0) { days = String.valueOf(d); sub = (d == 1 ? "dzień" : "dni") + " · " + prey; }
        else if (d == 0) { days = "DZIŚ"; sub = prey; }
        else { days = "+" + (-d); sub = "po terminie · " + prey; }
        v.setTextViewText(daysId, days);
        v.setTextColor(daysId, (d != Store.NO_DATE && d <= 0) ? C_DUE : C_TEXT);
        v.setTextViewText(subId, sub);
        if (d != Store.NO_DATE && d <= 0) {
            v.setViewVisibility(feedId, View.VISIBLE);
            v.setOnClickPendingIntent(feedId, FeedReceiver.pending(c, key));
        }
    }

    /** Odśwież widget tuż po północy, żeby licznik dni zmieniał się od razu. */
    static void scheduleMidnight(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, SnakeWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, SnakeWidget.class));
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        long at = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 60_000;
        am.set(AlarmManager.RTC, at, pi);
    }
}
