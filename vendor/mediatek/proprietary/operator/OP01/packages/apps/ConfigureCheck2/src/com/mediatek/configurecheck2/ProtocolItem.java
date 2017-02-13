/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.configurecheck2;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioSystem;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.telephony.RadioAccessFamily;
import android.telephony.TelephonyManager;

import android.provider.Settings;
import android.view.View;

import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * CheckNetworkMode is for EM->Telephony->Network Selecting
 * only for getModemType() == MODEM_TD, because CTSC is for op01
 */
class CheckNetworkMode extends CheckItemBase {
     private static final String TAG = " ProtocolItem CheckNetWorkMode";
     private boolean mAsyncDone = true;
     private boolean mNeedNofity = false;

     private static final int WCDMA_PREFERRED = Phone.NT_MODE_WCDMA_PREF;
     private static final int GSM_ONLY = Phone.NT_MODE_GSM_ONLY;
     private static final int WCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY;
     private static final int GSM_WCDMA_AUTO = Phone.NT_MODE_GSM_UMTS;
     private static final int LTE_ONLY = Phone.NT_MODE_LTE_ONLY;
     private static final int LTE_GSM_WCDMA = Phone.NT_MODE_LTE_GSM_WCDMA;
     private static final int LTE_GSM_WCDMA_PREFERRED = 31;
     //RILConstants.NETWORK_MODE_LTE_GSM_WCDMA_PREF;
     private static final int LTE_WCDMA = Phone.NT_MODE_LTE_WCDMA;
     //private static final int LTE_GSM = Phone.NT_MODE_LTE_GSM;
     private int mUserSelectMode = -1;
     private int mSubId = 1;

     private final Handler mNetworkQueryHandler = new Handler() {
          public final void handleMessage(Message msg) {
            CTSCLog.d(TAG, "Receive msg from network mode query");
            mAsyncDone = true;
            AsyncResult ar = (AsyncResult) msg.obj;
              if (ar.exception == null) {
                  int type = ((int[]) ar.result)[0];
                  CTSCLog.d(TAG, "Get Preferred Type " + type);
                  if (getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)) {
                      //for Settings->More...->Mobile networks->Preferred network type
                      switch (type) {
                      case GSM_ONLY: //GSM only
                      case WCDMA_ONLY: //TD-SCDMA only
                      case LTE_ONLY: //LTE only
                      case LTE_WCDMA: //4G/3G
                      //case LTE_GSM: //4G/2G
                          //display nothing
                          setValue("");
                          mResult = check_result.WRONG;
                          break;
                      case GSM_WCDMA_AUTO: //GSM/TD-SCDMA(auto)
                      case WCDMA_PREFERRED:
                          setValue(R.string.wcdma_pref);
                          mResult = check_result.RIGHT;
                          break;
                      case LTE_GSM_WCDMA:
                      case LTE_GSM_WCDMA_PREFERRED: //4G/3G/2G(auto)
                          setValue(R.string.lte_gsm_wcdma);
                          mResult = check_result.WRONG;
                          break;
                      default:
                          break;
                      }
                  } else {
                      //for EM->Telephony->Network Selecting
                      CTSCLog.d(TAG, "Get Preferred Type2 " + type);
                      switch (type) {
                      case GSM_ONLY: //GSM only
                          setValue("GSM only");
                          if (getKey().equals(CheckItemKeySet.CI_GSM_ONLY_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case WCDMA_ONLY: //TD-SCDMA only
                          setValue(R.string.value_NM_TD_SCDMA_Only);
                          if (getKey().equals(CheckItemKeySet.CI_TDWCDMA_ONLY) 
                              || getKey().equals(CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG)
                              || getKey().equals(CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case GSM_WCDMA_AUTO: //GSM/TD-SCDMA(auto)
                      case WCDMA_PREFERRED:
                          setValue(R.string.value_NM_TD_DUAL_MODE);                   
                          if (getKey().equals(CheckItemKeySet.CI_DUAL_MODE_CONFIG) 
                              || getKey().equals(CheckItemKeySet.CI_DUAL_MODE_CHECK)
                              || getKey().equals(CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {                        
                              mResult = check_result.WRONG;
                          }
                          break;
                      case LTE_ONLY: //LTE only
                          setValue("LTE only");
                          if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case LTE_GSM_WCDMA:
                      case LTE_GSM_WCDMA_PREFERRED: //4G/3G/2G(auto)
                          setValue("4G/3G/2G(auto)");
                          if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto_Check)
                              || getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case LTE_WCDMA: //4G/3G
                          setValue("4G/3G");
                          mResult = check_result.WRONG;
                          break;
                      /*case LTE_GSM: //4G/2G
                          setValue("4G/2G");
                          if (getKey().equals(CheckItemKeySet.CI_4G2G_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {                        
                              mResult = check_result.WRONG;
                          }
                          break;*/
                      default:
                          break;
                      }
                  }
              } else {
                 setValue("Query failed");                
              }
              if (mNeedNofity) {
                sendBroadcast();
              }
          }
      };
      
      private final Handler mNetworkSetHandler = new Handler() {
          public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg from network mode set");  
            if (getKey().equals(CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG)) {
                setValue(R.string.value_NM_TD_SCDMA_Only);
            } else if (getKey().equals(CheckItemKeySet.CI_DUAL_MODE_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG)){
                setValue(R.string.value_NM_TD_DUAL_MODE);
            } else if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
                setValue(R.string.value_NM_LTE_Only);
            } else if (getKey().equals(CheckItemKeySet.CI_4G2G_CONFIG)){
                setValue(R.string.value_NM_4G2G);
            } else if (getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)){
                setValue(R.string.wcdma_pref);
            } else if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)) {
                setValue(R.string.value_NM_4G3G2GAuto);
            } else if (getKey().equals(CheckItemKeySet.CI_GSM_ONLY_CONFIG)) {
                setValue(R.string.value_NM_GSM_Only);
            }
            CTSCLog.d(TAG, "update network mode done refresh");
            mResult = check_result.RIGHT;
            sendBroadcast();
          }
      };
    /*
     * set title and note in constructor function
     */
    CheckNetworkMode(Context c, String key) {
        super(c, key);

        setTitle(R.string.title_Network_Mode);
        StringBuilder note = new StringBuilder();

        if (key.equals(CheckItemKeySet.CI_TDWCDMA_ONLY)) {
            note.append(getContext().getString(R.string.note_NM_TD_SCDMA_Only))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_Protocol))
                .append(getContext().getString(R.string.SOP_PhoneCard));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if(key.equals(CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG)
                || key.equals(CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG)){
            note.append(getContext().getString(R.string.note_NM_TD_SCDMA_Only));
            if (key.equals(CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG)) {
                note.append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_Protocol))
                    .append(getContext().getString(R.string.SOP_PhoneCard));
            } else if (key.equals(CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG)){
                note.append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_TDS_PerfCon))
                    .append(getContext().getString(R.string.SOP_LTE_RF));
            }
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_DUAL_MODE_CONFIG)
                || key.equals(CheckItemKeySet.CI_DUAL_MODE_CHECK)
                || key.equals(CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG)){
            note.append(getContext().getString(R.string.note_NM_TD_DUAL_MODE));
            if (key.equals(CheckItemKeySet.CI_DUAL_MODE_CHECK)) {
                setProperty(PROPERTY_AUTO_CHECK); 
                note.append(getContext().getString(R.string.SOP_self_check))
                    .append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_Protocol))
                    .append(getContext().getString(R.string.SOP_PhoneCard));
            } else if (key.equals(CheckItemKeySet.CI_DUAL_MODE_CONFIG)){
                setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
                note.append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_Protocol))
                    .append(getContext().getString(R.string.SOP_PhoneCard));
            } else if (key.equals(CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG)){
                setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
                note.append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_TDS_PerfCon))
                    .append(getContext().getString(R.string.SOP_TDS_RRMcon));
            }
        } else if(key.equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
            note.append(getContext().getString(R.string.note_NM_Lte_Only))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_LTE_NS_IOT));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_GSM_ONLY_CONFIG)){
            note.append(getContext().getString(R.string.note_NM_Gsm_Only))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_LTE_IPv6));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_4G2G_CONFIG)){
            note.append(getContext().getString(R.string.note_NM_4G2G))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_LTE_NS_IOT));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)){
            setTitle(R.string.prefNetworkType_title);
            note.append(getContext().getString(R.string.prefNetworkType_note))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_LTE_IOT_A_F));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_4G_3G_2G_Auto_Check)
                || key.equals(CheckItemKeySet.CI_4G_3G_2G_Auto)){
            note.append(getContext().getString(R.string.note_NM_4g3g2gAuto));
            if (key.equals(CheckItemKeySet.CI_4G_3G_2G_Auto_Check)) {
                setProperty(PROPERTY_AUTO_CHECK);
            } else if (key.equals(CheckItemKeySet.CI_4G_3G_2G_Auto)){
                setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
            }
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
        setNote(note.toString());
    }

    public boolean onCheck() {
        getNetworkMode();
        return true;
    }
    
    public check_result getCheckResult() {
        if (!mAsyncDone) {
            mResult = check_result.UNKNOWN;
            mNeedNofity = true;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        mNeedNofity = false;
        CTSCLog.d(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            return false;
        }
        setNetWorkMode();  
        return true;
    }
    
    private int getCapabilitySim() {
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        ITelephony iTelephony =
                ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        CTSCLog.d(TAG, "getSimCount: " + telephonyManager.getSimCount());
        if (iTelephony == null || telephonyManager == null
                || telephonyManager.getSimCount() <= 1) {
            return PhoneConstants.SIM_ID_1;
        }

        for (int i = 0; i < telephonyManager.getSimCount(); i++) {
            try {
                CTSCLog.d(TAG, "get 3G capability: " + iTelephony.getRadioAccessFamily(i));
                if ((iTelephony.getRadioAccessFamily(i) & (RadioAccessFamily.RAF_UMTS
                        | RadioAccessFamily.RAF_LTE)) > 0) {
                    CTSCLog.d(TAG, "get 3G capability Sim: " + i);
                    return i;
                }
            } catch (RemoteException e) {
                CTSCLog.e(TAG, e.getMessage());
            }
        }
        return PhoneConstants.SIM_ID_1;
    }
    
    private void getNetworkMode() {
        Phone mPhone = null;
        CTSCLog.i(TAG, "getNetworkMode");
        mAsyncDone = false;
        int mSimType = getCapabilitySim();
        int[] subId = SubscriptionManager.getSubId(mSimType);
        if (subId != null) {
            for (int i = 0; i < subId.length; i++) {
                CTSCLog.i(TAG, "subId[" + i + "]: " + subId[i]);
            }
        }
        if (subId == null || subId.length == 0
                || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            CTSCLog.e(TAG, "Invalid sub id");
        } else {
            mSubId = subId[0];
        }
        
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(mSimType);
            mPhone.getPreferredNetworkType(
                    mNetworkQueryHandler.obtainMessage());
        } else {
             mPhone = PhoneFactory.getDefaultPhone();
             mPhone.getPreferredNetworkType(mNetworkQueryHandler.obtainMessage());
        }
    }  

    private void setNetWorkMode() {
        Phone mPhone = null;
        Message msg = null;        
        int mSimType = getCapabilitySim();       
        int[] subId = SubscriptionManager.getSubId(mSimType);
        if (subId != null) {
            for (int i = 0; i < subId.length; i++) {
                CTSCLog.i(TAG, "subId[" + i + "]: " + subId[i]);
            }
        }
        if (subId == null || subId.length == 0
                || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            CTSCLog.e(TAG, "Invalid sub id");
        } else {
            mSubId = subId[0];
        }       
        CTSCLog.i(TAG, "setNetworkMode");
        setValue("Modifing...");
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            mPhone =PhoneFactory.getPhone(mSimType);

            if (getKey().equals(CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG)) {
               mUserSelectMode = WCDMA_ONLY;
               mPhone.setPreferredNetworkType(WCDMA_ONLY, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_DUAL_MODE_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)){
                mUserSelectMode = GSM_WCDMA_AUTO;
                mPhone.setPreferredNetworkType(GSM_WCDMA_AUTO, mNetworkSetHandler.obtainMessage());
            }  else if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
                mUserSelectMode = LTE_ONLY;
                mPhone.setPreferredNetworkType(LTE_ONLY, mNetworkSetHandler.obtainMessage());
            } /*else if (getKey().equals(CheckItemKeySet.CI_4G2G_CONFIG)){
                mPhone.setPreferredNetworkType(LTE_GSM, mNetworkSetHandler.obtainMessage());
            } */else if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)){
                mUserSelectMode = LTE_GSM_WCDMA;
                mPhone.setPreferredNetworkType(LTE_GSM_WCDMA, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_GSM_ONLY_CONFIG)){
                mUserSelectMode = GSM_ONLY;
                mPhone.setPreferredNetworkType(GSM_ONLY, mNetworkSetHandler.obtainMessage());
            }
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
            if (getKey().equals(CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG)) {
                mUserSelectMode = WCDMA_ONLY;
                mPhone.setPreferredNetworkType(WCDMA_ONLY, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_DUAL_MODE_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG)
                    || getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)){
                mUserSelectMode = GSM_WCDMA_AUTO;
                mPhone.setPreferredNetworkType(GSM_WCDMA_AUTO, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_LTE_ONLY_CONFIG)){
                mUserSelectMode = LTE_ONLY;
                mPhone.setPreferredNetworkType(LTE_ONLY, mNetworkSetHandler.obtainMessage());
            } /*else if (getKey().equals(CheckItemKeySet.CI_4G2G_CONFIG)){
                mPhone.setPreferredNetworkType(LTE_GSM, mNetworkSetHandler.obtainMessage());
            }*/ else if (getKey().equals(CheckItemKeySet.CI_4G_3G_2G_Auto)){
                mUserSelectMode = LTE_GSM_WCDMA;
                mPhone.setPreferredNetworkType(LTE_GSM_WCDMA, mNetworkSetHandler.obtainMessage());
            } else if (getKey().equals(CheckItemKeySet.CI_GSM_ONLY_CONFIG)){
                mUserSelectMode = GSM_ONLY;
                mPhone.setPreferredNetworkType(GSM_ONLY, mNetworkSetHandler.obtainMessage());
            }
        }
        Settings.Global.putInt(getContext().getContentResolver(),
              Settings.Global.USER_PREFERRED_NETWORK_MODE, mUserSelectMode);
        Settings.Global.putInt(getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId, mUserSelectMode);
    }
}

