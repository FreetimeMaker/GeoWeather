package com.freetime.geoweather;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import android.os.Environment;

import androidx.core.content.ContextCompat;

public class AppUpdater {

    public static void checkAndUpdate(Context context, String owner, String repo, String token, String currentVersion) {
        new Thread(() -> {
            try {
                GitHubRelease latest = fetchLatestRelease(owner, repo, token);
                if (latest == null || latest.draft || latest.prerelease) return;

                if (isNewer(latest.tag_name, currentVersion)) {
                    for (GitHubAsset asset : latest.assets) {
                        if (asset.name.toLowerCase().endsWith(".apk")) {
                            long downloadId = enqueueApkDownload(context, asset.browser_download_url);
                            registerInstallOnDownloadComplete(context, downloadId);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    static class GitHubAsset {
        public String name;
        public String browser_download_url;
    }

    static class GitHubRelease {
        public String tag_name;
        public List<GitHubAsset> assets = new ArrayList<>();
        public boolean draft;
        public boolean prerelease;
    }

    private static GitHubRelease fetchLatestRelease(String owner, String repo, String token) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request.Builder builder = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YourAppUpdater");

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        try (Response res = client.newCall(builder.build()).execute()) {
            if (!res.isSuccessful()) return null;
            String body = res.body().string();
            JSONObject o = new JSONObject(body);

            GitHubRelease release = new GitHubRelease();
            release.tag_name = o.getString("tag_name");
            release.draft = o.getBoolean("draft");
            release.prerelease = o.getBoolean("prerelease");

            JSONArray arr = o.optJSONArray("assets");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject a = arr.getJSONObject(i);
                    GitHubAsset asset = new GitHubAsset();
                    asset.name = a.getString("name");
                    asset.browser_download_url = a.getString("browser_download_url");
                    release.assets.add(asset);
                }
            }
            return release;
        }
    }

    private static boolean isNewer(String latestTag, String currentVersion) {
        int[] a = parseSemver(latestTag);
        int[] b = parseSemver(currentVersion);
        if (a[0] != b[0]) return a[0] > b[0];
        if (a[1] != b[1]) return a[1] > b[1];
        return a[2] > b[2];
    }

    private static int[] parseSemver(String tag) {
        String clean = tag.startsWith("v") ? tag.substring(1) : tag;
        String[] parts = clean.split("\\.");
        int[] nums = {0,0,0};
        for (int i = 0; i < parts.length && i < 3; i++) {
            try { nums[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) {}
        }
        return nums;
    }

    private static long enqueueApkDownload(Context context, String apkUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("App Update")
                .setDescription("Downloading new Version")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return dm.enqueue(request);
    }

    private static void registerInstallOnDownloadComplete(Context context, long downloadId) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (id != downloadId) return;

                Uri uri = dm.getUriForDownloadedFile(downloadId);
                if (uri == null) return;

                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(uri, "application/vnd.android.package-archive");
                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(install);

                ctx.unregisterReceiver(this);
            }
        };
        ContextCompat.registerReceiver(context, receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

}
