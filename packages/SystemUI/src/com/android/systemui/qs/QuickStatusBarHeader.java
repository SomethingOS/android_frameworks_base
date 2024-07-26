/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.UserHandle;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.UserHandle;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.Space;
import android.widget.TextView;

import com.android.internal.graphics.ColorUtils;

import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.util.LargeScreenUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.Math;
import java.util.Iterator;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.plugins.ActivityStarter;
import android.media.session.MediaController;
import android.media.session.MediaSessionLegacyHelper;
import android.net.ConnectivityManager;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.qs.TouchAnimator.Builder;

import com.android.systemui.qs.QSPanelController;
import com.android.systemui.statusbar.connectivity.AccessPointController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.qs.tiles.dialog.BluetoothDialogFactory;
import com.android.systemui.qs.tiles.dialog.InternetDialogFactory;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.media.dialog.MediaOutputDialogFactory;

/**
 * View that contains the top-most bits of the QS panel (primarily the status bar with date, time,
 * battery, carrier info and privacy icons) and also contains the {@link QuickQSPanel}.
 */
public class QuickStatusBarHeader extends FrameLayout
            implements BluetoothCallback, View.OnClickListener, View.OnLongClickListener {

    private boolean mExpanded;
    private boolean mQsDisabled;

    protected QuickQSPanel mHeaderQsPanel;
    public float mKeyguardExpansionFraction;

    private int colorActive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
    private int colorInactive = Utils.getColorAttrDefaultColor(mContext, R.attr.shadeInactive);
    private int colorLabelActive = Utils.getColorAttrDefaultColor(mContext, com.android.internal.R.attr.textColorPrimaryInverse);
    private int colorLabelInactive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);

    private int mColorArtwork = Color.BLACK;
    private int mMediaTextIconColor = Color.WHITE;

    private ViewGroup mOpQsContainer;
    private ViewGroup mOpQsLayout;

    private ViewGroup mBluetoothButton;
    private ImageView mBluetoothIcon;
    private TextView mBluetoothText;
    private ImageView mBluetoothChevron;
    private boolean mBluetoothEnabled;

    private ViewGroup mInternetButton;
    private ImageView mInternetIcon;
    private TextView mInternetText;
    private ImageView mInternetChevron;
    private boolean mInternetEnabled;

    private final ActivityStarter mActivityStarter;
    private final ConnectivityManager mConnectivityManager;
    private final SubscriptionManager mSubManager;
    private final WifiManager mWifiManager;

    public TouchAnimator mQQSContainerAnimator;

    public QSPanelController mQSPanelController;
    public BluetoothController mBluetoothController;
    public BluetoothDialogFactory mBluetoothDialogFactory;
    public InternetDialogFactory mInternetDialogFactory;
    public AccessPointController mAccessPointController;

    private final Handler mHandler;
    private Runnable mUpdateRunnableBluetooth;
    private Runnable mUpdateRunnableInternet;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler(Looper.getMainLooper());
        mBluetoothEnabled = false;
        mInternetEnabled = false;
        mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mSubManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);

        mOpQsContainer = findViewById(R.id.qs_container);
        mOpQsLayout = findViewById(R.id.qs_so_header_layout);
        mInternetButton = findViewById(R.id.qs_so_button_internet);
        mInternetIcon = findViewById(R.id.qs_so_internet_icon);
        mInternetText = findViewById(R.id.qs_so_internet_text);
        mInternetChevron = findViewById(R.id.qs_so_chevron_internet);
        mBluetoothButton = findViewById(R.id.qs_so_button_bluetooth);
        mBluetoothIcon = findViewById(R.id.qs_so_bluetooth_icon);
        mBluetoothText = findViewById(R.id.qs_so_bluetooth_text);
        mBluetoothChevron = findViewById(R.id.qs_so_chevron_bluetooth);

        initBluetoothManager();

        mInternetButton.setOnClickListener(this);
        mBluetoothButton.setOnClickListener(this);

        mInternetButton.setOnLongClickListener(this);
        mBluetoothButton.setOnLongClickListener(this);

        startUpdateInterntTileStateAsync();
        startUpdateBluetoothTileStateAsync();
    }

    private void initBluetoothManager() {
        LocalBluetoothManager localBluetoothManager = LocalBluetoothManager.getInstance(mContext, null);

        if (localBluetoothManager != null) {
            localBluetoothManager.getEventManager().registerCallback(this);
            LocalBluetoothAdapter localBluetoothAdapter = localBluetoothManager.getBluetoothAdapter();
            int bluetoothState = BluetoothAdapter.STATE_DISCONNECTED;

            synchronized (localBluetoothAdapter) {
                if (localBluetoothAdapter.getAdapter().getState() != localBluetoothAdapter.getBluetoothState()) {
                    localBluetoothAdapter.setBluetoothStateInt(localBluetoothAdapter.getAdapter().getState());
                }
                bluetoothState = localBluetoothAdapter.getBluetoothState();
            }
            updateBluetoothState(bluetoothState);
        }
    }

    @Override
    public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
        updateBluetoothState(bluetoothState);
    }

    private void updateBluetoothState(@AdapterState int bluetoothState) {
        mBluetoothEnabled = bluetoothState == BluetoothAdapter.STATE_ON
                || bluetoothState == BluetoothAdapter.STATE_TURNING_ON;
        updateBluetoothTile();
    }

    public final void updateBluetoothTile() {
        if (mBluetoothButton == null
                || mBluetoothIcon == null
                || mBluetoothText == null
                || mBluetoothChevron == null)
            return;
        Drawable background = mBluetoothButton.getBackground();
        if (mBluetoothEnabled) {
            background.setTint(colorActive);
            mBluetoothIcon.setColorFilter(colorLabelActive);
            mBluetoothText.setTextColor(colorLabelActive);
            mBluetoothChevron.setColorFilter(colorLabelActive);
        } else {
            background.setTint(colorInactive);
            mBluetoothIcon.setColorFilter(colorLabelInactive);
            mBluetoothText.setTextColor(colorLabelInactive);
            mBluetoothChevron.setColorFilter(colorLabelInactive);
        }
    }

    public void updateInterntTile() {
        if (mInternetButton == null
                || mInternetIcon == null
                || mInternetText == null
                || mInternetChevron == null)
            return;

        String carrier;
        int iconResId = 0;

        if (isWifiConnected()) {
            carrier = getWifiSsid();
            mInternetEnabled = true;
            iconResId = mContext.getResources().getIdentifier("ic_wifi_signal_4", "drawable", "android");
        } else {
            carrier = getSlotCarrierName();
            mInternetEnabled = true;
            iconResId = mContext.getResources().getIdentifier("ic_signal_cellular_4_4_bar", "drawable", "android");
        }

        mInternetText.setText(carrier);
        mInternetIcon.setImageResource(iconResId);

        Drawable background = mInternetButton.getBackground();

        if (mInternetEnabled) {
            background.setTint(colorActive);
            mInternetIcon.setColorFilter(colorLabelActive);
            mInternetText.setTextColor(colorLabelActive);
            mInternetChevron.setColorFilter(colorLabelActive);
        } else {
            background.setTint(colorInactive);
            mInternetIcon.setColorFilter(colorLabelInactive);
            mInternetText.setTextColor(colorLabelInactive);
            mInternetChevron.setColorFilter(colorLabelInactive);
        }
    }

    private boolean isWifiConnected() {
        final Network network = mConnectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            return false;
        }
    }

    private String getSlotCarrierName() {
        CharSequence result = mContext.getResources().getString(R.string.quick_settings_internet_label);
        int subId = mSubManager.getDefaultDataSubscriptionId();
        final List<SubscriptionInfo> subInfoList = mSubManager.getActiveSubscriptionInfoList(true);
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subId == subInfo.getSubscriptionId()) {
                    result = subInfo.getDisplayName();
                    break;
                }
            }
        }
        return result.toString();
    }

    private String getWifiSsid() {
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getHiddenSSID() || wifiInfo.getSSID() == WifiManager.UNKNOWN_SSID) {
            return mContext.getResources().getString(R.string.quick_settings_wifi_label);
        } else {
            return wifiInfo.getSSID().replace("\"", "");
        }
    }


    public void onClick(View v) {
        if (v == mInternetButton) {
            new Handler().post(() -> mInternetDialogFactory.create(true,
                    mAccessPointController.canConfigMobileData(),
                    mAccessPointController.canConfigWifi(),
                    v));
        } else if (v == mBluetoothButton) {
            new Handler().post(() -> mBluetoothDialogFactory.create(true, v));
        }
    }

    public boolean onLongClick(View v) {
        if (v == mInternetButton) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
        } else if (v == mBluetoothButton) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Only react to touches inside QuickQSPanel
        if (event.getY() > mHeaderQsPanel.getTop()) {
            return super.onTouchEvent(event);
        } else {
            return false;
        }
    }

    void updateResources() {
        colorActive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
        colorInactive = Utils.getColorAttrDefaultColor(mContext, R.attr.shadeInactive);
        colorLabelActive = Utils.getColorAttrDefaultColor(mContext, com.android.internal.R.attr.textColorPrimaryInverse);
        colorLabelInactive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);

        Resources resources = mContext.getResources();
        int orientation = getResources().getConfiguration().orientation;
        boolean largeScreenHeaderActive = LargeScreenUtils.shouldUseLargeScreenShadeHeader(resources);

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mQsDisabled) {
            lp.height = 0;
        } else {
            lp.height = WRAP_CONTENT;
        }
        setLayoutParams(lp);

        MarginLayoutParams qqsLP = (MarginLayoutParams) mHeaderQsPanel.getLayoutParams();
        qqsLP.topMargin = 0;
        mHeaderQsPanel.setLayoutParams(qqsLP);

        MarginLayoutParams opQqsLP = (MarginLayoutParams) mOpQsLayout.getLayoutParams();
        int qqsMarginTop = resources.getDimensionPixelSize(largeScreenHeaderActive ?
                            R.dimen.qqs_layout_margin_top : R.dimen.large_screen_shade_header_min_height);
        opQqsLP.topMargin = qqsMarginTop;
        mOpQsLayout.setLayoutParams(opQqsLP);

        float qqsExpandY = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                            0 : resources.getDimensionPixelSize(R.dimen.qs_header_height)
                            + resources.getDimensionPixelSize(R.dimen.qs_so_header_layout_expanded_top_margin)
                            - qqsMarginTop;
        TouchAnimator.Builder builderP = new TouchAnimator.Builder()
            .addFloat(mOpQsLayout, "translationY", 0, qqsExpandY);
        mQQSContainerAnimator = builderP.build();
    }

    public void startUpdateInterntTileStateAsync() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                startUpdateInterntTileState();
            }
        });
    }

    public void startUpdateBluetoothTileStateAsync() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                startUpdateBluetoothTileState();
            }
        });
    }

    public void startUpdateInterntTileState() {
        Runnable runnable = mUpdateRunnableInternet;

        if (runnable == null) {
            mUpdateRunnableInternet = new Runnable() {
                public void run() {
                    updateInterntTile();
                    scheduleInternetUpdate();
                }
            };
        } else {
            mHandler.removeCallbacks(runnable);
        }

        scheduleInternetUpdate();
    }

    public void startUpdateBluetoothTileState() {
        Runnable runnable = mUpdateRunnableBluetooth;
        
        if (runnable == null) {
            mUpdateRunnableBluetooth = new Runnable() {
                public void run() {
                    updateBluetoothTile();
                    scheduleBluetoothUpdate();
                }
            };
        } else {
            mHandler.removeCallbacks(runnable);
        }

        scheduleBluetoothUpdate();
    }

    public void scheduleInternetUpdate() {
        Runnable runnable;
        if ((runnable = mUpdateRunnableInternet) != null) {
            mHandler.postDelayed(runnable, 1000);
        }
    }

    public void scheduleBluetoothUpdate() {
        Runnable runnable;
        if ((runnable = mUpdateRunnableBluetooth) != null) {
            mHandler.postDelayed(runnable, 1000);
        }
    }

    public void setExpanded(boolean expanded, QuickQSPanelController quickQSPanelController) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        quickQSPanelController.setExpanded(expanded);
    }

    public void setExpansion(boolean forceExpanded, float expansionFraction, float panelTranslationY) {
		final float keyguardExpansionFraction = forceExpanded ? 1f : expansionFraction;
		
		if (mQQSContainerAnimator != null) {
			mQQSContainerAnimator.setPosition(keyguardExpansionFraction);
		}
		
		if (forceExpanded) {
			setAlpha(expansionFraction);
		} else {
			setAlpha(1);
		}
		
		mKeyguardExpansionFraction = keyguardExpansionFraction;
	}

    public void disable(int state1, int state2, boolean animate) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        mHeaderQsPanel.setDisabledByPolicy(disabled);
        updateResources();
    }

    private void setContentMargins(View view, int marginStart, int marginEnd) {
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        view.setLayoutParams(lp);
    }
}