/**
 * CheckLteNetworkMode is for EM->Telephony->LTE Network Mode
 */
class CheckLteNetworkMode extends CheckItemBase {
     private static final String TAG = "CheckLteNetworkMode";
     private Phone mPhone = null;
     
     private static final int WCDMA_PREFERRED = Phone.NT_MODE_WCDMA_PREF;
     private static final int WCDMA_ONLY = Phone.NT_MODE_WCDMA_ONLY;
     private static final int GSM_ONLY = Phone.NT_MODE_GSM_ONLY; 
     private static final int GSM_WCDMA = Phone.NT_MODE_GSM_UMTS;
     //private static final int LTE_GSM = Phone.NT_MODE_LTE_GSM;
     private static final int LTE_ONLY = Phone.NT_MODE_LTE_ONLY;
     private static final int LTE_WCDMA = Phone.NT_MODE_LTE_WCDMA;
     private static final int LTE_WCDMA_GSM = Phone.NT_MODE_LTE_GSM_WCDMA;
     //private static final int WCDMA_GSM_LTE = Phone.NT_MODE_GSM_WCDMA_LTE;
     
     private boolean mAsyncDone = true;
     private boolean mNeedNofity = false;
     private int mUserSelectMode = -1;
     private int mSubId = 1;

     private final Handler mNetworkQueryHandler = new Handler() {
          public final void handleMessage(Message msg) {
            CTSCLog.d(TAG, "Receive msg from network mode query");
            mAsyncDone = true;
            AsyncResult ar = (AsyncResult) msg.obj;
              if (ar.exception == null) {
                  int type = ((int[]) ar.result)[0];
                  CTSCLog.d(TAG, "Get Preferred Type " + type);
                  if (getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)) {
                      //for Settings->More...->Mobile networks->Preferred network type
                      switch (type) {
                      case GSM_ONLY: //2G Only
                      case WCDMA_ONLY: //3G Only
                      //case LTE_GSM: //4G and 2G
                      case LTE_ONLY: //4G Only
                      case LTE_WCDMA: //4G/3G
                      //case WCDMA_GSM_LTE: //3G/2G Preferred
                          //display nothing
                          setValue("");
                          mResult = check_result.WRONG;
                          break;
                      case WCDMA_PREFERRED: //3G and 2G
                      case GSM_WCDMA:
                          setValue(R.string.wcdma_pref);
                          mResult = check_result.RIGHT;
                          break;
                      case LTE_WCDMA_GSM: //4G/3G and 2G
                          setValue(R.string.lte_gsm_wcdma);
                          mResult = check_result.WRONG;
                          break;
                      default:                          
                          return;
                      }
                  } else {
                      //for EM->Telephony->LTE Network Mode
                      switch (type) {
                      case GSM_ONLY: //2G Only
                          setValue("2G Only");
                          if (getKey().equals(CheckItemKeySet.CI_2G_ONLY_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case WCDMA_ONLY: //3G Only
                          setValue("3G Only");
                          if (getKey().equals(CheckItemKeySet.CI_3G_ONLY_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case WCDMA_PREFERRED: //3G and 2G
                      case GSM_WCDMA:
                          setValue("3G and 2G");
                          if (getKey().equals(CheckItemKeySet.CI_3G_AND_2G_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                     /* case LTE_GSM: //4G and 2G
                          setValue("4G and 2G");
                          mResult = check_result.WRONG;
                          break;*/
                      case LTE_ONLY: //4G Only
                          setValue("4G Only");
                          if (getKey().equals(CheckItemKeySet.CI_4G_ONLY_CONFIG)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      case LTE_WCDMA: //4G/3G
                          setValue("4G/3G");
                          mResult = check_result.WRONG;
                          break;
                      case LTE_WCDMA_GSM: //4G/3G and 2G
                          setValue("4G/3G and 2G");
                          if (getKey().equals(CheckItemKeySet.CI_4G_3G_AND_2G_Check)
                                  || getKey().equals(CheckItemKeySet.CI_4G_3G_AND_2G)) {
                              mResult = check_result.RIGHT;
                          } else {
                              mResult = check_result.WRONG;
                          }
                          break;
                      /*case WCDMA_GSM_LTE: //3G/2G Preferred
                          setValue("3G/2G Preferred");
                          mResult = check_result.WRONG;
                          break;*/
                      default:
                          return;
                      }
                  }
              } else {
                  setValue("Query failed"); 
              }
              if (mNeedNofity) {
                  sendBroadcast();
              }
          }
      };
      
      private final Handler mNetworkSetHandler = new Handler() {
          public final void handleMessage(Message msg) {
              CTSCLog.i(TAG, "Receive msg from network mode set");  
              AsyncResult ar = (AsyncResult) msg.obj;
              if (ar.exception != null) {
                  setValue("Set failed");
                  mResult = check_result.WRONG;
              } else {
                  if (getKey().equals(CheckItemKeySet.CI_3G_ONLY_CONFIG)) {
                      Settings.Global.putInt(getContext().getContentResolver(),
                              Settings.Global.PREFERRED_NETWORK_MODE, WCDMA_ONLY);
                      setValue("3G Only");
                  } else if (getKey().equals(CheckItemKeySet.CI_3G_AND_2G_CONFIG)) {
                      Settings.Global.putInt(getContext().getContentResolver(),
                              Settings.Global.PREFERRED_NETWORK_MODE, GSM_WCDMA);
                      setValue("3G and 2G");
                  } else if (getKey().equals(CheckItemKeySet.CI_4G_ONLY_CONFIG)){
                      Settings.Global.putInt(getContext().getContentResolver(),
                              Settings.Global.PREFERRED_NETWORK_MODE, LTE_ONLY);
                      setValue("4G Only");
                  } else if (getKey().equals(CheckItemKeySet.CI_2G_ONLY_CONFIG)){
                      Settings.Global.putInt(getContext().getContentResolver(),
                              Settings.Global.PREFERRED_NETWORK_MODE, GSM_ONLY);
                      setValue("2G Only");
                  } else if (getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)) {
                      Settings.Global.putInt(getContext().getContentResolver(),
                              Settings.Global.PREFERRED_NETWORK_MODE, GSM_WCDMA);
                      setValue(R.string.wcdma_pref);
                  } else if (getKey().equals(CheckItemKeySet.CI_4G_3G_AND_2G)) {
                      Settings.Global.putInt(getContext().getContentResolver(),
                              Settings.Global.PREFERRED_NETWORK_MODE, LTE_WCDMA_GSM);
                      setValue("4G/3G and 2G");
                  }
                  CTSCLog.d(TAG, "update network mode done refresh");
                  mResult = check_result.RIGHT;
              }
              sendBroadcast();
          }
      };
      
    CheckLteNetworkMode (Context c, String key) {
        super(c, key);
        
        if (key.equals(CheckItemKeySet.CI_3G_ONLY_CONFIG)) {
            setTitle(R.string.title_Network_Mode);
            setNote(getContext().getString(R.string.note_LTE_NM_3G_Only));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_3G_AND_2G_CONFIG)){
            setTitle(R.string.title_Network_Mode);
            setNote(getContext().getString(R.string.note_LTE_NM_3G_and_2G));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_4G_3G_AND_2G_Check)
                || key.equals(CheckItemKeySet.CI_4G_3G_AND_2G)){
            setTitle(R.string.title_Network_Mode);
            setNote(getContext().getString(R.string.note_LTE_NM_4G_3G_and_2G));
            if (key.equals(CheckItemKeySet.CI_4G_3G_AND_2G_Check)) {
                setProperty(PROPERTY_AUTO_CHECK);
            } else if (key.equals(CheckItemKeySet.CI_4G_3G_AND_2G)){
                setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
            }
        } else if (key.equals(CheckItemKeySet.CI_4G_ONLY_CONFIG)) {
            setTitle(R.string.title_Network_Mode);
            setNote(getContext().getString(R.string.note_LTE_NM_4G_Only));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_2G_ONLY_CONFIG)) {
            setTitle(R.string.title_Network_Mode);
            setNote(getContext().getString(R.string.note_LTE_NM_2G_Only));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if(key.equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)){
            setTitle(R.string.prefNetworkType_title);
            setNote(getContext().getString(R.string.prefNetworkType_note)
                    + getContext().getString(R.string.SOP_REFER)
                    + getContext().getString(R.string.SOP_LTE_IOT_A_F));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }     
        
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            int mSimType = getCapabilitySim();
            mPhone = PhoneFactory.getPhone(mSimType);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
    }


    private int getCapabilitySim() {
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        ITelephony iTelephony =
                ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        CTSCLog.d(TAG, "getSimCount: " + telephonyManager.getSimCount());
        if (iTelephony == null || telephonyManager == null
                || telephonyManager.getSimCount() <= 1) {
            return PhoneConstants.SIM_ID_1;
        }

        for (int i = 0; i < telephonyManager.getSimCount(); i++) {
            try {
                CTSCLog.d(TAG, "get 3G capability: " + iTelephony.getRadioAccessFamily(i));
                if ((iTelephony.getRadioAccessFamily(i) & (RadioAccessFamily.RAF_UMTS
                        | RadioAccessFamily.RAF_LTE)) > 0) {
                    CTSCLog.d(TAG, "get 3G capability Sim: " + i);
                    return i;
                }
            } catch (RemoteException e) {
                CTSCLog.e(TAG, e.getMessage());
            }
        }
        return PhoneConstants.SIM_ID_1;
    }
    
    public boolean onCheck() {
        getNetworkMode();
        return true;
    }
    
    public check_result getCheckResult() {
        if (!mAsyncDone) {
            mResult = check_result.UNKNOWN;
            mNeedNofity = true;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        mNeedNofity = false;
        CTSCLog.d(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            return false;
        }
        setNetWorkMode();  
        return true;
    }
    
    private void getNetworkMode() {
        CTSCLog.i(TAG, "getNetworkMode");
        mAsyncDone = false;
        mPhone.getPreferredNetworkType(mNetworkQueryHandler.obtainMessage());
    }  

    private void setNetWorkMode() {
        CTSCLog.i(TAG, "setNetworkMode");
        setValue("Modifing...");
        if (getKey().equals(CheckItemKeySet.CI_3G_ONLY_CONFIG)) {
            mPhone.setPreferredNetworkType(WCDMA_ONLY, mNetworkSetHandler.obtainMessage());
            mUserSelectMode = WCDMA_ONLY;
        } else if (getKey().equals(CheckItemKeySet.CI_3G_AND_2G_CONFIG)
                || getKey().equals(CheckItemKeySet.CI_PREF_WCDMA_PREF)) {
            mPhone.setPreferredNetworkType(GSM_WCDMA, mNetworkSetHandler.obtainMessage());
            mUserSelectMode = GSM_WCDMA;            
        } else if (getKey().equals(CheckItemKeySet.CI_4G_ONLY_CONFIG)) {            
            mPhone.setPreferredNetworkType(LTE_ONLY, mNetworkSetHandler.obtainMessage());
            mUserSelectMode = LTE_ONLY;            
        } else if (getKey().equals(CheckItemKeySet.CI_2G_ONLY_CONFIG)) {
            mPhone.setPreferredNetworkType(GSM_ONLY, mNetworkSetHandler.obtainMessage());
            mUserSelectMode = GSM_ONLY;
        } else if (getKey().equals(CheckItemKeySet.CI_4G_3G_AND_2G)) {
            mPhone.setPreferredNetworkType(LTE_WCDMA_GSM, mNetworkSetHandler.obtainMessage());
            mUserSelectMode = LTE_WCDMA_GSM;
        }
        Settings.Global.putInt(getContext().getContentResolver(),
            Settings.Global.USER_PREFERRED_NETWORK_MODE, mUserSelectMode);
        Settings.Global.putInt(getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId, mUserSelectMode);
    }
}

class CheckGPRSProtocol extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckGPRSProtol";
    private boolean needRefresh = false;

    private Context getEMContext() {
        Context eMContext = null;
        try {
            eMContext = getContext().createPackageContext(
                    "com.mediatek.engineermode", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        if (null == eMContext) {
            throw new NullPointerException("eMContext=" + eMContext);
        }
        return eMContext;
    }

    private final Handler mResponseHander = new Handler() {
          public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg form GPRS always attached continue  set");
            setValue(R.string.value_GPRS_attach_continue);
            mResult = check_result.RIGHT;
            sendBroadcast();
         }
    };

    CheckGPRSProtocol(Context c, String key) {
        super(c, key);

        if (key.equals(CheckItemKeySet.CI_GPRS_ON)) {
            setTitle(R.string.title_GPRS_ALWAYS_ATTACH);
            setNote(getContext().getString(R.string.note_GPRS_always_on) + getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol) + "" + getContext().getString(R.string.SOP_PhoneCard));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_GPRS_CONFIG)) {
            setTitle(R.string.title_GPRS_ALWAYS_ATTACH);
            setNote(getContext().getString(R.string.note_GPRS_always_on) + getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol) + "" + getContext().getString(R.string.SOP_PhoneCard));
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE)
                || key.equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE_LTE)){ 
            setTitle(R.string.title_GPRS_ALWAYS_ATTACH_CONTINUE);
            if (key.equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE)) {
            setNote(getContext().getString(R.string.note_GPRS_attach_continue) + getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol) + "" + getContext().getString(R.string.SOP_PhoneCard));
            } else if (key.equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE_LTE)) {
                setNote(getContext().getString(R.string.note_GPRS_attach_continue) + getContext().getString(R.string.SOP_REFER) + 
                        getContext().getString(R.string.SOP_TDS_RRMcon));
            }
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
    }

    public check_result getCheckResult() {
        /*
         * implement check function here
         */
        CTSCLog.i(TAG, "getCheckResult");
        String resultString;
        String resultImg;
        String note = null;

        int gprsAttachType = SystemProperties.getInt(
                "persist.radio.gprs.attach.type", 1);
        CTSCLog.d(TAG, "get gprs mode gprsAttachType =" + gprsAttachType);

        if (getKey().equals(CheckItemKeySet.CI_GPRS_ON) ||
            getKey().equals(CheckItemKeySet.CI_GPRS_CONFIG)) { //yaling check how to get continue info
            if (gprsAttachType == 1) {
                setValue(R.string.value_GPRS_always_on);
                mResult = check_result.RIGHT;
            } else {
                setValue(R.string.value_GPRS_not_always_on);
                mResult = check_result.WRONG;
            }
            CTSCLog.d(TAG, "getCheckResult mResult = " + mResult);
            return mResult;
        } else if(getKey().equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE)
                || getKey().equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE_LTE)) {
            SharedPreferences preference = getEMContext().getSharedPreferences("com.mtk.GPRS", 0);
            int attachMode = preference.getInt("ATTACH_MODE", -1);
            CTSCLog.d(TAG, "yaling test attachmode = " + attachMode);

            if (attachMode == 1) {
                setValue(R.string.value_GPRS_attach_continue);
                mResult = check_result.RIGHT;
            } else if (attachMode == 0) {
                setValue(R.string.value_GPRS_when_needed_continue);
                mResult = check_result.WRONG;
            } else {
                setValue(R.string.value_GPRS_not_to_specify);
                mResult = check_result.WRONG;
            }
        }
        CTSCLog.d(TAG, "getCheckResult2 mResult = " + mResult);
        return mResult;
    }

    public boolean onReset() {
        /*
         * implement your reset function here
         */
        CTSCLog.i(TAG, "getReset");
        if (!isConfigurable()) {
            //On no, this instance is check only, not allow auto config.
            return false;
        }
        SystemProperties.set("persist.radio.gprs.attach.type", "1");

        if(getKey().equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE)
                || getKey().equals(CheckItemKeySet.CI_GPRS_ATTACH_CONTINUE_LTE)) {
            String cmdStr[] = { "AT+EGTYPE=1,1", "" };
            Phone mPhone = PhoneFactory.getDefaultPhone();
            mPhone.invokeOemRilRequestStrings(cmdStr, mResponseHander
                      .obtainMessage());

            setValue("setting...");
            mResult = check_result.UNKNOWN;

            SharedPreferences preference = getEMContext().getSharedPreferences("com.mtk.GPRS", 0);
            SharedPreferences.Editor edit = preference.edit();

            edit.putInt("ATTACH_MODE", 1);
            edit.commit();
        } else {
            setValue(R.string.value_GPRS_always_on);
            mResult = check_result.RIGHT;
        }
        return true;
    }
}

