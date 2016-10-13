package com.home.sms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * @author appsrox.com
 *
 */
public class Compose extends Activity {

//	private static final String TAG = "ComposeActivity";

    private static final String ACTION_SENT = "com.appsrox.smsxp.SENT";
    private static final int DIALOG_SENDTO = 1;
    private static final int DIALOG_CLEAR = 2;

    private EditText et1;
    private ImageButton ib1, ib2, ib3;

    boolean isSentPending;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        setTitle("New Message");
        et1 = (EditText) findViewById(R.id.editText1);
        ib1 = (ImageButton) findViewById(R.id.imageButton1);
        ib2 = (ImageButton) findViewById(R.id.imageButton2);
        ib3 = (ImageButton) findViewById(R.id.imageButton3);

        et1.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged
                    (CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                reset();
            }
        });
    }

    private void reset() {
        if (TextUtils.isEmpty(et1.getText().toString())) {
            ib1.setEnabled(false);
            ib3.setEnabled(false);
        } else {
            ib1.setEnabled(true);
            ib3.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reset();
        if (TextUtils.isEmpty(SMS.getClipboardData()))
            ib2.setEnabled(false);
        else
            ib2.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSentPending)
            unregisterReceiver(sent);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton1:
                if (SMS.showConfirmDialog()) {
                    showDialog(DIALOG_CLEAR);
                } else {
                    et1.setText("");
                }
                break;

            case R.id.imageButton2:
                et1.getText().insert(et1.getSelectionStart(), SMS.getClipboardData());
                break;

            case R.id.imageButton3:
                if (SMS.useDefaultApp()) {
                    try {
                        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                        sendIntent.putExtra("sms_body", et1.getText().toString());
                        sendIntent.setType("vnd.android-dir/mms-sms");
                        startActivity(sendIntent);

                    } catch (ActivityNotFoundException e) {
                        SMS.sp.edit().putBoolean(SMS.USE_DEFAULT, false).commit();
                        showDialog(DIALOG_SENDTO);
                    }

                } else {
                    showDialog(DIALOG_SENDTO);
                }
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SENDTO:
                final EditText et = new EditText(this);
                et.setInputType(EditorInfo.TYPE_CLASS_PHONE);
                return new AlertDialog.Builder(this)
                        .setTitle("To")
                        .setView(et)
                        .setCancelable(true)
                        .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String to = et.getText().toString().trim();
                                if (!TextUtils.isEmpty(to)) {
                                    sendSMS(to);
                                } else {
                                    Toast.makeText(Compose.this, "To cannot be empty!", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .create();

            case DIALOG_CLEAR:
                return new AlertDialog.Builder(this)
                        .setTitle("Clear")
                        .setMessage("Are you sure?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                et1.setText("");
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .create();
        }
        return super.onCreateDialog(id);
    }

    private void sendSMS(String to) {
        PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SENT), 0);

        registerReceiver(sent, new IntentFilter(ACTION_SENT));
        isSentPending = true;

        SmsManager manager = SmsManager.getDefault() ;
        manager.sendTextMessage(to, null, et1.getText().toString(), sentIntent, null);
    }

    private BroadcastReceiver sent = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(Compose.this, "Sent successfully", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(Compose.this, "Error sending SMS", Toast.LENGTH_LONG).show();
                    break;
            }

            unregisterReceiver(this);
            isSentPending = false;
        }
    };

}


