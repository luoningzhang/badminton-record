package com.example.badmintonlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int PICK_BACKUP = 4011;
    private static final int SAVE_FILE = 4012;
    private static final String PREFS = "badminton_native_storage";
    private static final String DATA_KEY = "tracker_json";
    private static final String PRE_IMPORT_BACKUP_KEY = "tracker_json_pre_import";

    private WebView webView;
    private SharedPreferences prefs;
    private String pendingFileName;
    private String pendingMime;
    private String pendingContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(244, 247, 246));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setTextZoom(100);
        s.setDefaultTextEncodingName("UTF-8");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri != null && "file".equalsIgnoreCase(uri.getScheme())) return false;
                return true;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return url != null && !url.startsWith("file:///android_asset/");
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AndroidBridge {
        @JavascriptInterface
        public String loadData() {
            return prefs.getString(DATA_KEY, "");
        }

        @JavascriptInterface
        public void saveData(String json) {
            prefs.edit().putString(DATA_KEY, json == null ? "" : json).apply();
        }

        @JavascriptInterface
        public void saveFile(String name, String mime, String content) {
            runOnUiThread(() -> saveFileCompat(name, mime, content));
        }

        @JavascriptInterface
        public void pickBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "application/json",
                        "text/json",
                        "text/plain",
                        "application/octet-stream"
                });
                startActivityForResult(intent, PICK_BACKUP);
            });
        }
    }

    private void saveFileCompat(String name, String mime, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloads(name, mime, content);
            return;
        }

        pendingFileName = name;
        pendingMime = mime;
        pendingContent = content;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, SAVE_FILE);
    }

    private void saveToDownloads(String name, String mime, String content) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BadmintonLog");

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("无法创建文件");

            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new Exception("无法打开文件");
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }

            Toast.makeText(this, "已保存到 Downloads/BadmintonLog", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writePickedFile(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception("无法打开文件");
            out.write((pendingContent == null ? "" : pendingContent).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "文件已保存", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pendingFileName = null;
            pendingMime = null;
            pendingContent = null;
        }
    }

    private String readText(Uri uri) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("无法读取文件");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        String text = sb.toString().trim();
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        return text;
    }

    private void importBackup(Uri uri) {
        try {
            final String importedText = readText(uri);

            if (importedText.isEmpty()) {
                throw new Exception("文件为空");
            }

            new JSONObject(importedText);

            new AlertDialog.Builder(this)
                    .setTitle("导入备份")
                    .setMessage("导入会覆盖当前 App 内的数据。建议确认已经备份当前数据。是否继续？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("导入", (dialog, which) -> {
                        String current = prefs.getString(DATA_KEY, "");
                        prefs.edit()
                                .putString(PRE_IMPORT_BACKUP_KEY, current == null ? "" : current)
                                .putString(DATA_KEY, importedText)
                                .commit();

                        Toast.makeText(this, "导入成功，正在重新加载", Toast.LENGTH_SHORT).show();
                        webView.reload();
                    })
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if (requestCode == SAVE_FILE) {
            if (resultCode == RESULT_OK && intentData != null && intentData.getData() != null) {
                writePickedFile(intentData.getData());
            }
            return;
        }

        if (requestCode == PICK_BACKUP) {
            if (resultCode == RESULT_OK && intentData != null && intentData.getData() != null) {
                importBackup(intentData.getData());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }

        webView.evaluateJavascript("window.androidBack ? window.androidBack() : false", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }
}
