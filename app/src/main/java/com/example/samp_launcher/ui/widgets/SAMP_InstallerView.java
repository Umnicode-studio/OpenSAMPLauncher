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
import com.example.samp_launcher.core.SAMP.Enums.DownloadStatus;
import com.example.samp_launcher.core.SAMP.Enums.InstallStatus;
import com.example.samp_launcher.core.SAMP.SAMPInstaller;
import com.example.samp_launcher.core.SAMP.SAMPInstallerCallback;
import com.example.samp_launcher.core.SAMP.Enums.SAMPInstallerStatus;
import com.example.samp_launcher.core.Utils;

public class SAMP_InstallerView extends LinearLayout {
    private final float BUTTON_ANIM_SPEED = 0.2f; // By 1 px

    private Context _Context;
    private View RootView;
    private float InitialButtonY = 0;
    private boolean IsOnLayoutFired = false;

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

    private void Init(Context context){
        this.RootView = inflate(context, R.layout.samp_installer_view, this);

        // Bind installer Status changing
        LauncherApplication Application = (LauncherApplication)context.getApplicationContext();
        Application.Installer.Callbacks.add(new SAMPInstallerCallback() {
            public void OnStatusChanged(SAMPInstallerStatus Status) {
                Update(context, Application.Installer);
            }
            public void OnDownloadProgressChanged(DownloadStatus Status) {
                Resources resources = context.getResources();
                UpdateDownloadStatus(Status, resources);
            }
            public void OnInstallFinished(InstallStatus Status) {
                // nothing
            }
        });

        this._Context = context;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Get button initial position
        if (!this.IsOnLayoutFired) {
            this.InitialButtonY = this.RootView.findViewById(R.id.installer_button).getY();

            // Force-update status
            boolean Temp = this.EnableAnimations;

            this.EnableAnimations = false;
            this.Update(this._Context, ((LauncherApplication)this._Context.getApplicationContext()).Installer);
            this.EnableAnimations = Temp;

            this.IsOnLayoutFired = true;
        }
    }

    private void Update(Context context, SAMPInstaller Installer){
        SAMPInstallerStatus Status = Installer.GetStatus();

        // Get handles
        TextView Text = this.RootView.findViewById(R.id.installer_status_text);
        RelativeLayout BarLayout = this.RootView.findViewById(R.id.installer_progress_bar_layout);
        Button Button = this.RootView.findViewById(R.id.installer_button);

        Resources resources = context.getResources();

        // Update UI
        System.out.println(Status + " - " + Installer.GetLastInstallStatus());
        if (Status == SAMPInstallerStatus.NONE){ // No install running
            Text.setVisibility(VISIBLE);

            if (SAMPInstaller.IsInstalled(context.getPackageManager(), resources)){ // SAMP installed => do nothing TODO: Export
                Text.setText(resources.getString(R.string.installer_status_none_SAMP_found));
                Text.setTextColor(resources.getColor(R.color.colorOk));

                Button.setVisibility(INVISIBLE);
            }else{
                Text.setTextColor(resources.getColor(R.color.colorError));

                // Check for install errors
                if (Installer.GetLastInstallStatus() == InstallStatus.NETWORK_ERROR){
                    Text.setText(resources.getString(R.string.install_status_network_error));
                    Button.setText(resources.getString(R.string.installer_button_retry));
                }else if (Installer.GetLastInstallStatus() == InstallStatus.UNZIP_ERROR ) {
                    // TODO:
                }else{
                    // If there are no errors, promote to install SAMP
                    Text.setText(resources.getString(R.string.installer_status_none_SAMP_not_found));
                    Button.setText(resources.getString(R.string.installer_button_install));
                }

                Button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        // Install SAMP
                        ((LauncherApplication)context.getApplicationContext()).Installer.Install(context);
                    }
                });

                // Hide bar layout and button with animation
                this.MoveButtonTo(this.InitialButtonY - BarLayout.getHeight(), this.BUTTON_ANIM_SPEED, () -> {
                    BarLayout.setVisibility(INVISIBLE);
                });
            }
        }else if (Status == SAMPInstallerStatus.DOWNLOADING){
            ProgressBar Bar = this.RootView.findViewById(R.id.installer_download_progress);

            // Setup button
            Button.setText(resources.getString(R.string.installer_button_cancel));
            Button.setVisibility(VISIBLE);

            Button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // Cancel SAMP installation
                    ((LauncherApplication)_Context.getApplicationContext()).Installer.CancelInstall();
                }
            });

            // Set text color
            Text.setTextColor(resources.getColor(R.color.colorNone));

            // Setup progress bar
            Bar.setMax(100);

            // Setup progressBar layout and play-animation
            if (BarLayout.getVisibility() == INVISIBLE){
                this.MoveButtonTo(this.InitialButtonY, BUTTON_ANIM_SPEED, () -> { // speed by 1px
                    // Show progress bar and text on it
                    BarLayout.setVisibility(VISIBLE);

                    // Show text
                    Text.setVisibility(VISIBLE);
                });
            }else{
                // Show text
                Text.setVisibility(VISIBLE);
            }

            // Force update download status (used to show current status before it's updated )
            this.UpdateDownloadStatus(Installer.GetDownloadStatus(), resources);
        }
    }

    // Utils
    private void UpdateDownloadStatus(DownloadStatus Status, Resources resources){
        TextView Text = this.RootView.findViewById(R.id.installer_status_text);
        ProgressBar Bar = this.RootView.findViewById(R.id.installer_download_progress);
        TextView ProgressBarText = this.RootView.findViewById(R.id.installer_download_progress_text);

        // Setup values
        Text.setText(String.format(resources.getString(R.string.installer_status_downloading), Status.File, Status.FilesNumber));

        Bar.setProgress((int)(Status.Downloaded / Status.FullSize));

        if (Status.FullSize != -1.0f) {
            ProgressBarText.setText(String.format(resources.getString(R.string.installer_status_downloading_progress_bar_full),
                    Utils.BytesToMB(Status.Downloaded), Utils.BytesToMB(Status.FullSize)));
        }else{
            ProgressBarText.setText(String.format(resources.getString(R.string.installer_status_downloading_progress_bar_only_downloaded),
                    Utils.BytesToMB(Status.Downloaded)));
        }
    }

    private void MoveButtonTo(float y, float Speed, Runnable OnFinish){
        Button button = this.RootView.findViewById(R.id.installer_button);
        Animation ButtonAnim = button.getAnimation();

        if (ButtonAnim != null){ // Stop animation
            ButtonAnim.cancel();
        }

        // Convert speed to duration
        long Duration = 0;
        if (this.EnableAnimations) Duration = (long)(Math.abs(y - button.getY()) / Speed);

        button.setEnabled(false);
        button.animate().setDuration(Duration).y(y).setListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {

            }

            public void onAnimationEnd(Animator animation) {
                button.setEnabled(true);
                OnFinish.run();
            }

            public void onAnimationCancel(Animator animation) {

            }
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    // Alert
    public static void InstallThroughAlert(Context context){
        // Start install before create alert
        ((LauncherApplication)context.getApplicationContext()).Installer.Install(context);

        // Create server view
        SAMP_InstallerView InstallerView = new SAMP_InstallerView(context);
        InstallerView.EnableAnimations = false; // Disable animations

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
            public void OnStatusChanged(SAMPInstallerStatus Status) { }

            public void OnDownloadProgressChanged(DownloadStatus Status) { }
            public void OnInstallFinished(InstallStatus Status) {
                if (Status == InstallStatus.CANCELED){
                    dialog.dismiss();
                }else{
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                }
            }
        });
    }
}
