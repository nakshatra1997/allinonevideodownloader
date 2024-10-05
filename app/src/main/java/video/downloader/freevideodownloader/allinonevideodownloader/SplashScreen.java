package video.downloader.freevideodownloader.allinonevideodownloader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DELAY = 4000; // Delay time in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        setFullScreenMode();
        hideActionBar();
        navigateToMainActivity();
    }

    private void setFullScreenMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Use WindowInsetsController for API 30+ devices
            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars());
            getWindow().getInsetsController().setSystemBarsBehavior(
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            // Use traditional flag for older devices
            getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    private void hideActionBar() {
        // Only hide if ActionBar is present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void navigateToMainActivity() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, MainActivity.class));
            finish(); // Ensure SplashScreen activity is removed from the back stack
        }, SPLASH_DELAY);
    }
}
