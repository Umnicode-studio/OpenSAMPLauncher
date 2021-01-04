package com.example.samp_launcher.ui.widgets;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.samp_launcher.LauncherApplication;
import com.example.samp_launcher.R;
import com.example.samp_launcher.core.SAMP.DownloadState;
import com.example.samp_launcher.core.SAMP.SAMPInstaller;
import com.example.samp_launcher.core.SAMP.SAMPInstallerCallback;
import com.example.samp_launcher.core.SAMP.SAMPInstallerState;
import com.example.samp_launcher.core.SAMP.UnzipState;
import com.example.samp_launcher.ui.widgets.playbutton.SAMPLaunchCallback;

import java.text.DecimalFormat;

public class SAMP_InstallerView extends LinearLayout {
    private Context _Context;
    private View RootView;
    private interface Lambda{
        void Run();
    }
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

    private void Update(Context context, SAMPInstaller Installer){
        SAMPInstallerState State = Installer.GetState();

        // Get handles
        TextView Text = this.RootView.findViewById(R.id.installer_state_text);
        RelativeLayout BarLayout = this.RootView.findViewById(R.id.installer_progress_bar_layout);
        Button Button = this.RootView.findViewById(R.id.installer_button);

        Resources resources = context.getResources();

        // Update UI
        if (State == SAMPInstallerState.NONE){ // No install running
            Text.setVisibility(VISIBLE);

            if (SAMPInstaller.IsInstalled(context.getPackageManager(), resources)){ // SAMP installed => do nothing
                Text.setText(resources.getString(R.string.installer_state_none_SAMP_found));
                Text.setTextColor(resources.getColor(R.color.colorOk));

                Button.setVisibility(INVISIBLE);
            }else{ // SAMP not found => show button to install
                Text.setText(resources.getString(R.string.installer_state_none_SAMP_not_found));
                Text.setTextColor(resources.getColor(R.color.colorError));

                Button.setText(resources.getString(R.string.installer_button_install));
                Button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        // Install SAMP
                        ((LauncherApplication)context.getApplicationContext()).Installer.Install(context);
                    }
                });

                // Hide bar layout and button with animation
                BarLayout.setVisibility(INVISIBLE);

                long Duration = this.EnableAnimations ? 500 : 0;

                Button.setEnabled(false);
                Button.animate().translationYBy(-BarLayout.getHeight()).setDuration(Duration).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        Button.setEnabled(true);
                    }
                });
            }
        }else if (State == SAMPInstallerState.DOWNLOADING){
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

            // Setup progressbar layout and play-animation
            if (BarLayout.getVisibility() == INVISIBLE){
                long Duration = this.EnableAnimations ? 500 : 0;

                Button.setEnabled(false);
                Button.animate().translationYBy(BarLayout.getHeight()).setDuration(Duration).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        Button.setEnabled(true);

                        // Show progress bar and text on it
                        BarLayout.setVisibility(VISIBLE);

                        // Show text
                        Text.setVisibility(VISIBLE);
                    }
                });
            }else{
                // Show text
                Text.setVisibility(VISIBLE);
            }

            this.UpdateDownloadState(Installer.GetDownloadState(), resources);
        }
    }

    private void Init(Context context){
        this.RootView = inflate(context, R.layout.samp_installer_view, this);

        // Bind installer state changing
        LauncherApplication Application = (LauncherApplication)context.getApplicationContext();
        Application.Installer.Callbacks.add(new SAMPInstallerCallback() {
            public void OnStateChanged(SAMPInstallerState State) {
                Update(context, Application.Installer);
            }

            public void OnDownloadProgressChanged(DownloadState State) {
                Resources resources = context.getResources();
                UpdateDownloadState(State, resources);
            }

            public void OnUnzipProgressChanged(UnzipState State) { }
            public void InstallFinished() { }
            public void InstallCanceled() { }
        });

        // Set init state
        boolean Temp = this.EnableAnimations;

        this.EnableAnimations = false;
        this.Update(context, Application.Installer);

        this.EnableAnimations = Temp;

        this._Context = context;
    }

    // Utils
    private void UpdateDownloadState(DownloadState State, Resources resources){
        TextView Text = this.RootView.findViewById(R.id.installer_state_text);
        ProgressBar Bar = this.RootView.findViewById(R.id.installer_download_progress);
        TextView ProgressBarText = this.RootView.findViewById(R.id.installer_download_progress_text);

        // Setup values
        Text.setText(String.format(resources.getString(R.string.installer_state_downloading), State.File, State.FilesNumber));

        Bar.setProgress((int)(State.Downloaded / State.FullSize));
        ProgressBarText.setText(String.format(resources.getString(R.string.installer_state_downloading_progress_bar),
                State.Downloaded, State.FullSize));
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
            public void OnStateChanged(SAMPInstallerState State) { }

            public void OnDownloadProgressChanged(DownloadState State) { }
            public void OnUnzipProgressChanged(UnzipState State) { }

            public void InstallFinished() {
                builder.setPositiveButton( "Close", (dialog, which) -> dialog.dismiss());
            }
            public void InstallCanceled() {
                dialog.dismiss();
            }
        });
    }
}
