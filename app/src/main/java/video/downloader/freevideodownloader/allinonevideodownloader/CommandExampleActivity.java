package video.downloader.freevideodownloader.allinonevideodownloader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import video.downloader.freevideodownloader.allinonevideodownloader.BuildConfig;

public class CommandExampleActivity extends AppCompatActivity {

    private Button btnRunCommand;
    private EditText etCommand;
    private ProgressBar progressBar;
    private TextView tvCommandStatus;
    private TextView tvCommandOutput;
    private ProgressBar pbLoading;

    private boolean running = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // Lambda expression for progress callback
    private final DownloadProgressCallback callback = (progress, etaInSeconds, line) -> runOnUiThread(() -> {
        progressBar.setProgress((int) progress);
        tvCommandStatus.setText(line);
    });

    private static final String TAG = CommandExampleActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_example);

        initViews();
        initListeners();
    }

    private void initViews() {
        btnRunCommand = findViewById(R.id.btn_run_command);
        etCommand = findViewById(R.id.et_command);
        progressBar = findViewById(R.id.progress_bar);
        tvCommandStatus = findViewById(R.id.tv_status);
        pbLoading = findViewById(R.id.pb_status);
        tvCommandOutput = findViewById(R.id.tv_command_output);
    }

    private void initListeners() {
        btnRunCommand.setOnClickListener(v -> runCommand());
    }

    private void runCommand() {
        if (running) {
            Toast.makeText(this, "cannot start command. a command is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

        String command = etCommand.getText().toString().trim();
        if (TextUtils.isEmpty(command)) {
            etCommand.setError(getString(R.string.command_error));
            return;
        }

        YoutubeDLRequest request = new YoutubeDLRequest(Collections.emptyList());
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(command);
        matcher.results().forEach(m -> {
            if (m.group(1) != null) {
                request.addOption(m.group(1));
            } else {
                request.addOption(m.group(2));
            }
        });

        showStart();

        running = true;
        compositeDisposable.add(
            Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback.toString()))
                .subscribeOn(Schedulers.io()) // Changed to `io()` scheduler for better thread management
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> handleSuccess(response), this::handleError)
        );
    }

    private void handleSuccess(YoutubeDLResponse response) {
        pbLoading.setVisibility(View.GONE);
        progressBar.setProgress(100);
        tvCommandStatus.setText(getString(R.string.command_complete));
        tvCommandOutput.setText(response.getOut());
        Toast.makeText(this, "command successful", Toast.LENGTH_LONG).show();
        running = false;
    }

    private void handleError(Throwable e) {
        if (BuildConfig.DEBUG) Log.e(TAG, "command failed", e);
        pbLoading.setVisibility(View.GONE);
        tvCommandStatus.setText(getString(R.string.command_failed));
        tvCommandOutput.setText(e.getMessage());
        Toast.makeText(this, "command failed", Toast.LENGTH_LONG).show();
        running = false;
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void showStart() {
        tvCommandStatus.setText(getString(R.string.command_start));
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
}
