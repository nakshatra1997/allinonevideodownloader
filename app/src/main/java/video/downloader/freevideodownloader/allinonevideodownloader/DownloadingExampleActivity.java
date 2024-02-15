package video.downloader.freevideodownloader.allinonevideodownloader;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import video.downloader.freevideodownloader.allinonevideodownloader.R;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;

import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import video.downloader.freevideodownloader.allinonevideodownloader.BuildConfig;


public class DownloadingExampleActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnStartDownload;
    private EditText etUrl;
    private Switch useConfigFile;
    private ProgressBar progressBar;
    private TextView tvDownloadStatus;
    private TextView tvCommandOutput;
    private ProgressBar pbLoading;

    private AdView adView;
    private boolean downloading = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private static InterstitialAd interstitialAd;

    Function3<Float, Long, String, Unit> callback = new Function3<Float, Long, String, kotlin.Unit>() {
        @Override
        public Unit invoke(Float aFloat, Long aLong, String s) {
            int intValue = Math.round(aFloat); // Rounds to the nearest integer
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                      progressBar.setProgress(intValue);
                      tvDownloadStatus.setText(s);
                }
            });
            return null;
        }
    };

    private static final String TAG = DownloadingExampleActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloading_example);

        //back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //change app bar title
        getSupportActionBar().setTitle("Download");

        initViews();
        initListeners();
        initAds();
        initInterAds();
    }

    private void initInterAds(){
//        AdSettings.addTestDevice("7f506ce4-03e2-4410-bf12-17dce0f1d0e3");
        interstitialAd = new InterstitialAd(this, "1049003839494172_1049004882827401");
        InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                // Interstitial ad displayed callback
                Log.e(  TAG, "Interstitial ad displayed.");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                // Interstitial dismissed callback
                Log.e(TAG, "Interstitial ad dismissed.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Ad error callback
                Log.e(TAG, "Interstitial ad failed to load: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Interstitial ad is loaded and ready to be displayed
                Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!");
                // Show the ad
//                if(interstitialAd.isAdLoaded())
//                interstitialAd.show();
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
                Log.d(TAG, "Interstitial ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
                Log.d(TAG, "Interstitial ad impression logged!");
            }
        };

//         For auto play video ads, it's recommended to load the ad
//         at least 30 seconds before it is shown

        interstitialAd.buildLoadAdConfig()
                .withAdListener(interstitialAdListener)
                .build();
        interstitialAd.loadAd();
    }
    private void initAds(){

        adView = new AdView(this, "1049003839494172_1049004579494098", AdSize.BANNER_HEIGHT_50);

// Find the Ad Container
        LinearLayout adContainer = (LinearLayout) findViewById(R.id.banner_container);

// Add the ad view to your activity layout
        adContainer.addView(adView);

// Request an ad
        adView.loadAd();
    }

    private void initViews() {
        btnStartDownload = findViewById(R.id.btn_start_download);
        etUrl = findViewById(R.id.et_url);
//        useConfigFile = findViewById(R.id.use_config_file);
        progressBar = findViewById(R.id.progress_bar);
        tvDownloadStatus = findViewById(R.id.tv_status);
        pbLoading = findViewById(R.id.pb_status);
        tvCommandOutput = findViewById(R.id.tv_command_output);
    }

    private void initListeners() {
        btnStartDownload.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_start_download) {
            startDownload();
        }
    }

    private void startDownload() {
        initInterAds();
        if (downloading) {
            //Toast.makeText(DownloadingExampleActivity.this, "cannot start download. a download is already in progress", Toast.LENGTH_LONG).show();
            Toasty.error(DownloadingExampleActivity.this, "Cannot start download. a download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            //Toast.makeText(DownloadingExampleActivity.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            Toasty.success(DownloadingExampleActivity.this, "Grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

        String url = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }

        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File youtubeDLDir = getDownloadLocation();
        File config = new File(youtubeDLDir, "config.txt");

//        if (useConfigFile.isChecked() && config.exists()) {
//            request.addOption("--config-location", config.getAbsolutePath());
//        } else {
            request.addOption("--no-mtime");
            request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
            request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
//        }

        showStart();

        downloading = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, (String) null,  callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {

                    progressBar.setProgress(100);
                    tvDownloadStatus.setText(getString(R.string.download_complete));
                    pbLoading.setVisibility(View.GONE);
//                    tvCommandOutput.setText(youtubeDLResponse.getOut());
                    //Toast.makeText(DownloadingExampleActivity.this, "download successful", Toast.LENGTH_LONG).show();
                    Toasty.success(DownloadingExampleActivity.this, "Download successful", Toast.LENGTH_LONG).show();
                    if(interstitialAd.isAdLoaded())
                        interstitialAd.show();
                    downloading = false;
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e(TAG, "failed to download", e);
                    pbLoading.setVisibility(View.GONE);
                    tvDownloadStatus.setText(getString(R.string.download_failed));
                    tvCommandOutput.setText(e.getMessage());
                    //Toast.makeText(DownloadingExampleActivity.this, "download failed", Toast.LENGTH_LONG).show();
                    Toasty.error(DownloadingExampleActivity.this, "Download failed", Toast.LENGTH_LONG).show();
                    downloading = false;
                });
        compositeDisposable.add(disposable);

    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    //get back on the home screen
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "video-downloads");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }

    private void showStart() {
        tvDownloadStatus.setText(getString(R.string.download_start));
        progressBar.setProgress(0);
        pbLoading.setVisibility(View.VISIBLE);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    private String getClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                return clip.getItemAt(0).getText().toString();
            }
        }
        return "";
    }
}
