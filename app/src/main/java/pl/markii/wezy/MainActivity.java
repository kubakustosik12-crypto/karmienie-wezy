package pl.markii.wezy;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    static final String PERM = "android.permission.POST_NOTIFICATIONS";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(0x00000000);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        ReminderReceiver.ensureChannel(this);
        ReminderReceiver.schedule(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED
                && !Store.notifAsked(this)) {
            Store.setNotifAsked(this);
            requestPermissions(new String[]{PERM}, 1);
        }
    }

    private void refreshPage() {
        if (web != null) web.evaluateJavascript("window.onAppResume && window.onAppResume()", null);
    }

    @Override
    protected void onResume() { super.onResume(); refreshPage(); }

    @Override
    protected void onPause() { super.onPause(); SnakeWidget.updateAll(this); }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPage();
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onBack ? window.onBack() : false", new ValueCallback<String>() {
            @Override public void onReceiveValue(String v) { if (!"true".equals(v)) finish(); }
        });
    }

    private boolean notificationsOn() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager nm = getSystemService(NotificationManager.class);
        return nm == null || nm.areNotificationsEnabled();
    }

    class Bridge {
        @JavascriptInterface public String load() { return Store.load(MainActivity.this); }

        @JavascriptInterface public void save(String json) {
            Store.save(MainActivity.this, json);
            SnakeWidget.updateAll(MainActivity.this);
        }

        @JavascriptInterface public int getReminderHour() { return Store.reminderHour(MainActivity.this); }

        @JavascriptInterface public void setReminderHour(int h) {
            Store.setReminderHour(MainActivity.this, h);
            ReminderReceiver.schedule(MainActivity.this);
        }

        @JavascriptInterface public String notifState() { return notificationsOn() ? "granted" : "denied"; }

        @JavascriptInterface public void requestNotif() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED
                            && shouldShowRequestPermissionRationale(PERM)) {
                        requestPermissions(new String[]{PERM}, 1);
                    } else {
                        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(i);
                    }
                }
            });
        }

        @JavascriptInterface public void share(final String text) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, "Kopia – Karmienie węży")
                            .putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, "Zapisz kopię danych"));
                }
            });
        }

        @JavascriptInterface public String version() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "";
            }
        }
    }
}
