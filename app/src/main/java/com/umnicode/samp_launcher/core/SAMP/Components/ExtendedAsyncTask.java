package com.umnicode.samp_launcher.core.SAMP.Components;

import android.os.AsyncTask;

public abstract class ExtendedAsyncTask extends AsyncTask<Void, Void, Void> {
    public Runnable AfterCancelled;

    public abstract void Cancel(Runnable AfterFinished);
    public abstract DefaultTask GetTask();

    protected abstract void Cleanup(boolean IsCancelled);
};

