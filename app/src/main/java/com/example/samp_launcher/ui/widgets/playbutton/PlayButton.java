package com.example.samp_launcher.ui.widgets.playbutton;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

import com.example.samp_launcher.LauncherApplication;
import com.example.samp_launcher.R;
import com.example.samp_launcher.core.SAMP.DownloadStatus;
import com.example.samp_launcher.core.SAMP.SAMPInstaller;
import com.example.samp_launcher.core.SAMP.SAMPInstallerCallback;
import com.example.samp_launcher.core.SAMP.SAMPInstallerStatus;
import com.example.samp_launcher.core.SAMP.UnzipStatus;
import com.example.samp_launcher.core.ServerConfig;
import com.example.samp_launcher.ui.widgets.SAMP_InstallerView;

import kotlin.jvm.internal.Lambda;

public class PlayButton extends androidx.appcompat.widget.AppCompatButton {
    private PlayButtonAction Action;
    private ServerConfig Config;

    private SAMPLaunchCallback OnSAMPLaunch;
    private Context _Context;

    public PlayButton(Context context) {
        super(context);
        Init(context);
    }
    public PlayButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Init(context);
    }
    public PlayButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init(context);
    }

    public void SetServerConfig(ServerConfig Config){
        this.Config = Config;
        Resources resources = this._Context.getResources();

        if (!this.isInEditMode()) {
            // Check for SAMP ( but do not update action, because we check for this in init() and installer listener )
            if (!SAMPInstaller.IsInstalled(this._Context.getPackageManager(), resources)) {
                return;
            }

            // Check does server config is correct
            if (ServerConfig.IsStatusError(Config.Status)) {
                UpdateAction(PlayButtonAction.SHOW_SERVER_INCORRECT);
                return;
            }

            this.UpdateAction(PlayButtonAction.LAUNCH_SAMP);
        }else{
            this.setText("[In editor preview]"); // Set preview text for editor
        }
    }

    public PlayButtonAction GetAction(){
        return this.Action;
    }

    public void SetOnSAMPLaunchCallback(SAMPLaunchCallback Callback){
        this.OnSAMPLaunch = Callback;
    }
    private void Init(Context context){
        this._Context = context;
        this.SetServerConfig(new ServerConfig());

        // Bind on clicked
        this.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Action == PlayButtonAction.INSTALL_SAMP){
                    SAMP_InstallerView.InstallThroughAlert(_Context);
                }else if (Action == PlayButtonAction.INSTALL_SAMP_APK){
                    ((LauncherApplication)_Context.getApplicationContext()).Installer.OpenInstalledAPK();
                }else if (Action == PlayButtonAction.LAUNCH_SAMP){
                    OnSAMPLaunch.Launch();
                }
            }
        });

        // Force update
        this.UpdateActionByInstallerStatus(this.GetApplication().Installer.GetStatus(), true);
        System.out.println("Installer Status " + this.GetApplication().Installer.GetStatus());

        // Bind to installer Status change
        ((LauncherApplication)this._Context.getApplicationContext()).Installer.Callbacks.add(new SAMPInstallerCallback() {
            public void OnStatusChanged(SAMPInstallerStatus Status) {
                UpdateActionByInstallerStatus(Status, false);
            }
            public void OnDownloadProgressChanged(DownloadStatus Status) {
                if (Action == PlayButtonAction.SHOW_DOWNLOAD_Status) {
                    UpdateDownloadStatus(Status);
                }
            }

            public void OnUnzipProgressChanged(UnzipStatus Status) {
                if (Action == PlayButtonAction.SHOW_UNZIPPING_STATUS){
                    //TODO: Complete this
                }
            }

            public void InstallFinished() {
                UpdateAction(PlayButtonAction.LAUNCH_SAMP);
            }
            public void InstallCanceled() {
                UpdateAction(PlayButtonAction.INSTALL_SAMP);
            }
        });
    }

    // Utils
    private LauncherApplication GetApplication(){
        return (LauncherApplication)this._Context.getApplicationContext();
    }

    private void UpdateDownloadStatus(DownloadStatus Status){
        this.setText(String.format(this._Context.getResources().getString(R.string.play_button_show_download_Status),
                Status.File, Status.FilesNumber));
    }

    private void UpdateActionByInstallerStatus(SAMPInstallerStatus Status, boolean ProceedNone){
        if (Status == SAMPInstallerStatus.DOWNLOADING){
            UpdateAction(PlayButtonAction.SHOW_DOWNLOAD_Status);
        }else if (Status == SAMPInstallerStatus.WAITING_FOR_APK_INSTALL){
            UpdateAction(PlayButtonAction.INSTALL_SAMP_APK);
        }else if (Status == SAMPInstallerStatus.NONE){
            if (ProceedNone){
                if (SAMPInstaller.IsInstalled(this._Context.getPackageManager(), this._Context.getResources())){
                    UpdateAction(PlayButtonAction.LAUNCH_SAMP);
                }else{
                    UpdateAction(PlayButtonAction.INSTALL_SAMP);
                }
            }
        }
    }

    private void UpdateAction(PlayButtonAction NewAction){
        System.out.println("New action is " + NewAction);

        Resources resources = this._Context.getResources();

        // Active actions
        if (NewAction == PlayButtonAction.INSTALL_SAMP){
            this.setText(resources.getString(R.string.play_button_install_SAMP));
            this.setTextColor(resources.getColor(R.color.colorError));
            this.setEnabled(true);
        }else if (NewAction == PlayButtonAction.INSTALL_SAMP_APK){
            //TODO: Create this
        } else if (NewAction == PlayButtonAction.LAUNCH_SAMP){
            // If everything is ok - we can launch game by clicking button
            this.setText(resources.getString(R.string.play_button_launch_SAMP));
            this.setTextColor(resources.getColor(R.color.colorOk));
            this.setEnabled(true);
        }

        // Passive actions
        else if (NewAction == PlayButtonAction.SHOW_SERVER_INCORRECT){
            this.setText(resources.getString(R.string.play_button_server_incorrect));
            this.setTextColor(resources.getColor(R.color.colorError));
            this.setEnabled(false);
        }else if (NewAction == PlayButtonAction.SHOW_UNZIPPING_STATUS){
            // TODO: Complete this
        } else if (NewAction == PlayButtonAction.SHOW_DOWNLOAD_Status){
            this.UpdateDownloadStatus(this.GetApplication().Installer.GetDownloadStatus());
            this.setTextColor(resources.getColor(R.color.colorError));

            this.setEnabled(false);
        }

        this.Action = NewAction;
    }
}