package com.shilapi.xcertplay.bench;

import android.app.Activity;
import android.app.ActivityManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.shilapi.xcertplay.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Debug-only, identity-free decoder bench. No discovery, protocol or phone data access. */
public final class ReceiverBenchActivity extends Activity implements SurfaceHolder.Callback {
    private static final int EXPECTED_FRAMES = 300;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private TextView output;
    private Button run;
    private Thread worker;
    private boolean resumed;
    private boolean autoRunPending;
    private Surface surface;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        output = new TextView(this);
        output.setText("Local synthetic AVC decoder bench. No CarWith session or G0 result.");
        SurfaceView preview = new SurfaceView(this);
        preview.getHolder().addCallback(this);
        layout.addView(preview, new LinearLayout.LayoutParams(-1, 240));
        run = new Button(this);
        run.setText("Run 300-frame local AVC fixture (unpaced)");
        run.setOnClickListener(v -> startFixture());
        layout.addView(run);
        Button stop = new Button(this);
        stop.setText("Cancel");
        stop.setOnClickListener(v -> cancel());
        layout.addView(stop);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        autoRunPending = getIntent().getBooleanExtra("run_fixture", false);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        maybeAutoRun();
    }
    @Override protected void onPause() {
        resumed = false;
        cancel();
        super.onPause();
    }
    @Override public void surfaceCreated(SurfaceHolder holder) {
        surface = holder.getSurface();
        maybeAutoRun();
    }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        cancel();
        surface = null;
    }
    private void maybeAutoRun() {
        if (resumed && surface != null && autoRunPending) {
            autoRunPending = false;
            startFixture();
        }
    }
    private void cancel() {
        cancelled.set(true);
        if (worker != null) worker.interrupt();
    }
    private void startFixture() {
        if (!resumed || surface == null || !surface.isValid() ||
                (worker != null && worker.isAlive())) return;
        cancelled.set(false);
        run.setEnabled(false);
        output.setText("Decoding local test pattern; 30-second bound. No phone connection.");
        Surface target = surface;
        worker = new Thread(() -> decode(target), "receiver-fixture");
        worker.start();
    }

    private JSONObject probe() throws Exception {
        JSONObject result = new JSONObject();
        result.put("schema_version", 1);
        result.put("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        result.put("android_api", Build.VERSION.SDK_INT);
        result.put("abis", new JSONArray(Build.SUPPORTED_ABIS));
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memory);
        result.put("total_ram_bytes", memory.totalMem);
        result.put("process_java_heap_limit_bytes", Runtime.getRuntime().maxMemory());
        JSONObject features = new JSONObject();
        for (String name : new String[] {"android.hardware.wifi.direct", "android.hardware.bluetooth",
                "android.hardware.bluetooth_le"}) {
            features.put(name, getPackageManager().hasSystemFeature(name));
        }
        result.put("declared_features_not_connectivity_tests", features);
        JSONArray codecs = new JSONArray();
        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if (info.isEncoder()) continue;
            for (String type : info.getSupportedTypes()) {
                if (!type.equalsIgnoreCase("video/avc") && !type.equalsIgnoreCase("video/hevc")) continue;
                JSONObject codec = new JSONObject();
                codec.put("name", info.getName());
                codec.put("mime", type);
                if (Build.VERSION.SDK_INT >= 29) {
                    codec.put("declared_hardware_accelerated", info.isHardwareAccelerated());
                    codec.put("declared_software_only", info.isSoftwareOnly());
                }
                try {
                    codec.put("declared_720p30_supported", info.getCapabilitiesForType(type)
                            .getVideoCapabilities().areSizeAndRateSupported(1280, 720, 30));
                } catch (IllegalArgumentException e) {
                    codec.put("capability_query", "unsupported");
                }
                codecs.put(codec);
            }
        }
        result.put("decoder_capabilities_not_performance_proof", codecs);
        result.put("is_carwith_session", false);
        result.put("is_snapdragon_625_validation", false);
        return result;
    }

    private void decode(Surface target) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        JSONObject report = new JSONObject();
        AtomicInteger callbacks = new AtomicInteger();
        ConcurrentHashMap<Long, Long> queuedAt = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<Double> localLatencyMs = new ConcurrentLinkedQueue<>();
        int inputFrames = 0;
        int outputFrames = 0;
        boolean outputEos = false;
        long started = System.nanoTime();
        long drainFinished = started;
        try {
            report = probe();
            report.put("fixture", "synthetic-avc-1280x720-30fps-300frames-no-audio");
            report.put("mode", "unpaced decode to Surface; throughput is not displayed FPS");
            try (android.content.res.AssetFileDescriptor asset =
                    getResources().openRawResourceFd(R.raw.receiver_fixture)) {
                extractor.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
            }
            MediaFormat format = extractor.getTrackFormat(0);
            if (!"video/avc".equals(format.getString(MediaFormat.KEY_MIME)))
                throw new IllegalArgumentException("Expected AVC fixture");
            extractor.selectTrack(0);
            decoder = MediaCodec.createDecoderByType("video/avc");
            report.put("selected_decoder", decoder.getName());
            decoder.configure(format, target, null, 0);
            decoder.setOnFrameRenderedListener((codec, pts, nanoTime) -> {
                callbacks.incrementAndGet();
                Long queued = queuedAt.remove(pts);
                if (queued != null && nanoTime >= queued) {
                    localLatencyMs.add((nanoTime - queued) / 1_000_000.0);
                }
            }, ui);
            decoder.start();
            boolean inputEos = false;
            long lastPts = 0;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!outputEos && !cancelled.get() &&
                    System.nanoTime() - started < 30_000_000_000L) {
                if (!inputEos) {
                    int index = decoder.dequeueInputBuffer(5_000);
                    if (index >= 0) {
                        ByteBuffer buffer = decoder.getInputBuffer(index);
                        if (buffer == null) throw new IllegalStateException("Missing input buffer");
                        int size = extractor.readSampleData(buffer, 0);
                        if (size < 0) {
                            decoder.queueInputBuffer(index, 0, 0, lastPts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEos = true;
                        } else {
                            lastPts = extractor.getSampleTime();
                            queuedAt.put(lastPts, System.nanoTime());
                            decoder.queueInputBuffer(index, 0, size, lastPts, 0);
                            inputFrames++;
                            extractor.advance();
                        }
                    }
                }
                int index = decoder.dequeueOutputBuffer(info, 5_000);
                if (index >= 0) {
                    outputEos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    boolean frame = (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                            (!outputEos || info.size > 0);
                    if (frame) outputFrames++;
                    decoder.releaseOutputBuffer(index, frame);
                }
            }
            drainFinished = System.nanoTime();
            // Allow delayed/batched framework callbacks. Missing callbacks are not video drops.
            if (!cancelled.get()) Thread.sleep(500);
            report.put("status", cancelled.get() ? "cancelled" :
                    (outputEos && inputFrames == EXPECTED_FRAMES && outputFrames == EXPECTED_FRAMES ?
                            "passed" : "failed"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            put(report, "status", "cancelled");
        } catch (Exception e) {
            put(report, "status", cancelled.get() ? "cancelled" : "failed");
            put(report, "error_class", e.getClass().getSimpleName());
        } finally {
            try { if (decoder != null) decoder.release(); } catch (RuntimeException ignored) {}
            extractor.release();
            put(report, "input_frames", inputFrames);
            put(report, "decoded_output_frames", outputFrames);
            put(report, "output_eos", outputEos);
            put(report, "frame_rendered_callbacks", callbacks.get());
            double seconds = (drainFinished - started) / 1_000_000_000.0;
            put(report, "configure_to_output_eos_seconds", Math.max(0, seconds));
            if (outputEos && seconds > 0) put(report, "unpaced_output_frames_per_second", outputFrames / seconds);
            List<Double> latencies = new ArrayList<>(localLatencyMs);
            Collections.sort(latencies);
            put(report, "queue_to_surface_latency_sample_count", latencies.size());
            if (!latencies.isEmpty()) {
                put(report, "queue_to_surface_latency_p95_ms", latencies.get((int) Math.ceil(latencies.size() * .95) - 1));
            }
            put(report, "latency_scope", "local queue to framework Surface timestamp; not glass-to-glass or touch latency");
            put(report, "completed_at_epoch_ms", System.currentTimeMillis());
            JSONObject finalReport = report;
            ui.post(() -> {
                try {
                    String text = finalReport.toString(2);
                    // Private app storage; only this synthetic bench's report. No raw phone/log dumps.
                    Files.write(new File(getFilesDir(), "receiver-bench.json").toPath(),
                            text.getBytes(StandardCharsets.UTF_8));
                    output.setText(text);
                } catch (Exception e) {
                    output.setText("Report write failed: " + e.getClass().getSimpleName());
                }
                run.setEnabled(true);
            });
        }
    }

    private static void put(JSONObject object, String key, Object value) {
        try { object.put(key, value); } catch (org.json.JSONException ignored) {}
    }
}
