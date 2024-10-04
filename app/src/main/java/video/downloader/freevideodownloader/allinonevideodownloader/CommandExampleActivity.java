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
import com.yausername.youtubedl_android.YoutubeDLResponse;

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
    private TextView tvCommandStatus, tvCommandOutput;
    private ProgressBar pbLoading;

    private boolean isCommandRunning = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private static final String TAG = CommandExampleActivity.class.getSimpleName();

    // Progress callback for YoutubeDL download progress
    private final DownloadProgressCallback callback = (progress, etaInSeconds, line) -> runOnUiThread(() -> {
        progressBar.setProgress((int) progress);
        tvCommandStatus.setText(line);
    });

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
        btnRunCommand.setOnClickListener(v -> handleRunCommand());
    }

    private void handleRunCommand() {
        if (isCommandRunning) {
            showToast(getString(R.string.command_in_progress_error));
            return;
        }

        if (!isStoragePermissionGranted()) {
            showToast(getString(R.string.grant_storage_permission_error));
            return;
        }

        String command = etCommand.getText().toString().trim();
        if (TextUtils.isEmpty(command)) {
            etCommand.setError(getString(R.string.command_error));
            return;
        }

        YoutubeDLRequest request = parseCommand(command);
        if (request == null) {
            showToast(getString(R.string.command_parse_error));
            return;
        }

        executeCommand(request);
    }

    private YoutubeDLRequest parseCommand(String command) {
        YoutubeDLRequest request = new YoutubeDLRequest(Collections.emptyList());
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(command);
        matcher.results().forEach(m -> {
            if (m.group(1) != null) {
                request.addOption(m.group(1));
            } else {
                request.addOption(m.group(2));
            }
        });
        return request;
    }

    private void executeCommand(YoutubeDLRequest request) {
        showStart();
        isCommandRunning = true;

        compositeDisposable.add(
            Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback.toString()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCommandSuccess, this::handleCommandError)
        );
    }

    private void handleCommandSuccess(YoutubeDLResponse response) {
        pbLoading.setVisibility(View.GONE);
        progressBar.setProgress(100);
        tvCommandStatus.setText(getString(R.string.command_complete));
        tvCommandOutput.setText(response.getOut());
        showToast(getString(R.string.command_success));
        isCommandRunning = false;
    }

    private void handleCommandError(Throwable throwable) {
        if (BuildConfig.DEBUG) Log.e(TAG, "Command failed", throwable);
        pbLoading.setVisibility(View.GONE);
        tvCommandStatus.setText(getString(R.string.command_failed));
        tvCommandOutput.setText(throwable.getMessage());
        showToast(getString(R.string.command_failure));
        isCommandRunning = false;
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

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
