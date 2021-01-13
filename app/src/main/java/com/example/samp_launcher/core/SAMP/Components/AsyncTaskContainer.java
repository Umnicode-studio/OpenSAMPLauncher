package com.example.samp_launcher.core.SAMP.Components;

import com.example.samp_launcher.core.SAMP.Components.DownloadSystem.DownloadTask;

public class AsyncTaskContainer {
    private final ExtendedAsyncTask AsyncTask;

    public AsyncTaskContainer(ExtendedAsyncTask AsyncTask){
        this.AsyncTask = AsyncTask;
        this.AsyncTask.execute(); // Run task
    }

    public void Cancel(Runnable OnFinish){
        this.AsyncTask.OnCancelFinish_Container = OnFinish;
        this.AsyncTask.Cancel(); // Cancel task
    }
    public DefaultTask GetTask(){
        return this.AsyncTask.GetTask();
    }
}