class CheckCFU extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckCFU";
    private boolean mAsyncDone = true;

    private final Handler mSetModemATHander = new Handler() {
        public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg form CFU set");  
            mAsyncDone = true;
            setValue(R.string.value_CFU_always_not_query);
            mResult = check_result.RIGHT;
            sendBroadcast();
        }
    };
    
    CheckCFU(Context c, String key) {
        super(c, key);

        if (key.equals(CheckItemKeySet.CI_CFU)) {
            setTitle(R.string.title_CFU);
            setProperty(PROPERTY_AUTO_CHECK);
            setNote(getContext().getString(R.string.note_CFU_off) + 
                    getContext().getString(R.string.SOP_REFER) + 
                    getContext().getString(R.string.SOP_Protocol));
        } else if (key.equals(CheckItemKeySet.CI_CFU_CONFIG_LTE)) {
            setTitle(R.string.title_CFU);
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
            setNote(getContext().getString(R.string.note_CFU_off_lte) + 
                    getContext().getString(R.string.SOP_REFER) + 
//                    getContext().getString(R.string.SOP_23gPerfCon) + 
                    getContext().getString(R.string.SOP_TDS_RRMcon) +
                    getContext().getString(R.string.SOP_TDS_PerfCon) + 
                    getContext().getString(R.string.SOP_LTE_RF));
        } else {
            setTitle(R.string.title_CFU);
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
            setNote(getContext().getString(R.string.note_CFU_off) +
            getContext().getString(R.string.SOP_REFER) +
            getContext().getString(R.string.SOP_Protocol));
        }
    }

    public boolean onCheck() {  
        CTSCLog.i(TAG, "onCheck");
        String cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP,
                PhoneConstants.CFU_QUERY_TYPE_DEF_VALUE);
        CTSCLog.i(TAG, "cfuSetting = " + cfuSetting);
        
        if (cfuSetting.equals("0")) {
            setValue(R.string.value_CFU_default);
            if (getKey().equals(CheckItemKeySet.CI_CFU_CONFIG_LTE)) {
                mResult = check_result.WRONG;
            } else {
                mResult = check_result.RIGHT;
            }
        } else if (cfuSetting.equals("1")) {
            setValue(R.string.value_CFU_always_not_query);
            mResult = check_result.RIGHT;
        } else if (cfuSetting.equals("2")) {
            setValue(R.string.value_CFU_always_query);
            mResult = check_result.WRONG;
        } else {
            setValue("CFU query failed");
            mResult = check_result.WRONG;
        }
        
        return true;
    }
    
    public check_result getCheckResult() {
        /*
         * implement check function here
         */
        if (!mAsyncDone) {
            mResult = check_result.UNKNOWN;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        CTSCLog.i(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }
    
    public boolean onReset() {
        /*
         * implement your reset function here
         */
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            //On no, this instance is check only, not allow auto config.
            return false;
        }
        setCFU();             
        return true;
    }

    private void setCFU() {
        mAsyncDone = false;
        String cmdString[] = new String[2];
        cmdString[0] = "AT+ESSP=1";
        cmdString[1] = "";
        CTSCLog.i(TAG, "setCFU");

        Phone mPhone = PhoneFactory.getDefaultPhone();
        mPhone.invokeOemRilRequestStrings(cmdString, mSetModemATHander.obtainMessage());
    }
}

