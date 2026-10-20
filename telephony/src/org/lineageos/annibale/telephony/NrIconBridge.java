// SPDX-License-Identifier: Apache-2.0
package org.lineageos.annibale.telephony;

import android.os.Handler;
import android.os.Message;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.DisplayInfoController;
import com.android.internal.telephony.Phone;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.NrIconType;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

/** Phone-process adapter from Qualcomm's icon policy to Android's modem display policy. */
final class NrIconBridge extends Handler {
    private static final String TAG = "AnnibaleNrIcon";
    private static final int SERVICE_STATE = 1;
    private static final int RADIO_OFF = 2;
    private static final long RETRY_MS = 5000;
    private final Phone mPhone;
    private final DisplayInfoController mDisplayController;
    private final ExtTelephonyManager mManager;
    private Client mClient;
    private ExtPhoneCallbackListener mListener;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mNrRegistered;
    private boolean mLastAdvanced;
    private final Runnable mRetry = this::register;
    private final ServiceCallback mConnection = new ServiceCallback() {
        @Override public void onConnected() { post(() -> register()); }
        @Override public void onDisconnected() {
            post(() -> {
                mClient = null;
                mListener = null;
                publish(false);
                removeCallbacks(mRetry);
                postDelayed(mRetry, RETRY_MS);
            });
        }
    };

    NrIconBridge(Phone phone, DisplayInfoController controller) {
        super(phone.getLooper());
        mPhone = phone;
        mDisplayController = controller;
        mManager = ExtTelephonyManager.getInstance(phone.getContext());
        phone.registerForServiceStateChanged(this, SERVICE_STATE, null);
        phone.mCi.registerForOffOrNotAvailable(this, RADIO_OFF, null);
        SubscriptionManager subscriptions = phone.getContext().getSystemService(
                SubscriptionManager.class);
        if (subscriptions != null) {
            subscriptions.addOnSubscriptionsChangedListener(this::post,
                    new SubscriptionManager.OnSubscriptionsChangedListener() {
                        @Override public void onSubscriptionsChanged() {
                            if (updateRegistration() && mClient != null) register();
                        }
                    });
        }
        post(() -> {
            updateRegistration();
            connect();
        });
    }

    private void connect() {
        try {
            if (!mManager.connectService(mConnection)) {
                mManager.disconnectService(mConnection);
                postDelayed(this::connect, RETRY_MS);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Cannot bind Qualcomm telephony", e);
            // connectService retains callbacks even if bindService throws. Remove ours
            // so the next attempt actually binds instead of waiting on a dead connection.
            mManager.disconnectService(mConnection);
            postDelayed(this::connect, RETRY_MS);
        }
    }

    private void register() {
        removeCallbacks(mRetry);
        if (!mManager.isServiceConnected()) {
            publish(false);
            postDelayed(mRetry, RETRY_MS);
            return;
        }
        try {
            if (mListener != null) mManager.unregisterCallback(mListener);
            mClient = null;
            final int registeredSubId = mPhone.getSubId();
            final ExtPhoneCallbackListener listener = new ExtPhoneCallbackListener(getLooper()) {
                @Override
                public void onNrIconType(int slot, Token token, Status status, NrIconType icon) {
                    if (mListener != this || slot != mPhone.getPhoneId()) return;
                    if (status == null || status.get() != Status.SUCCESS || icon == null) {
                        publish(false);
                        return;
                    }
                    if (registeredSubId != mPhone.getSubId()) {
                        publish(false);
                        post(() -> register());
                        return;
                    }
                    updateRegistration();
                    publish(mNrRegistered
                            && icon.get() == NrIconType.TYPE_5G_UWB);
                }
            };
            mListener = listener;
            mClient = mManager.registerCallbackWithEvents(mPhone.getContext().getPackageName(),
                    listener, new int[] {ExtPhoneCallbackListener.EVENT_ON_NR_ICON_TYPE});
            if (mClient == null) {
                mListener = null;
                publish(false);
                postDelayed(mRetry, RETRY_MS);
                return;
            }
            query();
        } catch (RuntimeException e) {
            Log.e(TAG, "Cannot register NR icon listener", e);
            publish(false);
            postDelayed(mRetry, RETRY_MS);
        }
    }

    private void query() {
        if (mClient != null) {
            try {
                if (mManager.queryNrIconType(mPhone.getPhoneId(), mClient) == null) {
                    publish(false);
                    postDelayed(mRetry, RETRY_MS);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Cannot query NR icon", e);
                publish(false);
            }
        }
    }

    private boolean updateRegistration() {
        ServiceState state = mPhone.getServiceState();
        int subId = mPhone.getSubId();
        boolean nr = mPhone.mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON
                && SubscriptionManager.isValidSubscriptionId(subId) && state != null
                && (state.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_NR
                    || state.getNrState() == NetworkRegistrationInfo.NR_STATE_CONNECTED);
        boolean changed = subId != mSubId || nr != mNrRegistered;
        mSubId = subId;
        mNrRegistered = nr;
        if (changed || !nr) publish(false);
        return changed;
    }

    private void publish(boolean advanced) {
        if (advanced == mLastAdvanced) return;
        mLastAdvanced = advanced;
        Log.i(TAG, "slot=" + mPhone.getPhoneId() + " subId=" + mSubId
                + " modemNrAdvanced=" + advanced);
        mDisplayController.setModemNrAdvanced(advanced);
    }

    @Override public void handleMessage(Message message) {
        if (message.what == RADIO_OFF) {
            mNrRegistered = false;
            publish(false);
        } else if (message.what == SERVICE_STATE && updateRegistration()) {
            // A new subscription/NR session must get a fresh modem answer.
            if (mClient != null) register();
        }
    }
}
