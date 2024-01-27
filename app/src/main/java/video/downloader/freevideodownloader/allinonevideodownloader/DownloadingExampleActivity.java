package video.downloader.freevideodownloader.allinonevideodownloader;

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
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import video.downloader.freevideodownloader.allinonevideodownloader.R;
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


    private boolean downloading = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

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
    }

    private void initViews() {
        btnStartDownload = findViewById(R.id.btn_start_download);
        etUrl = findViewById(R.id.et_url);
        useConfigFile = findViewById(R.id.use_config_file);
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

        if (useConfigFile.isChecked() && config.exists()) {
            request.addOption("--config-location", config.getAbsolutePath());
        } else {
            request.addOption("--no-mtime");
            request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
            request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
        }

        showStart();

        downloading = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, (String) null,  callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    pbLoading.setVisibility(View.GONE);
                    progressBar.setProgress(100);
                    tvDownloadStatus.setText(getString(R.string.download_complete));
                    tvCommandOutput.setText(youtubeDLResponse.getOut());
                    //Toast.makeText(DownloadingExampleActivity.this, "download successful", Toast.LENGTH_LONG).show();
                    Toasty.success(DownloadingExampleActivity.this, "Download successful", Toast.LENGTH_LONG).show();
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
        File youtubeDLDir = new File(downloadsDir, "allinone-video-downloader");
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