class CheckCTAFTA extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckCTAFTA";
    private boolean mAsyncDone = true;
    private boolean mNeedNofity = false;

    private static final int MODEM_QUERY = 4;
    private static final int EVENT_QUERY_PREFERRED_TYPE_DONE = 1000;
    private static final int EVENT_SET_PREFERRED_TYPE_DONE = 1001;
    private static final int PCH_DATA_PREFER = 0;
    private static final int NETWORK_TYPE = 3;
    private static final int IPO_ENABLE = 1;
    private static final int IPO_DISABLE = 0;
    private boolean mModemFlag;
    private Phone mPhone = null;

    
    private final Handler mModemATHander = new Handler() {
        public final void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case MODEM_QUERY:
            mAsyncDone = true;
            CTSCLog.i(TAG, "recieve msg from query CTAFTA ");
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                String data[] = (String[]) ar.result;
                    handleQuery(data);                    
                } else {
                    setValue("Query failed");
                    mResult = check_result.UNKNOWN;
                } 
                if (mNeedNofity) {                 
                    sendBroadcast();
                }
                break;
            
            case EVENT_QUERY_PREFERRED_TYPE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int type = ((int[]) ar.result)[0];
                    CTSCLog.i(TAG, "Get Preferred Type " + type);
                    if (type == 0) {
                        mModemFlag = true;
                    } else {
                        mModemFlag = false;
                    }
                }
                break;
                
            case EVENT_SET_PREFERRED_TYPE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    CTSCLog.e(TAG, "Set preferrd type Fail");
                }
                break;
            }
            
        }
    };

    private void handleQuery(String[] data) {
        boolean isJB2 = (Build.VERSION.SDK_INT >= 17);
                if (null != data) {
                    CTSCLog.i(TAG, "data length is " + data.length);
                } else {
                    CTSCLog.i(TAG, "The returned data is wrong.");
                }
                int i = 0;
                for (String str : data) {
                    i++;
                }
                if (data[0].length() > 6) {
                    String mode = data[0].substring(7, data[0].length());
                    if (mode.length() >= 3) {
                        String subMode = mode.substring(0, 1);
                        String subCtaMode = mode.substring(2, mode.length());
                        CTSCLog.d(TAG, "subMode is " + subMode);
                        CTSCLog.d(TAG, "subCtaMode is " + subCtaMode);
                        mResult = check_result.WRONG;
                        if ("0".equals(subMode)) {
                            setValue(R.string.value_modem_none);
                    if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)
                            || getKey().equals(CheckItemKeySet.CI_CTAFTA)) {
                        mResult = check_result.RIGHT;
                    }
                } else if ("1".equals(subMode)) {
                    if (isJB2) {
                        setValue(R.string.value_modem_Integrity_off);
                    } else {
                        setValue(R.string.value_modem_CTA);
                    }
                    if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON)
                            || getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON_LTE)) {
                      mResult = check_result.RIGHT;
                    }                                 
                } else if ("2".equals(subMode)) {
                    setValue(R.string.value_modem_FTA);
                    /*if (getKey().equals(CheckItemKeySet.CI_MODEM_TEST_FTA_ON)) {
                        mResult = check_result.RIGHT;
                    }*/
                } else if ("3".equals(subMode)) {
                    setValue(R.string.value_modem_IOT);
                    if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)
                            || getKey().equals(CheckItemKeySet.CI_CTAFTA)) {
                        mResult = check_result.RIGHT;
                    }
                } else if ("4".equals(subMode)) {
                    setValue(R.string.value_modem_Operator);
                    if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)
                            || getKey().equals(CheckItemKeySet.CI_CTAFTA)) {
                        mResult = check_result.RIGHT;
                    }
                } else if (isJB2 && "5".equals(subMode)) {
                    setValue(R.string.value_modem_Factory);
                    if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)
                            || getKey().equals(CheckItemKeySet.CI_CTAFTA)) {
                        mResult = check_result.RIGHT;
                    }
                }
            } else {
                setValue("Query failed");
                mResult = check_result.UNKNOWN;
            }
        } else {
            setValue("Query failed");
            mResult = check_result.UNKNOWN;
        }
    }
    

    private final Handler mSetModemATHander = new Handler() {
        public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg form Mode set");
            AsyncResult ar;
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                if(getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON)
                        || getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON_LTE)) {
                    setValue(R.string.value_modem_Integrity_off);
                } else if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)) {
                    setValue(R.string.value_modem_none);
                } /*else if(getKey().equals(CheckItemKeySet.CI_MODEM_TEST_FTA_ON)) {
                    setValue(R.string.value_modem_FTA);
                }*/
                mResult = check_result.RIGHT;
            } else {
                setValue("AT cmd failed");
                mResult = check_result.WRONG;
            }
            
            sendBroadcast();
        }
    };

    CheckCTAFTA(Context c, String key) {
        super(c, key);
        if (key.equals(CheckItemKeySet.CI_CTAFTA)) { //check only
            setTitle(R.string.title_CTA_FTA);
            setNote(getContext().getString(R.string.note_CTA_FTA_off) +
                getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON)) {
            setTitle(R.string.title_integrity_check_off);
            setNote(getContext().getString(R.string.note_integrity_check_off) +
                getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol));
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)){
            setTitle(R.string.title_CTA_FTA);
            setNote(getContext().getString(R.string.note_CTA_FTA_off) +
                getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol));
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } /*else if (key.equals(CheckItemKeySet.CI_MODEM_TEST_FTA_ON)){ 
            setTitle(R.string.title_CTA_FTA);
            setNote(getContext().getString(R.string.note_fta_on) +
                    getContext().getString(R.string.SOP_REFER) +
                    getContext().getString(R.string.SOP_LTE_RRMcon));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        }*/ else if (key.equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON_LTE)){ 
            setTitle(R.string.title_integrity_check_off);
            setNote(getContext().getString(R.string.note_integrity_check_off) + 
                getContext().getString(R.string.SOP_REFER) + 
                getContext().getString(R.string.SOP_TDS_RRMcon) +
                getContext().getString(R.string.SOP_TDS_PerfCon));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
        
        mPhone = PhoneFactory.getDefaultPhone();
         if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        }
    }
    
    private void checkNetworkType() {
        CTSCLog.i(TAG, "checkNetworkType");
        mPhone.getPreferredNetworkType(mModemATHander
                .obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));
    }

    public boolean onCheck() {
        checkNetworkType();
        getCTAFTA();
        return true;
    }

    public check_result getCheckResult() {
        /*
         * implement check function here
         */
       if (!mAsyncDone) {
            mResult = check_result.UNKNOWN;
            mNeedNofity = true;
            setValue(R.string.ctsc_querying);
            return mResult;
        }
        mNeedNofity = false;
        CTSCLog.i(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    }

    public boolean onReset() {
        /*
         * implement your reset function here
         */
        CTSCLog.i(TAG, "onResult");
        if (!isConfigurable()) {
            //On no, this instance is check only, not allow auto config.
            return false;
        }
        if (getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_OFF)) {
            setCTAFTA("0");
        } else if(getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON)
                || getKey().equals(CheckItemKeySet.CI_CTAFTA_CONFIG_ON_LTE)) {
            setCTAFTA("1");
        }/* else if(getKey().equals(CheckItemKeySet.CI_MODEM_TEST_FTA_ON)) {
            if (mModemFlag) {
                writePreferred(NETWORK_TYPE);
                mPhone.setPreferredNetworkType(
                                NETWORK_TYPE,
                                mModemATHander
                                        .obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE));
            }
            setCTAFTA("2," + String.valueOf(0));
            enableIPO(false);
            setGprsTransferType(PCH_DATA_PREFER);
        }*/
        return true;
    }

    private void enableIPO(boolean value) {
        CTSCLog.v(TAG, value ? "enableIOP(true)" : "enableIPO(false)");
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.IPO_SETTING, value ? IPO_ENABLE : IPO_DISABLE);
    }
    
    /*
    private void setGprsTransferType(int type) {
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING, type);
            if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
                if (mPhone != null) {
                    PhoneFactory.getPhone(PhoneConstants.SIM_ID_1).setGprsTransferType(type, null);
                    PhoneFactory.getPhone(PhoneConstants.SIM_ID_2).setGprsTransferType(type, null);
                }
            } else {
                if (telephony != null) {
                    telephony.setGprsTransferType(type);
                }
            }
        } catch (RemoteException e) {
            CTSCLog.v(TAG, e.getMessage());
        }
    }*/
    
    private void writePreferred(int type) {
        SharedPreferences sh = getContext().getSharedPreferences("RATMode",
                Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = sh.edit();
        editor.putInt("ModeType", type);
        editor.commit();
    }
   
    private void getCTAFTA() {
        String cmd[] = new String[2];
        cmd[0] = "AT+EPCT?";
        cmd[1] = "+EPCT:";
        CTSCLog.i(TAG, "getCTAFTA");
        mAsyncDone = false;
        Phone mPhone = PhoneFactory.getDefaultPhone();
        mPhone.invokeOemRilRequestStrings(cmd, mModemATHander
                .obtainMessage(MODEM_QUERY));
    }

    private void setCTAFTA(String str) {
        Phone mPhone = PhoneFactory.getDefaultPhone();
        String cmd[] = new String[2];
        cmd[0] = "AT+EPCT=" + str;
        cmd[1] = "";
        CTSCLog.i(TAG, "setCTAFTA");
        mPhone.invokeOemRilRequestStrings(cmd, mSetModemATHander
                .obtainMessage());
    }
}

class CheckDataConnect extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckDataConnect";
    private boolean mhasSim = true;
    private boolean mReseting = false;
    
    CheckDataConnect(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_Data_Connection);

        if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)) {
            setNote(getContext().getString(R.string.note_DC_off) +
                getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_ON)) {
            setNote(getContext().getString(R.string.note_DC_on) +
                getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_IOT));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_ON_DM)) {
            setNote(getContext().getString(R.string.note_DC_on) +
                    getContext().getString(R.string.SOP_REFER) +
                    getContext().getString(R.string.SOP_DM));
                setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG)) {
            setNote(getContext().getString(R.string.note_DC_off) +
                getContext().getString(R.string.SOP_REFER) +
                getContext().getString(R.string.SOP_Protocol));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_CHECK)) {
            if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {// default is off
                setNote(getContext().getString(R.string.note_DC_off) +
                    getContext().getString(R.string.SOP_REFER) +
                    getContext().getString(R.string.SOP_Protocol));
            } else {// default is on
                setNote(getContext().getString(R.string.note_DC_on) + 
                getContext().getString(R.string.SOP_REFER) + 
                getContext().getString(R.string.SOP_Protocol));
            }
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE)) {
            setNote(getContext().getString(R.string.note_DC_off) + 
                    getContext().getString(R.string.SOP_REFER) + 
                    getContext().getString(R.string.SOP_TDS_RRMcon) + 
                    getContext().getString(R.string.SOP_TDS_PerfCon) +
                    getContext().getString(R.string.SOP_LTE_RRMcon));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE)) {
                setNote(getContext().getString(R.string.note_DC_on) + 
                getContext().getString(R.string.SOP_REFER) + 
                    getContext().getString(R.string.SOP_LTE_NV_IOT));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }

        List<SubscriptionInfo> mSimList = SubscriptionManager.from(c).getActiveSubscriptionInfoList();
        if (mSimList==null || mSimList.size() == 0) {         
            setProperty(PROPERTY_AUTO_CHECK);
            setValue(R.string.value_SIM);
            mResult = check_result.UNKNOWN;
            mhasSim = false;
        }else {
            CTSCLog.i(TAG, "mSimList size : "+mSimList.size());
        }
    }

    public boolean onCheck() {
        CTSCLog.d(TAG, "OnCheck mHasSim = " + mhasSim);
        if (mReseting) {
            setValue(R.string.ctsc_querying);
            return true;
        }
        if (mhasSim) {
//            ConnectivityManager cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            
            boolean dataEnable = TelephonyManager.getDefault().getDataEnabled();            
            if (!dataEnable) {               
                setValue(R.string.value_DC_off);
                if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_DM)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE)) {
                    mResult = check_result.WRONG;
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF) 
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE)) {
                    mResult = check_result.RIGHT;
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_CHECK)) {
                    //if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {

                    mResult = check_result.RIGHT;
                    //} else {
                    //    mResult = check_result.WRONG;
                    //}
                }
            } else {  
                setValue(R.string.value_DC_on);
                if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_DM)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE)) {
                    mResult = check_result.RIGHT;
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE)) {
                    mResult = check_result.WRONG;
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_CHECK)) {
                    //if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {

                    mResult = check_result.WRONG;
                    //} else {
                    //    mResult = check_result.RIGHT;
                    //}
                }
            }
            CTSCLog.i(TAG, "onCheck data enable = " +dataEnable + " mResult = " + mResult);
        }

        return true;
    }

    
    public check_result getCheckResult() {
        /*
         * implement check function here
         */ 
        CTSCLog.i(TAG, "getCheckResult mResult = " + mResult);
        return mResult;
    } 
    
    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);        
        return (subId != null) ? subId[0] : SubscriptionManager.getDefaultSubId();
    }

    public boolean onReset() {
        /*
         * implement your reset function here
         */
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            //On no, this instance is check only, not allow auto config.
            return false;
        }
//        ConnectivityManager cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG)
                || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE)) {
            TelephonyManager.getDefault().setDataEnabled(false);
            setValue(R.string.value_DC_off);
        } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)
                || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_DM)
                || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE)) {
            /*if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
                /*Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
                List<SubscriptionInfo> mSimList = SubscriptionManager.getActiveSubscriptionInfoList();
                CTSCLog.i(TAG, "mSimList size : " + mSimList.size());
                int mSimSlot = mSimList.get(0).getSimSlotIndex();
                int mSubId = getSubIdBySlot(mSimSlot);
                intent.putExtra(SubscriptionManager._ID, mSubId); 
                getContext().sendBroadcast(intent);*/
            //} else {
                  TelephonyManager.getDefault().setDataEnabled(true);
            //}
            
            setValue(R.string.value_DC_on);
        }         
//                mResult = check_result.RIGHT;
        mReseting = true;
        new Handler().postDelayed(new Runnable() {
             public void run() {
                CTSCLog.d(TAG, "data connect send set refresh");
                sendBroadcast();
                mReseting = false;
                mResult = check_result.RIGHT;
                if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_OFF_CONFIG_LTE)) {           
                    setValue(R.string.value_DC_off);
                } else if (getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_DM)
                        || getKey().equals(CheckItemKeySet.CI_DATA_CONNECT_ON_CONFIG_LTE)) {
                    setValue(R.string.value_DC_on);
                }
           }
        }, 4000);
        
        return true;
    }
}

