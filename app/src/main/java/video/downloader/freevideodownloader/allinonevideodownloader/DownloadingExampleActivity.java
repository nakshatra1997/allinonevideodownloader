public class DownloadingExampleActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnStartDownload;
    private EditText etUrl;
    private ProgressBar progressBar, pbLoading;
    private TextView tvDownloadStatus, tvCommandOutput;
    private boolean downloading = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private AdView adView;
    private static InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloading_example);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Download");

        initViews();
        initListeners();
        initAds();
        loadInterstitialAd();
    }

    private void initViews() {
        btnStartDownload = findViewById(R.id.btn_start_download);
        etUrl = findViewById(R.id.et_url);
        progressBar = findViewById(R.id.progress_bar);
        tvDownloadStatus = findViewById(R.id.tv_status);
        pbLoading = findViewById(R.id.pb_status);
        tvCommandOutput = findViewById(R.id.tv_command_output);
    }

    private void initListeners() {
        btnStartDownload.setOnClickListener(this);
    }

    private void initAds() {
        adView = new AdView(this, "YOUR_BANNER_AD_ID", AdSize.BANNER_HEIGHT_50);
        LinearLayout adContainer = findViewById(R.id.banner_container);
        adContainer.addView(adView);
        adView.loadAd();
    }

    private void loadInterstitialAd() {
        interstitialAd = new InterstitialAd(this, "YOUR_INTERSTITIAL_AD_ID");
        interstitialAd.loadAd(interstitialAd.buildLoadAdConfig()
                .withAdListener(new InterstitialAdListener() {
                    @Override
                    public void onInterstitialDisplayed(Ad ad) {
                        Log.d(TAG, "Interstitial ad displayed.");
                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        Log.d(TAG, "Interstitial ad dismissed.");
                    }

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + adError.getErrorMessage());
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        Log.d(TAG, "Interstitial ad is ready to be displayed.");
                    }

                    @Override
                    public void onAdClicked(Ad ad) {
                        Log.d(TAG, "Interstitial ad clicked.");
                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {
                        Log.d(TAG, "Interstitial ad impression logged.");
                    }
                }).build());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_start_download) {
            startDownload();
        }
    }

    private void startDownload() {
        if (downloading) {
            showToastError("Cannot start download. A download is already in progress.");
            return;
        }

        if (!isStoragePermissionGranted()) {
            showToastError("Grant storage permission and retry.");
            return;
        }

        String url = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            etUrl.setError(getString(R.string.url_error));
            return;
        }

        YoutubeDLRequest request = createYoutubeDLRequest(url);
        startDownloadRequest(request);
    }

    private YoutubeDLRequest createYoutubeDLRequest(String url) {
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File downloadDir = getDownloadLocation();
        request.addOption("--no-mtime");
        request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
        request.addOption("-o", downloadDir.getAbsolutePath() + "/%(title)s.%(ext)s");
        return request;
    }

    private void startDownloadRequest(YoutubeDLRequest request) {
        showStart();
        downloading = true;

        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, null, (progress, size, status) -> {
            runOnUiThread(() -> {
                progressBar.setProgress(Math.round(progress));
                tvDownloadStatus.setText(status);
            });
            return Unit.INSTANCE;
        }))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(response -> handleDownloadSuccess(), this::handleDownloadError);

        compositeDisposable.add(disposable);
    }

    private void handleDownloadSuccess() {
        progressBar.setProgress(100);
        tvDownloadStatus.setText(getString(R.string.download_complete));
        pbLoading.setVisibility(View.GONE);
        showToastSuccess("Download successful");
        showInterstitialAd();
        downloading = false;
    }

    private void handleDownloadError(Throwable throwable) {
        Log.e(TAG, "Failed to download", throwable);
        pbLoading.setVisibility(View.GONE);
        tvDownloadStatus.setText(getString(R.string.download_failed));
        tvCommandOutput.setText(throwable.getMessage());
        showToastError("Download failed");
        downloading = false;
    }

    private void showToastSuccess(String message) {
        Toasty.success(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToastError(String message) {
        Toasty.error(this, message, Toast.LENGTH_LONG).show();
    }

    private void showInterstitialAd() {
        if (interstitialAd != null && interstitialAd.isAdLoaded()) {
            interstitialAd.show();
        }
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File downloadFolder = new File(downloadsDir, "video-downloads");
        if (!downloadFolder.exists()) downloadFolder.mkdir();
        return downloadFolder;
    }

    public boolean isStoragePermissionGranted() {
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

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}
