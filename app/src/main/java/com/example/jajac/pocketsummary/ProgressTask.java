package com.example.jajac.pocketsummary;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;


public abstract class ProgressTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private ProgressDialog dialog;
    private Context context;

    public ProgressTask(Context context) {
        this.context = context;
        this.dialog = new ProgressDialog(context);
        this.dialog.setMessage("Processing...");
        this.dialog.setCancelable(false);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.dialog.show();
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }
    }
}
