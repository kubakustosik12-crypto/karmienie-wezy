package pl.markii.wezy;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Wspólne dane: aplikacja (WebView), widget i powiadomienia czytają te same SharedPreferences. */
final class Store {
    static final long NO_DATE = Long.MIN_VALUE;

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("wezy", Context.MODE_PRIVATE);
    }

    static synchronized String load(Context c) { return prefs(c).getString("data", ""); }

    static synchronized void save(Context c, String json) { prefs(c).edit().putString("data", json).commit(); }

    static JSONObject snakes(Context c) {
        try {
            String s = load(c);
            return s.isEmpty() ? new JSONObject() : new JSONObject(s);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /* ---- ustawienia ---- */
    static int reminderHour(Context c) { return prefs(c).getInt("remHour", 18); }
    static void setReminderHour(Context c, int h) { prefs(c).edit().putInt("remHour", h).apply(); }
    static boolean notifAsked(Context c) { return prefs(c).getBoolean("notifAsked", false); }
    static void setNotifAsked(Context c) { prefs(c).edit().putBoolean("notifAsked", true).apply(); }

    static void setWidgetSnake(Context c, int widgetId, String snakeId) {
        prefs(c).edit().putString("w_" + widgetId, snakeId).apply();
    }
    static String widgetSnake(Context c, int widgetId) { return prefs(c).getString("w_" + widgetId, "all"); }
    static void removeWidget(Context c, int widgetId) { prefs(c).edit().remove("w_" + widgetId).apply(); }

    /* ---- logika (lustro funkcji z index.html) ---- */
    static String str(JSONObject s, String k) {
        if (s == null || !s.has(k) || s.isNull(k)) return "";
        return s.optString(k, "");
    }

    static boolean inShed(JSONObject s) { return !str(s, "shed").isEmpty(); }

    /** Dni do następnego karmienia (ujemne = po terminie), NO_DATE gdy brak daty. */
    static long daysLeft(JSONObject s) {
        if (s == null) return NO_DATE;
        String last = str(s, "lastFed"), retry = str(s, "retry");
        try {
            LocalDate next;
            if (!retry.isEmpty() && (last.isEmpty() || retry.compareTo(last) > 0)) next = LocalDate.parse(retry);
            else if (!last.isEmpty()) next = LocalDate.parse(last).plusDays(Math.max(1, s.optInt("interval", 7)));
            else return NO_DATE;
            return ChronoUnit.DAYS.between(LocalDate.now(), next);
        } catch (Exception e) {
            return NO_DATE;
        }
    }

    /** Zapisuje karmienie dzisiaj (z widgetu lub powiadomienia). */
    static synchronized boolean markFed(Context c, String id) {
        JSONObject all = snakes(c);
        JSONObject s = all.optJSONObject(id);
        if (s == null) return false;
        String t = LocalDate.now().toString();
        try {
            JSONArray old = s.optJSONArray("log");
            JSONArray log = new JSONArray();
            JSONObject e = new JSONObject();
            e.put("d", t); e.put("t", "fed"); e.put("p", str(s, "prey"));
            log.put(e);
            if (old != null) for (int i = 0; i < old.length() && i < 149; i++) log.put(old.get(i));
            s.put("log", log);
            String last = str(s, "lastFed");
            if (last.isEmpty() || t.compareTo(last) >= 0) s.put("lastFed", t);
            s.put("retry", JSONObject.NULL);
            all.put(id, s);
            save(c, all.toString());
            return true;
        } catch (JSONException ex) {
            return false;
        }
    }
}
