package com.example.samp_launcher.ui.widgets;
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

import com.example.samp_launcher.LauncherApplication;
import com.example.samp_launcher.R;
import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.SAMPInstaller;
import com.example.samp_launcher.core.SAMP.SAMPInstallerCallback;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;
import com.example.samp_launcher.core.Utils;

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

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        System.out.println("Attached");
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
            public void OnDownloadProgressChanged(DownloadStatus Status) {
                Resources resources = context.getResources();
                UpdateDownloadStatus(Status, resources);
            }
            public void OnInstallFinished(InstallStatus Status) { }
        };

        // Bind installer Status changing
        Application.Installer.Callbacks.add(this.Callback);
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
            ProgressBar Bar = this.RootView.findViewById(R.id.installer_download_progress);

            // Setup button
            Button.setText(resources.getString(R.string.installer_button_cancel));
            Button.setVisibility(VISIBLE);

            this.BindButtonAsCancel();

            // Set text color
            Text.setTextColor(resources.getColor(R.color.colorNone));

            // Setup progress bar
            Bar.setMax(100);

            // Setup progressBar layout and play-animation
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

            // Force update download status (used to show current status before it's updated )
            this.UpdateDownloadStatus(Installer.GetDownloadStatus(), resources);
        }else if (Status == SAMPInstallerStatus.PREPARING){
            // Setup label
            Text.setTextColor(resources.getColor(R.color.colorNone));
            Text.setText(resources.getString(R.string.installer_status_preparing));

            // Setup button
            Button.setText(resources.getString(R.string.installer_button_cancel));
            Button.setVisibility(VISIBLE);

            this.BindButtonAsCancel();
            this.HideBarLayout(IsInit, true);
        } else{ // No install running ( CANCELING_INSTALL || NONE )
            if (SAMPInstaller.IsInstalled(context.getPackageManager(), resources)){ // SAMP installed => do nothing TODO: Export
                Text.setText(resources.getString(R.string.installer_status_none_SAMP_found));
                Text.setTextColor(resources.getColor(R.color.colorOk));

                Button.setVisibility(INVISIBLE);
            }else{
                Text.setTextColor(resources.getColor(R.color.colorError));

                // Check for previous install errors
                if (Installer.GetLastInstallStatus() == InstallStatus.DOWNLOADING_ERROR){
                    Text.setText(resources.getString(R.string.install_status_network_error));
                    Button.setText(resources.getString(R.string.installer_button_retry));
                }else if (Installer.GetLastInstallStatus() == InstallStatus.UNZIP_ERROR ) {
                    // TODO: UNZIP error message
                }else{
                    // If there are no errors, promote to install SAMP
                    Text.setText(resources.getString(R.string.installer_status_none_SAMP_not_found));
                    Button.setText(resources.getString(R.string.installer_button_install));
                }

                Button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        // Install SAMP
                        GetApplication().Installer.Install(context);
                    }
                });

                this.HideBarLayout(IsInit, Status == SAMPInstallerStatus.CANCELING_INSTALL);
            }
        }
    }

    // UI
    private void HideBarLayout(boolean IsInit, boolean DisableButton){
        RelativeLayout BarLayout = this.RootView.findViewById(R.id.installer_progress_bar_layout);
        Button Button = this.RootView.findViewById(R.id.installer_button);

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

    private void BindButtonAsCancel(){
        Button Button = this.RootView.findViewById(R.id.installer_button);

        // Set click listener
        Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Cancel SAMP installation
                GetApplication().Installer.CancelInstall();
            }
        });
    }

    // Utils
    private void UpdateDownloadStatus(DownloadStatus Status, Resources resources){
        TextView Text = this.RootView.findViewById(R.id.installer_status_text);
        ProgressBar Bar = this.RootView.findViewById(R.id.installer_download_progress);
        TextView ProgressBarText = this.RootView.findViewById(R.id.installer_download_progress_text);

        // Setup values
        Text.setText(String.format(resources.getString(R.string.installer_status_downloading), Status.File, Status.FilesNumber));

        if (Status.FullSize != -1.0f) {
            float Percents = (Status.Downloaded / Status.FullSize);
            Bar.setProgress((int)(Percents * 100));

            ProgressBarText.setText(String.format(resources.getString(R.string.installer_status_downloading_progress_bar_full),
                    Utils.BytesToMB(Status.Downloaded), Utils.BytesToMB(Status.FullSize)));
        }else{
            Bar.setProgress(0);
            ProgressBarText.setText(String.format(resources.getString(R.string.installer_status_downloading_progress_bar_only_downloaded),
                    Utils.BytesToMB(Status.Downloaded)));
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

            public void OnDownloadProgressChanged(DownloadStatus Status) { }
            public void OnInstallFinished(InstallStatus Status) {
                builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
            }
        });
    }
}