class CheckDataRoam extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckDataRoam";
    private boolean mhasSim = true;
    private Context mContext;
    
    CheckDataRoam(Context c, String key) {
        super(c, key);
        mContext = c;
        setTitle(R.string.title_Data_ROAM);
        StringBuilder note = new StringBuilder();

        if (key.equals(CheckItemKeySet.CI_DATA_ROAM)) {
            note.append(getContext().getString(R.string.note_DR_on))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_IOT));
            setProperty(PROPERTY_AUTO_CHECK);
            
        } else if (key.equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG)
                || key.equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE)) {
            note.append(getContext().getString(R.string.note_DR_on));
            if (key.equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG)) {
                note.append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_IOT));
            } else {
                note.append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_LTE_NV_IOT))
                    .append(getContext().getString(R.string.SOP_LTE_NS_IOT));
            }
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else if (key.equals(CheckItemKeySet.CI_DATA_ROAM_OFF_CONFIG)) {
            note.append(getContext().getString(R.string.note_DR_off));
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }

        setNote(note.toString());

        List<SubscriptionInfo> mSimList = SubscriptionManager.from(c).getActiveSubscriptionInfoList();
        if (mSimList == null || mSimList.size() == 0) {            
            setProperty(PROPERTY_AUTO_CHECK);
            setValue(R.string.value_SIM);
            mResult = check_result.UNKNOWN;
            mhasSim = false;
        }else {
            CTSCLog.i(TAG, "mSimList size : "+mSimList.size());
        } 
    }
    
    private Phone getDefaultDataPhone() {
        int defaultDataPhoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultSubId());
        Phone phone = PhoneFactory.getPhone(defaultDataPhoneId);
        if (phone == null) {
            phone = PhoneFactory.getPhone(0);
        }
        return phone;
    }     

    public boolean onCheck() {
        if (mhasSim) {
        Phone mPhone =  getDefaultDataPhone();
        boolean dataRoamEnable = mPhone.getDataRoamingEnabled();   
        if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM)
                || getKey().equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG)
                || getKey().equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE)) {
            if (!dataRoamEnable) { 
                mResult = check_result.WRONG;
                setValue(R.string.value_DR_off);
            } else { 
                mResult = check_result.RIGHT;
                setValue(R.string.value_DR_on);
            }                
        } else if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM_OFF_CONFIG)) {
            if (!dataRoamEnable) { 
                setValue(R.string.value_DR_off);
                mResult = check_result.RIGHT;
            } else { 
                mResult = check_result.WRONG;
                setValue(R.string.value_DR_on);
            }         
        }  
        
        CTSCLog.d(TAG, "data roam Enable = " + dataRoamEnable + " mResult = " + mResult);
        }        
        return true;
    }

    public check_result getCheckResult() {
        /*
         * implement check function here
         */ 
        CTSCLog.d(TAG, "get check result mResult = " + mResult);
        return mResult;
    } 

    public boolean onReset() {
        /*
         * implement your reset function here
         */
        CTSCLog.i(TAG, "onReset");
        if (!isConfigurable()) {
            //On no, this instance is check only, not allow auto config.
            return false;
        }

        List<SubscriptionInfo> mSimList = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG)
                || getKey().equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG_LTE)) {

            if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
                /*try {
                    TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
                    if (telephonyManagerEx != null) {
                        telephonyManagerEx.setDataRoamingEnabled(true, mSimList.get(0).getSimSlotIndex());
                    }
                } catch (RemoteException e) {
                    CTSCLog.d(TAG,"iTelephony exception");                        
                }*/
                Phone mPhone =  getDefaultDataPhone();
                mPhone.setDataRoamingEnabled(true);
                
            } else {
                Phone mPhone =  PhoneFactory.getDefaultPhone();            
                mPhone.setDataRoamingEnabled(true);
            }
            setValue(R.string.value_DR_on);
        } else if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM_OFF_CONFIG)) {
            Phone mPhone =  getDefaultDataPhone();
            mPhone.setDataRoamingEnabled(false);
            /*
            if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
                try {
                    TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
                    if (telephonyManagerEx != null) {
                        telephonyManagerEx.setDataRoamingEnabled(
                                false, mSimList.get(0).slotId);
                    }
                } catch (RemoteException e) {
                    CTSCLog.d(TAG,"iTelephony exception");                     
                }
                
            } else {
                Phone mPhone =  PhoneFactory.getDefaultPhone();
                mPhone.setDataRoamingEnabled(false);
            }*/
            setValue(R.string.value_DR_off);
        }
        mResult = check_result.RIGHT;
        return true;
    }
//
//
//    public boolean onCheck() {
//        if (mhasSim) {
//        Phone mPhone =  PhoneFactory.getDefaultPhone();
//        boolean dataRoamEnable = mPhone.getDataRoamingEnabled();
//        if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM) ||
//            getKey().equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG)) {
//            if (!dataRoamEnable) {
//                mResult = check_result.WRONG;
//                setValue(R.string.value_DR_off);
//            } else {
//                mResult = check_result.RIGHT;
//                setValue(R.string.value_DR_on);
//            }
//        } else if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM_OFF_CONFIG)) {
//            if (!dataRoamEnable) {
//                setValue(R.string.value_DR_off);
//                mResult = check_result.RIGHT;
//            } else {
//                mResult = check_result.WRONG;
//                setValue(R.string.value_DR_on);
//            }
//        }
//
//        CTSCLog.d(TAG, "data roam Enable = " + dataRoamEnable + " mResult = " + mResult);
//        }
//        return true;
//    }
//
//    public check_result getCheckResult() {
//        /*
//         * implement check function here
//         */
//        CTSCLog.d(TAG, "get check result mResult = " + mResult);
//        return mResult;
//    }
//
//    public boolean onReset() {
//        /*
//         * implement your reset function here
//         */
//        CTSCLog.i(TAG, "onReset");
//        if (!isConfigurable()) {
//            //On no, this instance is check only, not allow auto config.
//            return false;
//        }
//
//        List<SimInfoRecord> mSimList = SimInfoManager.getInsertedSimInfoList(getContext());
//        if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM_CONFIG)) {
//
//            if (PhoneConstants.GEMINI_SIM_NUM > 1) {
//                try {
//                    TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
//                    if (telephonyManagerEx != null) {
//                        telephonyManagerEx.setDataRoamingEnabled(true, mSimList.get(0).mSimSlotId);
//                    }
//                } catch (RemoteException e) {
//                    CTSCLog.d(TAG,"iTelephony exception");
//                }
//
//            } else {
//                Phone mPhone =  PhoneFactory.getDefaultPhone();
//                mPhone.setDataRoamingEnabled(true);
//            }
//            setValue(R.string.value_DR_on);
//        } else if (getKey().equals(CheckItemKeySet.CI_DATA_ROAM_OFF_CONFIG)) {
//            if (PhoneConstants.GEMINI_SIM_NUM > 1) {
//                try {
//                    TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
//                    if (telephonyManagerEx != null) {
//                        telephonyManagerEx.setDataRoamingEnabled(
//                                false, mSimList.get(0).mSimSlotId);
//                    }
//                } catch (RemoteException e) {
//                    CTSCLog.d(TAG,"iTelephony exception");
//                }
//
//            } else {
//                Phone mPhone =  PhoneFactory.getDefaultPhone();
//                mPhone.setDataRoamingEnabled(false);
//            }
//            setValue(R.string.value_DR_off);
//        }
//        mResult = check_result.RIGHT;
//        return true;
//    }
}

class CheckPLMN extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckPLMN";
    private boolean mAsyncDone = true;
    private boolean mNeedNofity = false;
    private Context mContext;

    private final Handler mNetworkSelectionModeHandler = new Handler() {
        public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg form network slection mode");
           
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int auto = ((int[]) ar.result)[0];
                CTSCLog.d(TAG, "Get Selection Type " + auto);
                if(auto == 0) {
                    setValue(R.string.value_PLMN_auto_select);
                    mResult = check_result.RIGHT;
                } else {
                    setValue(R.string.value_PLMN_manual_select);
                    mResult = check_result.WRONG;
                }   
            } else {
                setValue("PLMN Query failed");
                mResult = check_result.UNKNOWN;
            }
            mAsyncDone = true;
            if (mNeedNofity) {
                sendBroadcast();
            }                
        }
    };

    
    private final Handler mSetNetworkSelectionModeHander = new Handler() {
        public final void handleMessage(Message msg) {
            CTSCLog.i(TAG, "Receive msg form Mode set");  
            if(getKey().equals(CheckItemKeySet.CI_PLMN_DEFAULT_CONFIG)) {
                setValue(R.string.value_PLMN_auto_select);
            }            
            mResult = check_result.RIGHT;
            sendBroadcast();
        }
    };
     
    CheckPLMN(Context c, String key) {
        super(c, key);
        mContext = c;
        if (key.equals(CheckItemKeySet.CI_PLMN_DEFAULT)) {
            setTitle(R.string.title_PLMN);
            setProperty(PROPERTY_AUTO_CHECK);
        } else {
            setTitle(R.string.title_PLMN);
            setProperty(PROPERTY_AUTO_CHECK|PROPERTY_AUTO_CONFG);
        }
        setNote(getContext().getString(R.string.note_PLMN_auto_select) +
            getContext().getString(R.string.SOP_REFER) +
            getContext().getString(R.string.SOP_Protocol));
    }

    public boolean onCheck() {
        List<SubscriptionInfo> mSimList = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();


        if (mSimList == null || mSimList.size() == 0) {
            setProperty(PROPERTY_AUTO_CHECK);
            setValue(R.string.value_SIM);
            mResult = check_result.UNKNOWN;
        } else {
            getNetworkSelectionMode();
            CTSCLog.i(TAG, "mSimList size : "+mSimList.size());
        }
        return true;
    }


    public check_result getCheckResult() {
        /*
         * implement check function here
         */ 
        CTSCLog.i(TAG, "getCheckResult");
        if (!mAsyncDone) {
             mResult = check_result.UNKNOWN;
             mNeedNofity = true;
             setValue(R.string.ctsc_querying);
             return mResult;
        }
        mNeedNofity = false;
        return mResult;
    } 

    @Override
    public boolean onReset() {
        if (!isConfigurable()) {
            //On no, this instance is check only, not allow auto config.
            return false;
        }
        setNetWorkSelectionMode();
        return true;
    }

    public void getNetworkSelectionMode() {
        CTSCLog.i(TAG, "getNetworkSelectionMode");
        Phone mPhone = null;
        mAsyncDone = false;
        mPhone = PhoneFactory.getDefaultPhone();
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        }
        ((PhoneBase)((PhoneProxy)mPhone).getActivePhone()).mCi.
            getNetworkSelectionMode(mNetworkSelectionModeHandler.obtainMessage());
     }

     private void setNetWorkSelectionMode() {
         Phone mPhone = null;
          mPhone = PhoneFactory.getDefaultPhone();
         if (TelephonyManager.getDefault().getPhoneCount() > 1) {
             mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
         }
         ((PhoneBase)((PhoneProxy)mPhone).getActivePhone()).mCi.
             setNetworkSelectionModeAutomatic(mSetNetworkSelectionModeHander.obtainMessage());
     }
}

class CheckDTSUPPORT extends CheckItemBase {

    private static final String TAG = " ProtocolItem CheckDTSUPPORT";

    CheckDTSUPPORT(Context c, String key) {
        super(c, key);
        setTitle(R.string.title_GEMINI_SUPPORT);
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true &&  SystemProperties.get("ro.mtk_dt_support").equals("1") == true) {
            setValue(R.string.value_GEMINI_DT_SUPPORT);
        } else if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true &&  SystemProperties.get("ro.mtk_dt_support").equals("1") == false) {
            setValue(R.string.value_GEMINI_SINGLE_SUPPORT);     
        } else if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == false) {
            setValue(R.string.value_SINGLE_SD_SUPPORT);
        }
        setNote(getContext().getString(R.string.note_GEMINI_SUPPORT) +
            getContext().getString(R.string.SOP_REFER) +
            getContext().getString(R.string.SOP_FieldTest));
    }
}

class CheckSIMSlot extends CheckItemBase {
    private static final String TAG = " ProtocolItem CheckSIMSlot";
    private boolean mNoSim = false;
    private Context mContext;
 
    
    CheckSIMSlot(Context c, String key) {
        super(c, key);
        mContext = c;

        List<SubscriptionInfo> mSimList = SubscriptionManager.from(c).getActiveSubscriptionInfoList();
        if (mSimList != null) {
            CTSCLog.i(TAG, "mSimList size : " + mSimList.size());
        }
        setTitle(R.string.title_SIM_STATE);
        if (key.equals(CheckItemKeySet.CI_DUAL_SIM_CHECK)) {
            setNote(getContext().getString(R.string.SOP_REFER) + 
                getContext().getString(R.string.SOP_FieldTest));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_SIM_3G_CHECK)) {         
            setNote(getContext().getString(R.string.note_SIM_3G_CHECK) + 
                getContext().getString(R.string.SOP_REFER) + 
                getContext().getString(R.string.SOP_LOCAL_TEST));
            setProperty(PROPERTY_AUTO_CHECK);
        } else if (key.equals(CheckItemKeySet.CI_LAB_4G_USIM)) {
            setNote(getContext().getString(R.string.note_lab_4g_USIM) +
                    getContext().getString(R.string.SOP_REFER) +
                    getContext().getString(R.string.SOP_LTE_NV_IOT));
            setProperty(PROPERTY_CLEAR);
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
    }

