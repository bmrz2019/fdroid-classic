package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.fdroid.fdroid.CleanCacheService;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;

public class PreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String[] SUMMARIES_TO_UPDATE = {
            Preferences.PREF_UPD_INTERVAL,
            Preferences.PREF_UPD_WIFI_ONLY,
            Preferences.PREF_UPD_NOTIFY,
            Preferences.PREF_UPD_HISTORY,
            Preferences.PREF_ROOTED,
            Preferences.PREF_HIDE_ANTI_FEATURE_APPS,
            Preferences.PREF_INCOMP_VER,
            Preferences.PREF_THEME,
            Preferences.PREF_IGN_TOUCH,
            Preferences.PREF_LANGUAGE,
            Preferences.PREF_KEEP_CACHE_TIME,
            Preferences.PREF_EXPERT,
            Preferences.PREF_PRIVILEGED_INSTALLER,
            Preferences.PREF_ENABLE_PROXY,
            Preferences.PREF_PROXY_HOST,
            Preferences.PREF_PROXY_PORT,
    };

    private static final int REQUEST_INSTALL_ORBOT = 0x1234;
    private CheckBoxPreference enableProxyCheckPref;
    private CheckBoxPreference useTorCheckPref;
    private Preference updateAutoDownloadPref;
    private long currentKeepCacheTime;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.preferences);
        useTorCheckPref = findPreference(Preferences.PREF_USE_TOR);
        enableProxyCheckPref = findPreference(Preferences.PREF_ENABLE_PROXY);
        updateAutoDownloadPref = findPreference(Preferences.PREF_AUTO_DOWNLOAD_INSTALL_UPDATES);
        Preference reset_transient = findPreference(Preferences.RESET_TRANSIENT);
        reset_transient.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset_transient_title)
                    .setMessage(R.string.reset_transient_message)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        DBHelper.resetTransient(getContext());
                        UpdateService.updateNow(getContext());
                    })
                    .setNegativeButton(android.R.string.no, null).show();
            return true;
        });
    }

    private void checkSummary(String key, int resId) {
        CheckBoxPreference pref = findPreference(key);
        pref.setSummary(resId);
    }

    private void entrySummary(String key) {
        ListPreference pref = findPreference(key);
        pref.setSummary(pref.getEntry());
    }

    private void textSummary(String key, int resId) {
        EditTextPreference pref = findPreference(key);
        pref.setSummary(getString(resId, pref.getText()));
    }

    private void updateSummary(String key, boolean changing) {

        int result = 0;

        switch (key) {
            case Preferences.PREF_UPD_INTERVAL:
                ListPreference listPref = findPreference(
                        Preferences.PREF_UPD_INTERVAL);
                int interval = Integer.parseInt(listPref.getValue());
                Preference onlyOnWifi = findPreference(
                        Preferences.PREF_UPD_WIFI_ONLY);
                onlyOnWifi.setEnabled(interval > 0);
                if (interval == 0) {
                    listPref.setSummary(R.string.update_interval_zero);
                } else {
                    listPref.setSummary(listPref.getEntry());
                }
                break;

            case Preferences.PREF_UPD_WIFI_ONLY:
                checkSummary(key, R.string.automatic_scan_wifi_on);
                break;

            case Preferences.PREF_UPD_NOTIFY:
                checkSummary(key, R.string.notify_on);
                break;

            case Preferences.PREF_UPD_HISTORY:
                textSummary(key, R.string.update_history_summ);
                break;

            case Preferences.PREF_THEME:
                entrySummary(key);
                if (changing) {
                    Activity activity = getActivity();
                    result |= PreferencesActivity.RESULT_RESTART;
                    activity.setResult(result);
                    FDroidApp fdroidApp = (FDroidApp) activity.getApplication();
                    fdroidApp.reloadTheme();
                    fdroidApp.applyTheme(activity);
                    FDroidApp.forceChangeTheme(activity);
                }
                break;

            case Preferences.PREF_INCOMP_VER:
                checkSummary(key, R.string.show_incompat_versions_on);
                break;

            case Preferences.PREF_ROOTED:
                checkSummary(key, R.string.rooted_on);
                break;

            case Preferences.PREF_HIDE_ANTI_FEATURE_APPS:
                checkSummary(key, R.string.hide_anti_feature_apps_on);
                break;

            case Preferences.PREF_IGN_TOUCH:
                checkSummary(key, R.string.ignoreTouch_on);
                break;

            case Preferences.PREF_LANGUAGE:
                entrySummary(key);
                if (changing) {
                    result |= PreferencesActivity.RESULT_RESTART;
                    Activity activity = getActivity();
                    activity.setResult(result);
                    ((FDroidApp) activity.getApplication()).updateLanguage();
                }
                break;

            case Preferences.PREF_KEEP_CACHE_TIME:
                entrySummary(key);
                if (changing
                        && currentKeepCacheTime != Preferences.get().getKeepCacheTime()) {
                    CleanCacheService.schedule(getActivity());
                }
                break;

            case Preferences.PREF_EXPERT:
                checkSummary(key, R.string.expert_on);
                break;

            case Preferences.PREF_PRIVILEGED_INSTALLER:
                checkSummary(key, R.string.system_installer_on);
                break;

            case Preferences.PREF_ENABLE_PROXY:
                CheckBoxPreference checkPref = findPreference(key);
                checkPref.setSummary(R.string.enable_proxy_summary);
                break;

            case Preferences.PREF_PROXY_HOST:
                EditTextPreference textPref = findPreference(key);
                String text = Preferences.get().getProxyHost();
                if (TextUtils.isEmpty(text) || text.equals(Preferences.DEFAULT_PROXY_HOST)) {
                    textPref.setSummary(R.string.proxy_host_summary);
                } else {
                    textPref.setSummary(text);
                }
                break;

            case Preferences.PREF_PROXY_PORT:
                EditTextPreference textPref2 = findPreference(key);
                int port = Preferences.get().getProxyPort();
                if (port == Preferences.DEFAULT_PROXY_PORT) {
                    textPref2.setSummary(R.string.proxy_port_summary);
                } else {
                    textPref2.setSummary(String.valueOf(port));
                }
                break;
        }
    }

    /**
     * Initializes SystemInstaller preference, which can only be enabled when F-Droid is installed as a system-app
     */
    private void initPrivilegedInstallerPreference() {
        final CheckBoxPreference pref = findPreference(Preferences.PREF_PRIVILEGED_INSTALLER);
        Preferences p = Preferences.get();
        boolean enabled = p.isPrivilegedInstallerEnabled();
        boolean installed = PrivilegedInstaller.isExtensionInstalledCorrectly(getActivity())
                == PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES;
        pref.setEnabled(installed);
        pref.setDefaultValue(installed);
        pref.setChecked(enabled && installed);

        pref.setOnPreferenceClickListener(preference -> {
            SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
            if (pref.isChecked()) {
                editor.remove(Preferences.PREF_PRIVILEGED_INSTALLER);
            } else {
                editor.putBoolean(Preferences.PREF_PRIVILEGED_INSTALLER, false);
            }
            editor.apply();
            return true;
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        for (final String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
        }

        currentKeepCacheTime = Preferences.get().getKeepCacheTime();

        initPrivilegedInstallerPreference();
        // this pref's default is dynamically set based on whether Orbot is installed
        boolean useTor = Preferences.get().isTorEnabled();
        useTorCheckPref.setDefaultValue(useTor);
        useTorCheckPref.setChecked(useTor);
        useTorCheckPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object enabled) {
                if ((Boolean) enabled) {
                    final Activity activity = getActivity();
                    enableProxyCheckPref.setEnabled(false);
                    if (OrbotHelper.isOrbotInstalled(activity)) {
                        NetCipher.useTor();
                    } else {
                        Intent intent = OrbotHelper.getOrbotInstallIntent(activity);
                        activity.startActivityForResult(intent, REQUEST_INSTALL_ORBOT);
                    }
                } else {
                    enableProxyCheckPref.setEnabled(true);
                    NetCipher.clearProxy();
                }
                return true;
            }
        });

        if (PrivilegedInstaller.isDefault(getActivity())) {
            updateAutoDownloadPref.setTitle(R.string.update_auto_install);
            updateAutoDownloadPref.setSummary(R.string.update_auto_install_summary);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        Preferences.get().configureProxy();
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
    }

}
