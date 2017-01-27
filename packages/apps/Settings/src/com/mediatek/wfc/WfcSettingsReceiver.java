/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.wfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.settings.FeatureOption;


/* Broadcast receiver to receive SIM_CHANGE & WIFI_STATE_CHANGE intents
 * & take action accordingly for WFC Settings
 */
public class WfcSettingsReceiver extends BroadcastReceiver {

    private static final String TAG = "WfcSettingsReceiver";
    private static final String FIRST_TIME = "first_time_in_wfcSetting";
    private static final String WIFI_CONNECTED_FIRST_TIME = "wifi_connected_first_time";
    private static final String KEY_ISIM_PRESENT = "persist.sys.wfc_isimAppPresent";
    // Hotknot intents which tells hotKnot wifi mac discovery start & finish
    private static final String HOTKNOT_DISCOVERY_START_ACTION = "com.mediatek.hotknot.DISCOVERY_START_ACTION";
    private static final String HOTKNOT_DISCOVERY_END_ACTION = "com.mediatek.hotknot.DISCOVERY_END_ACTION";
    private static final String LISTEN_WIFI_STATE_CHANGE_INTENT_FLAG = "listen_wifi_state_change_intent_flag";

    
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.v(TAG, action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            /* Add in db only on first time */
            if (sp.getBoolean(FIRST_TIME, true)) {
                if (FeatureOption.MTK_WFC_SUPPORT) {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.WHEN_TO_MAKE_WIFI_CALLS,
                            TelephonyManager.WifiCallingChoices.ALWAYS_USE);
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SELECTED_WFC_PREFERRENCE,
                            TelephonyManager.WifiCallingPreferences.WIFI_PREFERRED);
                    Log.v(TAG, "set wfc settings as ON & wifi-prefered");
                } else {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.WHEN_TO_MAKE_WIFI_CALLS,
                            TelephonyManager.WifiCallingChoices.NEVER_USE);
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SELECTED_WFC_PREFERRENCE,
                            TelephonyManager.WifiCallingPreferences.CELLULAR_ONLY);
                    Log.v(TAG, "set wfc settings as OFF & cellular only");
                }
                Editor ed = sp.edit();
                ed.putBoolean(FIRST_TIME, false);
                ed.commit();
                Log.v(TAG, "first_time_in_wfcSetting after commit" + sp.getBoolean(FIRST_TIME, true));
            }
        }
        else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action) && FeatureOption.MTK_WFC_SUPPORT
                && getWifiStateChangeListenFlag(context)) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            Log.v(TAG, "wifiState:" + wifiState);
            if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                // TODO: 
                if (sp.getBoolean(WIFI_CONNECTED_FIRST_TIME, true)) {
                    sp.edit().putBoolean(WIFI_CONNECTED_FIRST_TIME, false).commit();
                    Log.v(TAG, "wifi_connected_first_time after commit"
                            + sp.getBoolean(WIFI_CONNECTED_FIRST_TIME, true));
                }
                else return;

                Log.v(TAG, "launching WFC wifi dialog activity");
                Intent i = new Intent(context, WfcWifiDialogActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }
        else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED) && FeatureOption.MTK_WFC_SUPPORT) {
            String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            Log.v(TAG, "simState:" + simState);
            if (simState.equals(IccCardConstants.INTENT_VALUE_ICC_READY)) {
                if (Settings.System.getInt(context.getContentResolver(),
                        Settings.System.WHEN_TO_MAKE_WIFI_CALLS,
                        TelephonyManager.WifiCallingChoices.NEVER_USE)
                        == TelephonyManager.WifiCallingChoices.ALWAYS_USE
                        && Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SELECTED_WFC_PREFERRENCE,
                        TelephonyManager.WifiCallingPreferences.WIFI_PREFERRED)
                        == TelephonyManager.WifiCallingPreferences.WIFI_ONLY) {
                    //setRadioPower(context, false);
                    WfcSettings.sendWifiOnlyModeIntent(context, true);
                    Log.v(TAG, "Turn OFF radio, as wfc is getting ON & pref is wifi_only");
                }
            }
        }
        //ALPS02164418: On boot Hotknot turns Wifi ON to read MAC address from MD.
        // After getting MAC, it turns Wifi OFF. This turning of Wifi ON causes
        // showing of WFC Wifi first time ON Alert, while User has not turned wifi ON.
        // To prevent this, HotKnot intimates other user of its MAC discovery start &
        // end via HOTKNOT_DISCOVERY_START_ACTION & HOTKNOT_DISCOVERY_END_ACTION intents,
        // During this period WfcRecevier will ignore Wifi_state_change intents.
        else if (action.equals(HOTKNOT_DISCOVERY_START_ACTION)) {
            Log.v(TAG, "Turn OFF wifiStateChange listen flag");
            setWifiStateChangeListenFlag(context, false);
        }
        else if (action.equals(HOTKNOT_DISCOVERY_END_ACTION)) {
            Log.v(TAG, "Turn ON wifiStateChange listen flag");
            setWifiStateChangeListenFlag(context, true);
        }
    }

    private void setWifiStateChangeListenFlag(Context context, boolean flag) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(LISTEN_WIFI_STATE_CHANGE_INTENT_FLAG, flag).commit();
    }

    private boolean getWifiStateChangeListenFlag(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(LISTEN_WIFI_STATE_CHANGE_INTENT_FLAG, true);
    }

    private void setRadioPower(Context context, boolean turnOn) {
        int sim_mode = turnOn ? 1:0; 
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, sim_mode);
        
        ITelephony iTel = ITelephony.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE));
        try {
            iTel.setRadioForSubscriber(SubscriptionManager.getDefaultVoiceSubId(), turnOn);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in setRadioPower" + e);
            e.printStackTrace();
        }
    }
}