    private int getCapabilitySim() {
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        ITelephony iTelephony =
                ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        CTSCLog.d(TAG, "getSimCount: " + telephonyManager.getSimCount());
        if (iTelephony == null || telephonyManager == null
                || telephonyManager.getSimCount() <= 1) {
            return PhoneConstants.SIM_ID_1;
        }

        for (int i = 0; i < telephonyManager.getSimCount(); i++) {
            try {
                CTSCLog.d(TAG, "get 3G capability: " + iTelephony.getRadioAccessFamily(i));
                if ((iTelephony.getRadioAccessFamily(i) & (RadioAccessFamily.RAF_UMTS
                        | RadioAccessFamily.RAF_LTE)) > 0) {
                    CTSCLog.d(TAG, "get 3G capability Sim: " + i);
                    return i;
                }
            } catch (RemoteException e) {
                CTSCLog.e(TAG, e.getMessage());
            }
        }
        return PhoneConstants.SIM_ID_1;
    }
    
    public boolean onCheck() {
        List<SubscriptionInfo> mSimList = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        if (mSimList == null) {
            if (getKey().equals(CheckItemKeySet.CI_DUAL_SIM_CHECK)) {
                setValue(R.string.value_SIM);
                mResult = check_result.WRONG;
            }
            if (getKey().equals(CheckItemKeySet.CI_SIM_3G_CHECK)) {
                setValue(getContext().getString(R.string.note_SIM_3G_CHECK));
                mResult = check_result.WRONG; 
            }
            return true;
        }
        CTSCLog.i(TAG, "mSimList size : " + mSimList.size());

        if (getKey().equals(CheckItemKeySet.CI_DUAL_SIM_CHECK)) {
            if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
                if (mSimList.size() == 2) {
                    setValue(R.string.value_DUAL_SIM_STATE);
                    mResult = check_result.RIGHT;
                } else {
                    setValue(R.string.note_DUAL_SIM_STATE);
                    mResult = check_result.WRONG;
                }            
            } else {
                if (mSimList.size() == 0) {            
                    setValue(R.string.value_SIM);
                    mResult = check_result.WRONG;
                } else {
                    setValue(R.string.value_SIM_STATE);
                    mResult = check_result.RIGHT;
                }
            }
        } else if (getKey().equals(CheckItemKeySet.CI_SIM_3G_CHECK)) {
            if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true && mSimList.size() == 2) {
                setValue(getContext().getString(R.string.note_SINGLE_SIM_CHECK));
                mResult = check_result.WRONG;
            } else if (mSimList.size() == 0) {
                setValue(getContext().getString(R.string.note_SIM_3G_CHECK));
                mResult = check_result.WRONG;
            } else {
                int slot3G = 0;
                slot3G = getCapabilitySim();                 
                CTSCLog.d(TAG, "is3G slot = " + slot3G);
                if (mSimList.get(0).getSimSlotIndex() == slot3G) {
                    
                    CTSCLog.d(TAG, "is3G result = true");
                    setValue(getContext().getString(R.string.value_SIM_3G));
                    mResult = check_result.RIGHT;
                } else {
                    setValue(getContext().getString(R.string.note_SIM_3G_CHECK));
                    mResult = check_result.WRONG;
                }
            }            
        } else if (getKey().equals(CheckItemKeySet.CI_LAB_4G_USIM)) {
            if (mSimList.size() == 0) {
                setValue(getContext().getString(R.string.note_lab_4g_USIM));
                mNoSim = true;
        }
    }

        return true;
    } 
    
    public check_result getCheckResult() {
        if (getKey().equals(CheckItemKeySet.CI_LAB_4G_USIM)) {
            check_result result = super.getCheckResult();

            if (mNoSim && result == check_result.UNKNOWN) {
                mResult = check_result.WRONG;
            }                
        }

        return mResult;
    }
}

class CheckModemSwitch extends CheckItemBase {
    private static final String TAG = "CheckModemSwitch";
    private AlertDialog mAlertDialog = null;
    private static IWorldPhone sWorldPhone = null;
    //private static LteDcConfigHandler sLteDcConfigHandler = null;
    private static int sProjectType;
    private static final int PROJ_TYPE_NOT_SUPPORT = 0;
    private static final int PROJ_TYPE_WORLD_PHONE = 1;
    private static final int PROJ_TYPE_SGLTE_LTTG  = 2;

    CheckModemSwitch(Context c, String key) {
        super(c, key);
        setTitle(R.string.modem_switch_title);
        StringBuilder note = new StringBuilder();

        if (WorldPhoneUtil.isWorldPhoneSupport()) {
            sWorldPhone = PhoneFactory.getWorldPhone();
            sProjectType = PROJ_TYPE_WORLD_PHONE;
        } else if (WorldPhoneUtil.isLteSupport()) {
            sWorldPhone = PhoneFactory.getWorldPhone();
            sProjectType = PROJ_TYPE_SGLTE_LTTG;
        } else {
            sProjectType = PROJ_TYPE_NOT_SUPPORT;
        }
        if (isSupportSwitch(note)) {
            setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        } else {
            setProperty(PROPERTY_AUTO_CHECK);
        }
        
        if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD)) {
            note.append(getContext().getString(R.string.modem_switch_note))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_Protocol))
                .append(getContext().getString(R.string.SOP_PhoneCard));
            
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TG)) {
            note.append(getContext().getString(R.string.modem_switch_tg_note))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_PhoneCard))
                .append(getContext().getString(R.string.SOP_TDS_RRMcon))
                .append(getContext().getString(R.string.SOP_TDS_PerfCon))
                .append(getContext().getString(R.string.SOP_LTE_RF));
            
        } /*else if (getKey().equals(CheckItemKeySet.CI_MS_TDD_CASE_SGLTE)
                || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_SGLTE)) {
            if (getKey().equals(CheckItemKeySet.CI_MS_TDD_CASE_SGLTE)) {
                note.append(getContext().getString(R.string.modem_switch_tdd_case_note));
            }
            note.append(getContext().getString(R.string.modem_switch_sglte_note))
                .append(getContext().getString(R.string.SOP_REFER))
                .append(getContext().getString(R.string.SOP_LTE_NS_IOT))
                .append(getContext().getString(R.string.SOP_LTE_NV_IOT))
                .append(getContext().getString(R.string.SOP_LTE_UICC_USIM))
                .append(getContext().getString(R.string.SOP_LTE_RRMcon))
                .append(getContext().getString(R.string.SOP_LTE_PCT))
                .append(getContext().getString(R.string.SOP_LTE_RF));
            
        }*/ else if (getKey().equals(CheckItemKeySet.CI_MS_FDD_CASE_FDD_CSFB)
                || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB)){
            if (getKey().equals(CheckItemKeySet.CI_MS_FDD_CASE_FDD_CSFB)) {
                note.append(getContext().getString(R.string.modem_switch_fdd_case_note));
            }
            note.append(getContext().getString(R.string.modem_switch_fdd_csfb_note))
                    .append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_LTE_UICC_USIM))
                    .append(getContext().getString(R.string.SOP_LTE_RRMcon))
                    .append(getContext().getString(R.string.SOP_LTE_PCT))
                    .append(getContext().getString(R.string.SOP_LTE_RF));

        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB)){            
            note.append(getContext().getString(R.string.modem_switch_tdd_csfb_note))
                    .append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_TDS_PerfCon))
                    .append(getContext().getString(R.string.SOP_LTE_NS_IOT))
                    .append(getContext().getString(R.string.SOP_LTE_NV_IOT))
                    .append(getContext().getString(R.string.SOP_LTE_UICC_USIM))
                    .append(getContext().getString(R.string.SOP_LTE_RRMcon))
                    .append(getContext().getString(R.string.SOP_LTE_PCT))
                    .append(getContext().getString(R.string.SOP_LTE_RF));
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_AUTO)) {
            note.append(getContext().getString(R.string.modem_switch_auto_note))
                    .append(getContext().getString(R.string.SOP_REFER))
                    .append(getContext().getString(R.string.SOP_Protocol));
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
        setNote(note.toString());
    }

    public boolean onCheck() {
        CTSCLog.d(TAG, " oncheck");
        int airplaneMode = Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        if (airplaneMode == 1) {
            setProperty(PROPERTY_AUTO_CHECK);
            CTSCLog.d(TAG, "Modem switch is not allowed in flight mode");
        }

        mResult = check_result.WRONG;
        int modemType = ModemSwitchHandler.getActiveModemType();
        CTSCLog.d(TAG, "Get modem type: " + modemType);

        if (modemType == ModemSwitchHandler.MD_TYPE_WG) {
            setValue(R.string.modem_switch_wg);
        } else if (modemType == ModemSwitchHandler.MD_TYPE_TG) {
            setValue(R.string.modem_switch_tg);
            if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TG)) {
                mResult = check_result.RIGHT;
            }
        } else if (modemType == ModemSwitchHandler.MD_TYPE_LWG) {
            setValue(R.string.modem_switch_fdd_csfb);
            if (getKey().equals(CheckItemKeySet.CI_MS_FDD_CASE_FDD_CSFB)
                    || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB)) {
                mResult = check_result.RIGHT;
            }
        } else if (modemType == ModemSwitchHandler.MD_TYPE_LTG) {
            setValue(R.string.modem_switch_tdd_csfb);
            if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB)) {
                mResult = check_result.RIGHT;
            }
        } else if (modemType == 0) {
            setValue(R.string.modem_switch_auto);
            if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_AUTO)) {
                mResult = check_result.RIGHT;
            }
        } else {
            setValue(R.string.ctsc_error);
            CTSCLog.e(TAG, "Query Modem type failed: " + modemType);
        }

        if (Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.WORLD_PHONE_AUTO_SELECT_MODE, 1) == 1) {
            setValue(R.string.modem_switch_auto);
            if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_AUTO)) {
                mResult = check_result.RIGHT;
            } else {
                mResult = check_result.WRONG;
            }
        }

        return true;
    }

    private boolean isSupportSwitch(StringBuilder note) {
        boolean bTdd = false;//forward compatibility for none-lte phone, Tdd Modem is none-lte 
        
        boolean bFddCsfb = true;
        boolean bTddCsfb = true;
        boolean bSglte = true;
        boolean bTg = true;
        boolean bWg = true;
        String optr = SystemProperties.get("ro.operator.optr");
        if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
            if (WorldPhoneUtil.isLteSupport()) {
                bWg = false;
                bTg = false;
                bSglte = false;                
            } else {
                bFddCsfb = false;
                bTddCsfb = false;
                bSglte = false;
            }
        } else if (sProjectType == PROJ_TYPE_SGLTE_LTTG) {
            bWg = false;
            bFddCsfb = false;
            bTddCsfb = false;
        } else if (sProjectType == PROJ_TYPE_NOT_SUPPORT) {
            bWg = false;
            bTg = false;
            bFddCsfb = false;
            bTddCsfb = false;
            bSglte = false;
        }
        
        if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD)) {
            //forward compatibility for none-lte phone, Tdd Modem is none-lte
            if (!bTdd) {
                note.append(getContext().getString(R.string.modem_switch_not_support))
                    .append(getContext().getString(R.string.modem_switch_tdd))
                    .append("\n\n");
                return false;
            }
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TG)) {
            if (!bTg) {
                note.append(getContext().getString(R.string.modem_switch_not_support))
                    .append(getContext().getString(R.string.modem_switch_tg))
                    .append("\n\n");
                return false;
            }
        }/* else if (getKey().equals(CheckItemKeySet.CI_MS_TDD_CASE_SGLTE)
                || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_SGLTE)) {
            if (!bSglte) {
                note.append(getContext().getString(R.string.modem_switch_not_support))
                    .append(getContext().getString(R.string.modem_switch_mmdc))
                    .append("\n\n");
                return false;
            }
        }*/ else if (getKey().equals(CheckItemKeySet.CI_MS_FDD_CASE_FDD_CSFB)
                || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB)){
            if (!bFddCsfb) {
                note.append(getContext().getString(R.string.modem_switch_not_support))
                    .append(getContext().getString(R.string.modem_switch_fdd_csfb))
                    .append("\n\n");
                return false;
            }
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB)){
            if (!bTddCsfb) {
                note.append(getContext().getString(R.string.modem_switch_not_support))
                    .append(getContext().getString(R.string.modem_switch_tdd_csfb))
                    .append("\n\n");
                return false;
            }
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_AUTO)) {
            return true;
        } else {
            throw new IllegalArgumentException("Error key = " + getKey());
        }
        
        return true;
    }

    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        int oldMdType = ModemSwitchHandler.getActiveModemType();
        
        if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD)) {
            sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_TDD);
            if (oldMdType == ModemSwitchHandler.MD_TYPE_FDD) {
                ModemSwitchHandler.switchModem(ModemSwitchHandler.MD_TYPE_TDD);
            }
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TG)) {
            if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_TG);
                if (!WorldPhoneUtil.isLteSupport()) {
                    if (oldMdType != ModemSwitchHandler.MD_TYPE_TDD) {
                        ModemSwitchHandler.switchModem(ModemSwitchHandler.MD_TYPE_TDD);
                    }
                }
            } else if (sProjectType == PROJ_TYPE_SGLTE_LTTG) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_TG);
            }
         //else if (getKey().equals(CheckItemKeySet.CI_MS_TDD_CASE_SGLTE)
           //     || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_SGLTE)) {
           
        } else if (getKey().equals(CheckItemKeySet.CI_MS_FDD_CASE_FDD_CSFB)
                || getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_FDD_CSFB)){
            sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_LWG);
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB)){
            sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_LTG);
        } else if (getKey().equals(CheckItemKeySet.CI_MODEM_SWITCH_AUTO)){
            sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_AUTO, 0);
        }
        
        switchModemAlert(10000, 1000);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                CTSCLog.d(TAG, "wait 1s for setting successfully");
                sendBroadcast(); 
                mResult = check_result.RIGHT;
            }
        }, 13000);  
        return true;
    }
    
    private void switchModemAlert(long millisUntilFinished, long countDownInterval) {
        if (null == mAlertDialog) {
            mAlertDialog = new AlertDialog.Builder(getContext()).create();
        }
        mAlertDialog.setTitle("Switching Modem Mode");
        mAlertDialog.setMessage("Wait");
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();

        new CountDownTimer(millisUntilFinished, countDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
               mAlertDialog.setMessage("Wait " + (millisUntilFinished/1000) + " seconds");
            }

            @Override
            public void onFinish() {
                mAlertDialog.cancel();
            }
        }.start();
    }
}

