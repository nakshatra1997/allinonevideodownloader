package video.downloader.freevideodownloader.allinonevideodownloader;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import video.downloader.freevideodownloader.allinonevideodownloader.R;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //set full screen
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //hide action bar
        getSupportActionBar().hide();

        // Using lambda expression to simplify Runnable
        new Handler().postDelayed(() -> {
            //start the main activity
            startActivity(new Intent(SplashScreen.this, MainActivity.class));
            //finish this activity
            finish();
        }, 4000);
    }
}
