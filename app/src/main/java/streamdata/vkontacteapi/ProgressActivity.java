package streamdata.vkontacteapi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


public class ProgressActivity extends Activity {
    private ProgressBar firstBar = null;
    private ProgressBar secondBar = null;
    private MyBroadRequestReceiver receiver;
    private TextView textView = null;
    private int i = 0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        firstBar = (ProgressBar)findViewById(R.id.firstBar);
        secondBar = (ProgressBar)findViewById(R.id.secondBar);
        textView =(TextView)findViewById(R.id.progressText);

        IntentFilter filter = new IntentFilter(getString(R.string.ACTION_NAME));
        receiver = new MyBroadRequestReceiver();
        registerReceiver( receiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public class MyBroadRequestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int maxValue = 100;
            int step = 10;

            if (i == 0 || i == step) {

                textView.setText("Downloading");
                //make the progress bar visible
                firstBar.setVisibility(View.VISIBLE);
                firstBar.setMax(maxValue);
                secondBar.setVisibility(View.VISIBLE);
            }else if ( i < firstBar.getMax() ) {
                //Set first progress bar value
                firstBar.setProgress(i);
                //Set the second progress bar value
                firstBar.setSecondaryProgress(i + step);
            }else {
                firstBar.setProgress(maxValue);
                firstBar.setSecondaryProgress(maxValue);

                firstBar.setVisibility(View.GONE);
                secondBar.setVisibility(View.GONE);
                textView.setText("Mission completed");
            }
            i = i + step;
        }
    }
}
