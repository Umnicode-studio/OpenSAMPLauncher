package com.example.samp_launcher.core.SAMP.Components;

import android.os.AsyncTask;

public abstract class ExtendedAsyncTask extends AsyncTask<Void, Void, Void> {
    public Runnable OnCancelFinish_Container = null;

    protected abstract void onCancelled();

    public abstract void Cancel();
    public abstract DefaultTask GetTask();

    protected abstract void Cleanup();
};

