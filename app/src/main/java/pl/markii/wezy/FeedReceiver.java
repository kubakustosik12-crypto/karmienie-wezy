package pl.markii.wezy;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** „✓ Nakarmiony” z widgetu albo z powiadomienia – bez otwierania aplikacji. */
public class FeedReceiver extends BroadcastReceiver {
    static final String EXTRA = "snake";

    static PendingIntent pending(Context c, String id) {
        Intent i = new Intent(c, FeedReceiver.class)
                .setAction("pl.markii.wezy.FEED." + id)
                .putExtra(EXTRA, id);
        return PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String id = intent.getStringExtra(EXTRA);
        if (id == null) return;
        if (Store.markFed(c, id)) {
            SnakeWidget.updateAll(c);
            ReminderReceiver.cancel(c, id);
            String name = Store.str(Store.snakes(c).optJSONObject(id), "short");
            Toast.makeText(c, (name.isEmpty() ? id : name) + ": karmienie zapisane", Toast.LENGTH_SHORT).show();
        }
    }
}
