package pl.markii.wezy;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/** Wybór węża przy dodawaniu widgetu na pulpit. */
public class WidgetConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }

        setContentView(R.layout.activity_config);
        bind(R.id.opt_boa, "boa");
        bind(R.id.opt_pyton, "pyton");
        bind(R.id.opt_all, "all");
    }

    private void bind(int viewId, final String snake) {
        findViewById(viewId).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pick(snake); }
        });
    }

    private void pick(String snake) {
        Store.setWidgetSnake(this, widgetId, snake);
        SnakeWidget.update(this, AppWidgetManager.getInstance(this), widgetId);
        SnakeWidget.scheduleMidnight(this);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        finish();
    }
}
