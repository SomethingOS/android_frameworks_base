/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.globalactions;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.NonNull;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

import com.android.systemui.MultiListLayout;
import com.android.systemui.MultiListLayout.MultiListAdapter;
import com.android.systemui.R;

/**
 * Creates a customized Dialog for displaying the Shut Down and Restart actions.
 */
public class GlobalActionsPowerDialog {

    /**
     * Create a dialog for displaying Shut Down and Restart actions.
     */
<<<<<<< HEAD
    public static Dialog create(@NonNull Context context, MultiListAdapter adapter) {
        final ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(
                R.layout.global_actions_grid_lite, null);
=======
    public static Dialog create(@NonNull Context context, ListAdapter adapter, boolean forceDark) {
        ViewGroup listView = (ViewGroup) LayoutInflater.from(context).inflate(
                com.android.systemui.res.R.layout.global_actions_power_dialog_flow, null);
>>>>>>> fa3d403840df (Power menu styles: Initial checkin for U [1/3])

        final MultiListLayout multiListLayout = view.findViewById(R.id.global_actions_view);
        multiListLayout.setAdapter(adapter);

        final View overflowButton = view.findViewById(R.id.global_actions_overflow_button);
        if (overflowButton != null) {
            overflowButton.setVisibility(View.GONE);
            final LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) multiListLayout.getLayoutParams();
            params.setMarginEnd(context.getResources().getDimensionPixelSize(
                    R.dimen.global_actions_side_margin));
            multiListLayout.setLayoutParams(params);
        }

        final Dialog dialog = new Dialog(context) {
            @Override
            protected void onStart() {
                super.onStart();
                multiListLayout.updateList();
            }

            @Override
            public void show() {
                super.show();
                view.setOnApplyWindowInsetsListener((v, windowInsets) -> {
                    view.setPadding(
                        windowInsets.getStableInsetLeft(),
                        windowInsets.getStableInsetTop(),
                        windowInsets.getStableInsetRight(),
                        windowInsets.getStableInsetBottom()
                    );
                    return WindowInsets.CONSUMED;
                });
            }
        };

        final Window window = dialog.getWindow();
        window.setLayout(WRAP_CONTENT, WRAP_CONTENT);
        window.setType(LayoutParams.TYPE_VOLUME_OVERLAY);
        window.addFlags(
            LayoutParams.FLAG_ALT_FOCUSABLE_IM |
            LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
<<<<<<< HEAD
        dialog.setContentView(view);
=======
        dialog.setContentView(listView);

        BlurUtils blurUtils = new BlurUtils(context.getResources(),
                CrossWindowBlurListeners.getInstance(), new DumpManager());

        Window window = dialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY);
        window.setTitle(""); // prevent Talkback from speaking first item name twice
        window.setBackgroundDrawable(res.getDrawable(
                forceDark ? com.android.systemui.res.R.drawable.global_actions_background
                        : com.android.systemui.res.R.drawable.global_actions_lite_background,
                context.getTheme()));
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        if (blurUtils.supportsBlursOnWindows()) {
            // Enable blur behind
            // Enable dim behind since we are setting some amount dim for the blur.
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    | WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            // Set blur behind radius
            int blurBehindRadius = context.getResources()
                    .getDimensionPixelSize(com.android.systemui.res.R.dimen.max_window_blur_radius);
            window.getAttributes().setBlurBehindRadius(blurBehindRadius);
            // Set dim only when blur is enabled.
            window.setDimAmount(0.54f);
        }

>>>>>>> fa3d403840df (Power menu styles: Initial checkin for U [1/3])
        return dialog;
    }
}
