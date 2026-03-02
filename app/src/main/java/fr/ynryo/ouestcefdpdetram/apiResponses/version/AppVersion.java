package fr.ynryo.ouestcefdpdetram.apiResponses.version;

import androidx.annotation.NonNull;

public class AppVersion {
    private boolean success;
    private String appName;
    private String version;
    private int versionCode;
    private String type;
    private String url;

    public boolean isSuccess() {
        return success;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getAppName() {
        return appName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "AppVersion{" +
                "success=" + success +
                ", app_name='" + appName + '\'' +
                ", version='" + version + '\'' +
                ", version_code=" + versionCode +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