/**
 * Both check ModemSwitch and NetworkMode.
 * Network Mode must be queried\config after Modem is switched ready
 */
class CheckMdSwitchAndNwMode extends CheckItemBase {
    private static final String TAG = "CheckMdSwitchAndNwMode";
    private CheckModemSwitch mModemSwitch = null;
    private CheckItemBase mNetworkMode = null;
    private boolean mIsWaiting = false;
    
    CheckMdSwitchAndNwMode(Context c, String key) {
        super(c, key);
        ConstructCheckItems();
        
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        setTitle(getContext().getString(R.string.modem_switch_title) 
                + " and "
                + getContext().getString(R.string.title_Network_Mode));
        
        StringBuilder note = new StringBuilder("");
        note.append(mModemSwitch.getNote())
            .append("\n\n");
        if (mNetworkMode != null) {
            note.append(mNetworkMode.getNote());
        }
        setNote(note.toString());
    }
    
    private void ConstructCheckItems() {
        if (getKey().equals(CheckItemKeySet.CI_TDD_And_TDWCDMA_ONLY)
                || getKey().equals(CheckItemKeySet.CI_TDD_And_DUAL_MODE)) {
            mModemSwitch = new CheckModemSwitch(getContext(), CheckItemKeySet.CI_MODEM_SWITCH_TDD);
        } else if (getKey().equals(CheckItemKeySet.CI_TG_And_3G_ONLY) 
                || getKey().equals(CheckItemKeySet.CI_TG_And_3G_2G)) {
            mModemSwitch = new CheckModemSwitch(getContext(), CheckItemKeySet.CI_MODEM_SWITCH_TG);
        } /*else if (getKey().equals(CheckItemKeySet.CI_SGLTE_And_4G_ONLY)
                || getKey().equals(CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G)
                || getKey().equals(CheckItemKeySet.CI_SGLTE_And_2G_ONLY)
                || getKey().equals(CheckItemKeySet.CI_SGLTE_And_3G_ONLY)) {
            mModemSwitch = new CheckModemSwitch(getContext(), CheckItemKeySet.CI_MODEM_SWITCH_SGLTE);
        }*/ else if (getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_GSM_TDS)
                || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_TDS_ONLY)
                || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_LTE_ONLY)
                || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_4g2g)
                || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto)
                || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_GSM_ONLY)) {            
            mModemSwitch = new CheckModemSwitch(getContext(), CheckItemKeySet.CI_MODEM_SWITCH_TDD_CSFB);
        } else if (getKey().equals(CheckItemKeySet.CI_FDDCSFB_And_4G3GAnd2G)
                || getKey().equals(CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto)) {
            mModemSwitch = new CheckModemSwitch(getContext(), CheckItemKeySet.CI_MS_FDD_CASE_FDD_CSFB);
        }
        
        if (SystemProperties.get("ro.mtk_lte_dc_support").equals("1") == true){
            if (getKey().equals(CheckItemKeySet.CI_TDD_And_TDWCDMA_ONLY)
                    || getKey().equals(CheckItemKeySet.CI_TG_And_3G_ONLY)
                    || getKey().equals(CheckItemKeySet.CI_SGLTE_And_3G_ONLY)) {
                mNetworkMode = new CheckLteNetworkMode(getContext(), CheckItemKeySet.CI_3G_ONLY_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_TG_And_3G_2G)
                    || getKey().equals(CheckItemKeySet.CI_TDD_And_DUAL_MODE)) {
                mNetworkMode = new CheckLteNetworkMode(getContext(), CheckItemKeySet.CI_3G_AND_2G_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_SGLTE_And_4G_ONLY)) {
                mNetworkMode = new CheckLteNetworkMode(getContext(), CheckItemKeySet.CI_4G_ONLY_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_SGLTE_And_4G3GAnd2G)
                    || getKey().equals(CheckItemKeySet.CI_FDDCSFB_And_4G3GAnd2G)) {
                mNetworkMode = new CheckLteNetworkMode(getContext(), CheckItemKeySet.CI_4G_3G_AND_2G);
            } else if (getKey().equals(CheckItemKeySet.CI_SGLTE_And_2G_ONLY)) {
                mNetworkMode = new CheckLteNetworkMode(getContext(), CheckItemKeySet.CI_2G_ONLY_CONFIG);
            }
        } else {
            if (getKey().equals(CheckItemKeySet.CI_TDD_And_TDWCDMA_ONLY)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_TDWCDMA_ONLY_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_TDD_And_DUAL_MODE)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_DUAL_MODE_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_TG_And_3G_ONLY)
                    || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_TDS_ONLY)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_LTE_TDS_ONLY_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_TG_And_3G_2G)
                    || getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_GSM_TDS)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_LTE_GSM_TDS_CONFIG);
            } else if (getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_LTE_ONLY)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_LTE_ONLY_CONFIG);
            } /*else if (getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_4g2g)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_4G2G_CONFIG);
            }*/ else if (getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_4G3G2GAuto)
                    || getKey().equals(CheckItemKeySet.CI_FDDCSFB_And_4G3G2GAuto)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_4G_3G_2G_Auto);
            } else if (getKey().equals(CheckItemKeySet.CI_TDDCSFB_And_GSM_ONLY)) {
                mNetworkMode = new CheckNetworkMode(getContext(), CheckItemKeySet.CI_GSM_ONLY_CONFIG);
            }
        }
    }

    public boolean onCheck() {
        CTSCLog.d(TAG, "oncheck");
        
        mModemSwitch.onCheck();
        if (!mModemSwitch.isConfigurable()) {
            setProperty(PROPERTY_AUTO_CHECK);
        }
        if (mNetworkMode != null) {
            mNetworkMode.onCheck();
        }
        return true;
    }
    
    public check_result getCheckResult() {
        mModemSwitch.getCheckResult();
        if (mNetworkMode != null) {
            mNetworkMode.getCheckResult();
        }
        
        StringBuilder value = new StringBuilder("");
        value.append("Modem:")
             .append(mModemSwitch.getValue())
             .append("\n");
        if (mNetworkMode != null) {
            value.append("Network:");
            if (mIsWaiting) {
                value.append(getContext().getString(R.string.modem_switch_wait_ready));
            } else {
                value.append(mNetworkMode.getValue());
            }
        }
        setValue(value.toString());
        
        mResult = check_result.WRONG;
        if (check_result.UNKNOWN == mModemSwitch.mResult) {
            mResult = check_result.UNKNOWN;
        }
        
        if (mNetworkMode != null 
              && check_result.UNKNOWN == mNetworkMode.mResult) {
            mResult = check_result.UNKNOWN;
        }
        if (check_result.RIGHT == mModemSwitch.mResult
                && mNetworkMode != null && check_result.RIGHT == mNetworkMode.mResult) {
            mResult = check_result.RIGHT;
        }
        CTSCLog.d(TAG, "mResult = " + mResult);
        return mResult;
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        if (check_result.RIGHT != mModemSwitch.mResult) {
            mModemSwitch.onReset();
            
            //if switch modem, so must set network again
            //it takes nearly 10s to switch modem successfully
            CTSCLog.d(TAG, "wait 10s to set Network...");
            mIsWaiting = true;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    CTSCLog.d(TAG, "10s pass, now to set Network");
                    mIsWaiting = false;
                    if (mNetworkMode != null) {
                        mNetworkMode.onReset();
                    }
                    sendBroadcast();
                }
            }, 20000);
            
        } else if (check_result.RIGHT != mNetworkMode.mResult) {
            if (mNetworkMode != null) {
                mNetworkMode.onReset();
            }
            sendBroadcast();
        }

        return true;
    }
}

class CheckUser2Root extends CheckItemBase {
    private static final String TAG = "CheckUser2Root";
    private static final String ANDROID_BUILD_VERSION = "ro.build.version.sdk";
    private static final int ANDROID_BUILD_ICS = 14;
    
    private static final String RO_ADB_SECURE = "ro.adb.secure";
    private static final String RO_SECURE = "ro.secure";
    private static final String RO_DEBUG = "ro.debuggable";
    private static final String USB_CONFIG = "persist.sys.usb.config";
    private static final String ATCI_USERMODE = "persist.service.atci.usermode";
    private static final String RO_BUILD_TYPE = "ro.build.type";
    
    private static final String[][] MODIFY_ITEMS = {
        // { item,        root_value,               user_value }
        { USB_CONFIG,     "none",                   null }, 
        { RO_SECURE,      "0",                      "1" },
        { RO_ADB_SECURE,  "0",                      "1" }, 
        { RO_DEBUG,       "1",                      "0" },
        { USB_CONFIG,     "mass_storage,adb,acm",   "mass_storage" },
        { ATCI_USERMODE,  "1",                      "0" }, };
    
    CheckUser2Root(Context c, String key) {
        super(c, key);
        
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        setTitle(R.string.user2root_title);
        if (getKey().equals(CheckItemKeySet.CI_USER2ROOT_ROOT)) {
            setNote(getContext().getString(R.string.user2root_note)
                    + getContext().getString(R.string.SOP_REFER)
                    + getContext().getString(R.string.SOP_LTE_NS_IOT));
        } else {
            throw new IllegalArgumentException("Error key = " + key);
        }
    }

    public boolean onCheck() {
        CTSCLog.d(TAG, "oncheck");
        boolean isRooted = true;
        for (int i = 1; i < MODIFY_ITEMS.length; i++) {
            CTSCLog.d(TAG, MODIFY_ITEMS[i][0] + ":" + SystemProperties.get(MODIFY_ITEMS[i][0]));
            if (5 == i) {
                continue;//no need to care atcid, because it has no relationship with root/user
            } else if (4 == i) {
                if (!SystemProperties.get(MODIFY_ITEMS[i][0]).contains("mass_storage")
                        || !SystemProperties.get(MODIFY_ITEMS[i][0]).contains("adb")
                        || !SystemProperties.get(MODIFY_ITEMS[i][0]).contains("acm")) {
                    isRooted = false;
                    break;
                }
            } else {
                if (!SystemProperties.get(MODIFY_ITEMS[i][0]).equalsIgnoreCase(MODIFY_ITEMS[i][1])) {
                    isRooted = false;
                    break;
                }
            }
        }
        
        if (isRooted) {
            setValue(R.string.root);
            if (getKey().equals(CheckItemKeySet.CI_USER2ROOT_ROOT)) {
                mResult = check_result.RIGHT;
            }
        } else {
            setValue(R.string.user);
            if (getKey().equals(CheckItemKeySet.CI_USER2ROOT_ROOT)) {
                mResult = check_result.WRONG;
            }
        }
        return true;
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        int sdkVersion = SystemProperties.getInt(ANDROID_BUILD_VERSION, 10);
        if (sdkVersion >= ANDROID_BUILD_ICS) {
            toRoot();
        } else {
            throw new IllegalArgumentException("User2Root is not support for current android version!");
        }
        return true;
    }
    
