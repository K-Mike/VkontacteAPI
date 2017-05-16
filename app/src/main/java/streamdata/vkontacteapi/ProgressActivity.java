package streamdata.vkontacteapi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;


public class ProgressActivity extends Activity {
    private ProgressBar firstBar = null;
    private ProgressBar secondBar = null;
    private MyBroadRequestReceiver receiver;
    private Button myButton;
    private int i = 0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        firstBar = (ProgressBar)findViewById(R.id.firstBar);
        secondBar = (ProgressBar)findViewById(R.id.secondBar);
        myButton = (Button)findViewById(R.id.myButton);
        myButton.setOnClickListener(new ButtonListener());

        IntentFilter filter = new IntentFilter(getString(R.string.ACTION_NAME));
        receiver = new MyBroadRequestReceiver();
        registerReceiver( receiver, filter);
    }
    class ButtonListener implements OnClickListener{
        @Override
        public void onClick(View v) {
//            if (i == 0 || i == 10) {
//                //make the progress bar visible
//                firstBar.setVisibility(View.VISIBLE);
//                firstBar.setMax(150);
//                secondBar.setVisibility(View.VISIBLE);
//            }else if ( i< firstBar.getMax() ) {
//                //Set first progress bar value
//                firstBar.setProgress(i);
//                //Set the second progress bar value
//                firstBar.setSecondaryProgress(i + 10);
//            }else {
//                firstBar.setProgress(0);
//                firstBar.setSecondaryProgress(0);
//                i = 0;
//                firstBar.setVisibility(View.GONE);
//                secondBar.setVisibility(View.GONE);
//            }
//            i = i + 10;
        }
    }

    public class MyBroadRequestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (i == 0 || i == 10) {
                //make the progress bar visible
                firstBar.setVisibility(View.VISIBLE);
                firstBar.setMax(150);
                secondBar.setVisibility(View.VISIBLE);
            }else if ( i< firstBar.getMax() ) {
                //Set first progress bar value
                firstBar.setProgress(i);
                //Set the second progress bar value
                firstBar.setSecondaryProgress(i + 10);
            }else {
                firstBar.setProgress(0);
                firstBar.setSecondaryProgress(0);
                i = 0;
                firstBar.setVisibility(View.GONE);
                secondBar.setVisibility(View.GONE);
            }
            i = i + 10;


        }


    }
}
