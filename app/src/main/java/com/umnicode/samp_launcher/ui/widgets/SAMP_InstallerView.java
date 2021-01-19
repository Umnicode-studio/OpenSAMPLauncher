package com.umnicode.samp_launcher.ui.widgets;
import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.umnicode.samp_launcher.LauncherApplication;
import com.umnicode.samp_launcher.R;
import com.umnicode.samp_launcher.core.SAMP.Components.TaskStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPPackageStatus;
import com.umnicode.samp_launcher.core.SAMP.SAMPInstaller;
import com.umnicode.samp_launcher.core.SAMP.SAMPInstallerCallback;
import com.umnicode.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;
import com.umnicode.samp_launcher.core.Utils;

interface ButtonAnimCallback{
    void beforeAnim();
    void onFinished();
}

public class SAMP_InstallerView extends LinearLayout {
    private final float BUTTON_ANIM_SPEED = 0.2f; // By 1 px

    private Context _Context;
    private View RootView;
    private float InitialButtonY = 0;
    private boolean IsOnLayoutFired = false;
    private SAMPInstallerCallback Callback;

    public boolean EnableAnimations = true;

    public SAMP_InstallerView(Context context) {
        super(context);
        this.Init(context);
    }
    public SAMP_InstallerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.Init(context);
    }
    public SAMP_InstallerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.Init(context);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Unregister callback from installer
        this.GetApplication().Installer.Callbacks.remove(this.Callback);
    }

    private void Init(Context context){
        this._Context = context;

        this.RootView = inflate(context, R.layout.samp_installer_view, this);
        LauncherApplication Application = this.GetApplication();

        // Create callback
        this.Callback = new SAMPInstallerCallback() {
            public void OnStatusChanged(SAMPInstallerStatus Status) {
                Update(context, Application.Installer, false);
            }

            public void OnDownloadProgressChanged(TaskStatus Status) {
                UpdateDownloadTaskStatus(Status, context.getResources());
            }
            public void OnExtractProgressChanged(TaskStatus Status) {
                UpdateExtractTaskStatus(Status, context.getResources());
            }

            public void OnInstallFinished(InstallStatus Status) { }
        };

        // Bind installer Status changing
        Application.Installer.Callbacks.add(this.Callback);

        // Setup progress bar
        ProgressBar ProgressBar = this.RootView.findViewById(R.id.installer_download_progress);
        ProgressBar.setMax(100);
    }
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Get button initial position
        if (!this.IsOnLayoutFired) {
            this.InitialButtonY = this.RootView.findViewById(R.id.installer_button).getY();

            // Force-update status
            this.Update(this._Context, this.GetApplication().Installer, true);
            this.IsOnLayoutFired = true;
        }
    }

    private void Update(Context context, SAMPInstaller Installer, boolean IsInit){
        SAMPInstallerStatus Status = Installer.GetStatus();

        // Get handles
        TextView Text = this.RootView.findViewById(R.id.installer_status_text);
        RelativeLayout BarLayout = this.RootView.findViewById(R.id.installer_progress_bar_layout);
        Button Button = this.RootView.findViewById(R.id.installer_button);

        Resources resources = context.getResources();

        // Update UI
        Text.setVisibility(VISIBLE);

        System.out.println(Status + " - " + Installer.GetLastInstallStatus()); //TODO:

        if (Status == SAMPInstallerStatus.DOWNLOADING){
            this.ShowProcessState(Button, Text, BarLayout, IsInit, resources);

            // Force update download status (used to show current status before it's updated )
            this.UpdateDownloadTaskStatus(Installer.GetCurrentTaskStatus(), resources);
        }
        else if (Status == SAMPInstallerStatus.EXTRACTING){
            this.ShowProcessState(Button, Text, BarLayout, IsInit, resources);

            // Force update extract status (used to show current status before it's updated )
            this.UpdateExtractTaskStatus(Installer.GetCurrentTaskStatus(), resources);
        }
        else if (Status == SAMPInstallerStatus.PREPARING){
            // Setup label
            Text.setTextColor(resources.getColor(R.color.colorNone));
            Text.setText(resources.getString(R.string.installer_status_preparing));

            // Setup button
            Button.setText(resources.getString(R.string.installer_button_cancel));
            Button.setVisibility(VISIBLE);

            this.BindButtonAsCancel(Button);
            this.HideBarLayout(BarLayout, Button, IsInit, true);
        }
        else if (Status == SAMPInstallerStatus.WAITING_FOR_APK_INSTALL) {
            // Setup label
            Text.setTextColor(resources.getColor(R.color.colorOk));
            Text.setText(resources.getString(R.string.installer_status_waiting_for_apk_install));

            // Setup button
            Button.setText(resources.getString(R.string.installer_button_waiting_for_apk_install));
            Button.setVisibility(VISIBLE);

            this.BindButtonAsApkInstall(Button);
            this.HideBarLayout(BarLayout, Button, IsInit, false);
        }
        else{ // No install running ( CANCELING_INSTALL || NONE )
            SAMPPackageStatus PkgStatus = SAMPInstaller.IsInstalled(context.getPackageManager(), resources);

            if (PkgStatus == SAMPPackageStatus.FOUND){ // SAMP installed => do nothing TODO: Export
                Text.setText(resources.getString(R.string.installer_status_none_SAMP_found));
                Text.setTextColor(resources.getColor(R.color.colorOk));

                BarLayout.setVisibility(INVISIBLE);
                Button.setVisibility(INVISIBLE);
            }
            else {
                Text.setTextColor(resources.getColor(R.color.colorError));

                // Check for previous install errors
                //TODO: Permissions
                if (Installer.GetLastInstallStatus() == InstallStatus.DOWNLOADING_ERROR){
                    // Set error message and button text
                    Text.setText(resources.getString(R.string.install_status_downloading_error));
                    Button.setText(resources.getString(R.string.installer_button_retry));
                }
                else if (Installer.GetLastInstallStatus() == InstallStatus.EXTRACTING_ERROR) {
                    // Also we don't bind button callback because it's the same for all branches
                    Text.setText(resources.getString(R.string.install_status_extracting_error));
                    Button.setText(resources.getString(R.string.installer_button_retry));
                }
                else if (Installer.GetLastInstallStatus() == InstallStatus.APK_NOT_FOUND){
                    Text.setText(resources.getString(R.string.install_status_apk_not_found));
                    Button.setText(resources.getString(R.string.installer_button_retry));
                }else {
                    // If there are no errors, promote to install SAMP
                    Text.setText(resources.getString(R.string.installer_status_none_SAMP_not_found));
                    Button.setText(resources.getString(R.string.installer_button_install));
                }

                // Check does cache found
                if (PkgStatus == SAMPPackageStatus.CACHE_NOT_FOUND) {
                    // If there are no errors, promote to install SAMP
                    Text.setText(resources.getString(R.string.installer_status_none_SAMP_cache_not_found));
                    Button.setText(resources.getString(R.string.installer_button_install_cache));

                    Button.setOnClickListener(v -> {
                        // Install SAMP
                        GetApplication().Installer.InstallOnlyCache(context);
                    });
                }else{
                    Button.setOnClickListener(v -> {
                        // Install SAMP
                        GetApplication().Installer.Install(context);
                    });
                }

                this.HideBarLayout(BarLayout, Button, IsInit, Status == SAMPInstallerStatus.CANCELING_INSTALL);
            }
        }
    }

    // UI
    private void HideBarLayout(RelativeLayout BarLayout, Button Button, boolean IsInit, boolean DisableButton){
        // Hide bar layout and button with animation
        this.MoveButtonTo(this.InitialButtonY - BarLayout.getHeight(), this.BUTTON_ANIM_SPEED, IsInit,
                new ButtonAnimCallback() {
                    public void beforeAnim() {
                        BarLayout.setVisibility(INVISIBLE);
                    }

                    public void onFinished() {
                        Button.setEnabled(!DisableButton);
                    }
                });
    }

    private void BindButtonAsCancel(Button Btn){
        // Set click listener
        Btn.setOnClickListener(v -> {
            // Cancel SAMP installation
            GetApplication().Installer.CancelInstall();
        });
    }
    private void BindButtonAsApkInstall(Button Btn){
        Btn.setOnClickListener(v -> {
            // Open downloaded APK file
            GetApplication().Installer.OpenInstalledAPK();
        });
    }

    // States
    private void ShowProcessState(Button Button, TextView Text, RelativeLayout BarLayout, boolean IsInit, Resources resources){
        // Show progress bar, setup text color etc
        // And setup button
        Button.setText(resources.getString(R.string.installer_button_cancel));
        Button.setVisibility(VISIBLE);

        this.BindButtonAsCancel(Button);

        // Set text color
        Text.setTextColor(resources.getColor(R.color.colorNone));

        // Setup progressBar layout and play animation
        if (BarLayout.getVisibility() == INVISIBLE){
            this.MoveButtonTo(this.InitialButtonY, BUTTON_ANIM_SPEED, IsInit,
                    new ButtonAnimCallback() { // Speed is measured in ms/1px
                        public void beforeAnim() { }
                        public void onFinished() {
                            // Show progress bar and text on it
                            BarLayout.setVisibility(VISIBLE);
                        }
                    });
        }
    }

    // Utils
    private void UpdateDownloadTaskStatus(TaskStatus Status, Resources resources){
        TextView Text = this.RootView.findViewById(R.id.installer_status_text);

        // Setup values
        Text.setText(String.format(resources.getString(R.string.installer_status_downloading), Status.File, Status.FilesNumber));
        this.UpdateProgressBar(Status, resources);
    }
    private void UpdateExtractTaskStatus(TaskStatus Status, Resources resources){
        TextView Text = this.RootView.findViewById(R.id.installer_status_text);

        // Setup values
        Text.setText(String.format(resources.getString(R.string.installer_status_extracting), Status.File, Status.FilesNumber));
        this.UpdateProgressBar(Status, resources); // Update progress bar
    }

    private void UpdateProgressBar(TaskStatus Status, Resources resources){
        ProgressBar ProgressBar = this.RootView.findViewById(R.id.installer_download_progress);
        TextView ProgressBarText = this.RootView.findViewById(R.id.installer_download_progress_text);

        if (Status.FullSize != -1.0f) { // We have both params - full size and done (in bytes)
            float Percents = (Status.Done / Status.FullSize);
            ProgressBar.setProgress((int)(Percents * 100));

            ProgressBarText.setText(String.format(resources.getString(R.string.installer_progress_bar_full),
                    Utils.BytesToMB(Status.Done), Utils.BytesToMB(Status.FullSize)));
        }else{ // We have only count of proceeded bytes
            ProgressBar.setProgress(0);
            ProgressBarText.setText(String.format(resources.getString(R.string.installer_progress_bar_only_done),
                    Utils.BytesToMB(Status.Done)));
        }
    }

    private void MoveButtonTo(float y, float Speed, boolean IsInit, ButtonAnimCallback Callback){
        Button button = this.RootView.findViewById(R.id.installer_button);
        Animation ButtonAnim = button.getAnimation();

        if (ButtonAnim != null){ // Stop animation
            ButtonAnim.cancel();
        }

        // Convert speed to duration
        long Duration = 0;
        if (this.EnableAnimations && !IsInit) Duration = (long)(Math.abs(y - button.getY()) / Speed);

        button.setEnabled(false);
        Callback.beforeAnim();
        button.animate().setDuration(Duration).y(y).setListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) { }
            public void onAnimationEnd(Animator animation) {
                button.setEnabled(true);
                Callback.onFinished();
            }

            public void onAnimationCancel(Animator animation) { }
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    private LauncherApplication GetApplication(){
        return (LauncherApplication)this._Context.getApplicationContext();
    }

    // Alert
    public static void InstallThroughAlert(Context context){
        // Start install before create alert
        ((LauncherApplication)context.getApplicationContext()).Installer.Install(context);

        // Create server view
        SAMP_InstallerView InstallerView = new SAMP_InstallerView(context);
        //InstallerView.EnableAnimations = false; // Disable animations

        // Create builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("");
        builder.setCancelable(false);

        // Set custom layout
        builder.setView(InstallerView.RootView);

        // Add close button
        builder.setPositiveButton( "In background", (dialog, which) -> dialog.dismiss());

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Bind event ( if user cancel or finish install - change text )
        ((LauncherApplication)context.getApplicationContext()).Installer.Callbacks.add(new SAMPInstallerCallback(){
            public void OnStatusChanged(SAMPInstallerStatus Status) {
                if (Status == SAMPInstallerStatus.CANCELING_INSTALL){
                    dialog.dismiss();
                }
            }

            public void OnDownloadProgressChanged(TaskStatus Status) { }
            public void OnExtractProgressChanged(TaskStatus Status) { }

            public void OnInstallFinished(InstallStatus Status) {
                builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
            }
        });
    }
}