    private void toRoot() {
        for (int i = 0; i < MODIFY_ITEMS.length; i++) {
            SystemProperties.set(MODIFY_ITEMS[i][0], MODIFY_ITEMS[i][1]);
        }
        String type = SystemProperties.get(RO_BUILD_TYPE, "unknown");
        CTSCLog.v(TAG, "build type: " + type);
        if (!type.equals("eng")) {
            try {
                CTSCLog.v(TAG, "user2root start atcid-daemon-u");
                Process proc = Runtime.getRuntime().exec("start atcid-daemon-u");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class CheckUsbShareNet extends CheckItemBase {
    private static final String TAG = "CheckUsbShareNet";
    private ConnectivityManager mCm = null;
    private boolean mIsWaiting = false;
        
    CheckUsbShareNet(Context c, String key) {
        super(c, key);
        setTitle(R.string.UsbShareNet_title);
        setNote(getContext().getString(R.string.UsbShareNet_note)
                + getContext().getString(R.string.SOP_REFER)
                + getContext().getString(R.string.SOP_LTE_NS_IOT));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
        
        mCm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        if (mIsWaiting) {
            setValue(R.string.ctsc_querying);
            mResult = check_result.UNKNOWN;
        } else {
            String[] tethered = mCm.getTetheredIfaces();
            String[] usbRegexs = mCm.getTetherableUsbRegexs();
            
            boolean usbTethered = false;
            for (String s : tethered) {
                for (String regex : usbRegexs) {
                    if (s.matches(regex)) {
                        usbTethered = true;
                    }
                }
            }
            if (usbTethered) {
                setValue(R.string.UsbShareNet_yes);
                mResult = check_result.RIGHT;
            } else {
                setValue(R.string.UsbShareNet_no);
                mResult = check_result.WRONG;
            }
        }
        
        return mResult;
    }
    
    private void setUsbTethering(boolean enabled) {
        mCm.setUsbTethering(enabled);
        mIsWaiting = true;
        
        new Handler().postDelayed(new Runnable() {
            public void run() {
                CTSCLog.d(TAG, "wait 1s for setting successfully");
                sendBroadcast();
                mIsWaiting = false;
            }
        }, 6000);
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        setUsbTethering(true);
        return true;
    }
}

class CheckGPSState extends CheckItemBase {
    private static final String TAG = "CheckGPSState";
    private static final String MODE_CHANGING_ACTION =
            "com.android.settings.location.MODE_CHANGING";
    private static final String CURRENT_MODE_KEY = "CURRENT_MODE";
    private static final String NEW_MODE_KEY = "NEW_MODE";
    private int mCurrentMode;
        
    CheckGPSState(Context c, String key) {
        super(c, key);
        setTitle(R.string.GPS_title);
        setNote(c.getString(R.string.GPS_off_note)
                + c.getString(R.string.SOP_REFER)
                + c.getString(R.string.SOP_LTE_NS_IOT));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }
    
    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        mCurrentMode = Settings.Secure.getInt(getContext().getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);
        boolean enabled = (mCurrentMode != Settings.Secure.LOCATION_MODE_OFF);
        if (enabled) {
            setValue(R.string.ctsc_enabled);
            mResult = check_result.WRONG;
        } else {
            setValue(R.string.ctsc_disabled);
            mResult = check_result.RIGHT;
        }  
        return mResult;
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        setLocationMode(Settings.Secure.LOCATION_MODE_OFF);
        return true;
    }
    
    public void setLocationMode(int mode) {
        if (isRestricted()) {
            // Location toggling disabled by user restriction. Read the current location mode to
            // update the location master switch.
            CTSCLog.i(TAG, "Restricted user, not setting location mode");
            return;
        }
        Intent intent = new Intent(MODE_CHANGING_ACTION);
        intent.putExtra(CURRENT_MODE_KEY, mCurrentMode);
        intent.putExtra(NEW_MODE_KEY, mode);
        getContext().sendBroadcast(intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
        Settings.Secure.putInt(getContext().getContentResolver(), Settings.Secure.LOCATION_MODE, mode);
    }
    
    private boolean isRestricted() {
        final UserManager um = (UserManager)getContext().getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }
}


class CheckBluetoothState extends CheckItemBase {
    private static final String TAG = "CheckBluetoothState";
    private boolean mIsWaiting = false;
        
    CheckBluetoothState(Context c, String key) {
        super(c, key);
        setTitle(R.string.Bluetooth_title);
        setNote(c.getString(R.string.Bluetooth_off_note)
                + c.getString(R.string.SOP_REFER)
                + c.getString(R.string.SOP_LTE_NS_IOT));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }
    
    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        if (mIsWaiting) {
            setValue(R.string.ctsc_querying);
            mResult = check_result.UNKNOWN;
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            switch (adapter.getState()) {
                case BluetoothAdapter.STATE_ON:
                    setValue(R.string.ctsc_enabled);
                    mResult = check_result.WRONG;
                    break;
                case BluetoothAdapter.STATE_OFF:
                    setValue(R.string.ctsc_disabled);
                    mResult = check_result.RIGHT;
                    break;
                default:
                    setValue(R.string.ctsc_unknown);
                    mResult = check_result.WRONG;
                    break;
            }
        }        
        return mResult;
    }
    
    private void setBluetoothState(boolean enabled) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (enabled) {
            adapter.enable();
        } else {
            adapter.disable();
        }       
    }
    
    public boolean onReset() {
        CTSCLog.i(TAG, "onReset");
        setBluetoothState(false);
        mIsWaiting = true;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                mIsWaiting = false;
                sendBroadcast();
            }
        }, 3000);
        return true;
    }
}


class CheckUSBCBAState extends CheckItemBase {
    private static final String TAG = "CheckUSBCBAState";    
        
    CheckUSBCBAState(Context c, String key) {
        super(c, key);
        setTitle(R.string.USB_CBA_title);
        setNote(c.getString(R.string.USB_CBA_note)
                + c.getString(R.string.SOP_REFER)
                + c.getString(R.string.USB_CBA_sop));
        setProperty(PROPERTY_AUTO_CHECK);
    }
    
    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        if (SystemProperties.get("ro.mtk_usb_cba_support").equals("1") == true) {
            mResult = check_result.RIGHT;
            setValue(R.string.USB_CBA_on_value);
        } else {
            setValue(R.string.USB_CBA_off_value);
            mResult = check_result.WRONG;
        }
        return mResult;
    }        
}

class CheckAutoAnswer extends CheckItemBase {
    private static final String TAG = "CheckAudioAnswer";
    private static final String SHREDPRE_NAME = "AutoAnswer";
    private static final String FLAG = "flag";
    
    CheckAutoAnswer(Context c, String key) {
        super(c, key);
        setTitle(R.string.auto_answer_title);
        setNote(c.getString(R.string.auto_answer_note)
                + c.getString(R.string.SOP_REFER)
                + c.getString(R.string.MOS_SOP));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }
    
    private Context getEMContext() {
        Context eMContext = null;
        try {
            eMContext = getContext().createPackageContext(
                    "com.mediatek.engineermode", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        if (null == eMContext) {
            throw new NullPointerException("eMContext=" + eMContext);
        }
        return eMContext;
    }
    
    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        final SharedPreferences autoAnswerSh = 
                getEMContext().getSharedPreferences(SHREDPRE_NAME, Context.MODE_WORLD_READABLE);         
        final boolean isEnable = autoAnswerSh.getBoolean(FLAG, false);
        CTSCLog.d(TAG, "flag is :" + isEnable);

        if (isEnable) {
            setValue(R.string.value_DC_on);
            mResult = check_result.RIGHT;
        } else {
            setValue(R.string.value_DC_off);
            mResult = check_result.WRONG;
        }
        return mResult;
    }
    
    public boolean onReset() {
        CTSCLog.d(TAG, "onReset");
        final SharedPreferences autoAnswerSh =  
            getEMContext().getSharedPreferences(SHREDPRE_NAME, Context.MODE_WORLD_READABLE);         
        final SharedPreferences.Editor editor = autoAnswerSh.edit();
        editor.putBoolean(FLAG, true);
        editor.commit();
        mResult = check_result.RIGHT;

        return true;
    }
}

class CheckJBM extends CheckItemBase {
    private static final String TAG = "CheckJBM";
    private byte[] mData;
    private static final int DATA_SIZE = 1444;
    private static final int CONSTANT_256 = 256;
    private static final int VOLUME_SIZE = 22;
    
    CheckJBM(Context c, String key) {
        super(c, key);
        setTitle(R.string.JBM_title);
        setNote(c.getString(R.string.JBM_note)
                + c.getString(R.string.JBM_note2)
                + c.getString(R.string.SOP_REFER)
                + c.getString(R.string.MOS_SOP));
        setProperty(PROPERTY_AUTO_CHECK | PROPERTY_AUTO_CONFG);
    }
    
    public check_result getCheckResult() {
        CTSCLog.d(TAG, "getCheckResult");
        mData = new byte[DATA_SIZE];
        for (int n = 0; n < DATA_SIZE; n++) {
            mData[n] = 0;
        }
        int ret = AudioSystem.getEmParameter(mData, DATA_SIZE);
        if (ret != 0) {
             CTSCLog.i(TAG,
                "getCheckResult GetEMParameter return value is : "
                    + ret);
        }
        /*set common param mode is 0, param 7 index is 7*/
        int param7 = getSpeechEnhanceAudioData(0, 7);
        int param8 = getSpeechEnhanceAudioData(0, 8);
        int param9 = getSpeechEnhanceAudioData(0, 9);
        int param10 = getSpeechEnhanceAudioData(0, 10);
        CTSCLog.d(TAG, "JBM para7= " + param7 + " param8 = " + param8
            + " param9 = " + param9 + " param10 = " + param10);
        if (param7 != 1 || param8 != 3
            || param9 != 9 || param10 != 98) {
            mResult = check_result.WRONG;
            setValue(R.string.JBM_value_fail);
        } else {
            mResult = check_result.RIGHT;
            setValue(R.string.JBM_value_ok);
        }            
        
        return mResult;
    }
    
    public boolean onReset() {
        CTSCLog.d(TAG, "onReset");
        int ret1 = setSpeechEnhanceAudioData(7, 1);
        int ret2 = setSpeechEnhanceAudioData(8, 3);
        int ret3 = setSpeechEnhanceAudioData(9, 9);
        int ret4 = setSpeechEnhanceAudioData(10, 98);
        CTSCLog.d(TAG, "JBM onReset ret1 = " + ret1 + " ret2 = " + ret2
            + " ret3 = " + ret3 + " ret4 = " + ret4);
        if ( ret1==0 && ret2==0 && ret3==0 && ret4==0) {
            mResult = check_result.RIGHT;
            setValue(R.string.JBM_value_ok);
        } else {
            mResult = check_result.WRONG;
            setValue(R.string.JBM_value_fail);
        }
        
        return true;
    }

    private int getSpeechEnhanceAudioData(int mode_index, int param_index) {
        int high = 0;
        int low = 0;
        high = mData[VOLUME_SIZE + param_index * 2 + 1];
        low = mData[VOLUME_SIZE + param_index * 2];
       
        high = high < 0 ? high + CONSTANT_256 : high;
        low = low < 0 ? low + CONSTANT_256 : low;
        return high * CONSTANT_256 + low;

    }

    private int setSpeechEnhanceAudioData(int param_index , int inputValue) {
        final int high = (int) (inputValue / CONSTANT_256);
        final int low = (int) (inputValue % CONSTANT_256);

        CTSCLog.d(TAG, "input value param_idx = " + param_index + " value = " + inputValue);
        
        mData[VOLUME_SIZE + param_index * 2] = (byte) low;
        mData[VOLUME_SIZE + param_index * 2 + 1] = (byte) high;
        

        int result = AudioSystem.setEmParameter(mData, DATA_SIZE);
        if (0 == result) {
            CTSCLog.d(TAG, " setSpeechEnhanceAudioData set sucess");
        } else {
            CTSCLog.d(TAG,
                "Audio_SpeechEnhancement SetEMParameter return value is : "
                    + result);
        }
        return result;
    }
}