package com.home.sms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author appsrox.com
 *
 */
public class MainActivity extends ListActivity {

//	private static final String TAG = "MainActivity";

    private static final int DIALOG_STARTDATE = 1;
    private static final int DIALOG_ENDDATE = 2;
    private static final String ALL = "- Sender -";
    private static final int DIALOG_CLIPBOARD = 3;

    private static final String TEMP_FILE = "smsxp.txt";

    private String[] addressArr;
    private final SimpleDateFormat sdf = new SimpleDateFormat();
    private final Date dt = new Date();
    private Date now;
    private String timePattern;
    private final String datePattern = "dd MMM ";

    private Typeface font;
    private TextView headingText;

    private Dialog filterDialog;
    private int criteriaAddr;
    private String criteriaStartDt;
    private String criteriaEndDt;
    private String criteriaMsg;

    private ImageButton ib4, ib5;

    private String sortOrder;

    private ExportTask task;
    private boolean inprogress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        ib4 = (ImageButton) findViewById(R.id.imageButton4);
        ib5 = (ImageButton) findViewById(R.id.imageButton5);

        font = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Semibold.ttf");
        headingText = (TextView) findViewById(R.id.heading_tv);
        headingText.setTypeface(font);

        Cursor c = getContentResolver().query(SMS.INBOX_URI, new String[]{"DISTINCT address"}, null, null, "address ASC");
        int n = c.getCount()+1;
        addressArr = new String[n];
        addressArr[0] = ALL;
        if (c.moveToFirst()) {
            for (int i=1;i<n;i++) {
                addressArr[i] = c.getString(0);
                c.moveToNext();
            }
        }
        c.close();
    }

    private Cursor createCursor(boolean startManaging) {
        String[] projection = {"_id", "address", "date", "body"};
        String selection = "1=1";
        sdf.applyPattern("yyyy-M-d");

        if (criteriaAddr != 0) {
            selection += " AND address = '"+addressArr[criteriaAddr]+"'";
        }
        if (criteriaStartDt != null) {
            try {
                selection += " AND date > "+sdf.parse(criteriaStartDt).getTime();
            } catch (ParseException e) {}
        }
        if (criteriaEndDt != null) {
            try {
                selection += " AND date < "+sdf.parse(criteriaEndDt).getTime();
            } catch (ParseException e) {}
        }
        if (!TextUtils.isEmpty(criteriaMsg)) {
            selection += " AND body LIKE '%"+criteriaMsg+"%'";
        }

        Cursor c = getContentResolver().query(SMS.INBOX_URI, projection, selection, null, sortOrder);
        if (startManaging) startManagingCursor(c);

        if (c.getCount() > 0) {
            ib4.setEnabled(true);
            ib5.setEnabled(true);
        } else {
            ib4.setEnabled(false);
            ib5.setEnabled(false);
        }

        return c;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("criteriaAddr", criteriaAddr);
        outState.putString("criteriaStartDt", criteriaStartDt);
        outState.putString("criteriaEndDt", criteriaEndDt);
        outState.putString("criteriaMsg", criteriaMsg);
        outState.putBoolean("inprogress", inprogress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        criteriaAddr = state.getInt("criteriaAddr");
        criteriaStartDt = state.getString("criteriaStartDt");
        criteriaEndDt = state.getString("criteriaEndDt");
        criteriaMsg = state.getString("criteriaMsg");
        inprogress = state.getBoolean("inprogress");
    }

    @Override
    protected void onResume() {
        super.onResume();
        now = new Date();
        timePattern = SMS.is24Hours() ? "HH:mm" : "h:mm a";

        String[] from = {"address", "date", "body"};
        int[] to = {R.id.textView1, R.id.textView2, R.id.textView3};
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.row,
                createCursor(true),
                from,
                to);

        adapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                TextView tv = (TextView)view;
                switch(view.getId()) {
                    case R.id.textView2:
                        dt.setTime(cursor.getLong(columnIndex));

                        if (now.getYear()==dt.getYear() && now.getMonth()==dt.getMonth() && now.getDate()==dt.getDate())
                            sdf.applyPattern(timePattern);
                        else if (now.getYear()==dt.getYear())
                            sdf.applyPattern(datePattern + timePattern);
                        else
                            sdf.applyPattern(datePattern + "yyyy " + timePattern);

                        tv.setText(sdf.format(dt));
                        return true;

                    case R.id.textView3:
                        if (!SMS.showFullText()) {
                            tv.setSingleLine(true);
                            tv.setEllipsize(TruncateAt.END);
                        }
                        break;
                }
                return false;
            }
        });
        setListAdapter(adapter);

        if (inprogress) {
            task = new ExportTask();
            task.execute();
        }
    }

    @Override
    protected void onPause() {
        if (task != null) {
            task.finish();
            task = null;
        }
        super.onPause();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton1:
                Intent intent1 = new Intent();
                intent1.setClass(this, SettingsActivity.class);
                startActivity(intent1);
                break;

            case R.id.imageButton2:
                Intent intent2 = new Intent();
                intent2.setClass(this, Compose.class);
                startActivity(intent2);
                break;

            case R.id.imageButton3:
                showDialog(R.id.imageButton3);
                break;

            case R.id.imageButton4:
                showDialog(R.id.imageButton4);
                break;

            case R.id.imageButton5:
                task = new ExportTask();
                task.execute();
                break;

            case R.id.imageButton6:
                showDialog(DIALOG_CLIPBOARD);
                break;

            case R.id.textView1:
                showDialog(DIALOG_STARTDATE);
                break;

            case R.id.textView2:
                showDialog(DIALOG_ENDDATE);
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
            case R.id.imageButton3:
                filterDialog = new AlertDialog.Builder(this)
                        .setTitle("Filter")
                        .setView(initFilterLayout())
                        .setCancelable(true)
                        .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ((SimpleCursorAdapter)getListAdapter()).changeCursor(createCursor(true));
                            }
                        })
                        .setNegativeButton("Clear", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                criteriaAddr = 0;
                                criteriaEndDt = null;
                                criteriaStartDt = null;
                                criteriaMsg = null;
                                ((SimpleCursorAdapter)getListAdapter()).changeCursor(createCursor(true));
                            }
                        })
                        .create();
                return filterDialog;

            case DIALOG_STARTDATE:
            case DIALOG_ENDDATE:
                Calendar cal = Calendar.getInstance();
                DatePickerDialog.OnDateSetListener dateListener =
                        new DatePickerDialog.OnDateSetListener() {
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                String txt = Util.concat(year, "-", monthOfYear+1, "-", dayOfMonth);
                                if (id == DIALOG_STARTDATE) {
                                    criteriaStartDt = txt;
                                    ((TextView)filterDialog.findViewById(R.id.textView1)).setText(criteriaStartDt);
                                } else if (id == DIALOG_ENDDATE) {
                                    criteriaEndDt = txt;
                                    ((TextView)filterDialog.findViewById(R.id.textView2)).setText(criteriaEndDt);
                                }
                            }
                        };
                return new DatePickerDialog(this, dateListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));

            case R.id.imageButton4:
                return new AlertDialog.Builder(this)
                        .setTitle("Sort Options")
                        .setItems(R.array.sort_opts_arr, new  DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which) {
                                    case 0:
                                        sortOrder = "address ASC";
                                        break;
                                    case 1:
                                        sortOrder = "address DESC";
                                        break;
                                    case 2:
                                        sortOrder = "date ASC";
                                        break;
                                    case 3:
                                        sortOrder = "date DESC";
                                        break;
                                }
                                ((SimpleCursorAdapter)getListAdapter()).changeCursor(createCursor(true));
                            }
                        })
                        .create();

            case DIALOG_CLIPBOARD:
                return new AlertDialog.Builder(this)
                        .setTitle("Clipboard")
                        .setView(getLayoutInflater().inflate(R.layout.clipboard, null))
                        .setCancelable(true)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditText et = (EditText) ((Dialog)dialog).findViewById(R.id.editText1);
                                SMS.setClipboardData(et.getText().toString());
                            }
                        })
                        .setNegativeButton("Clear", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditText et = (EditText) ((Dialog)dialog).findViewById(R.id.editText1);
                                et.setText("");
                                SMS.setClipboardData("");
                            }
                        })
                        .create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
            case R.id.imageButton3:
                ((Spinner)dialog.findViewById(R.id.spinner1)).setSelection(criteriaAddr);

                ((TextView)dialog.findViewById(R.id.textView1)).setText(criteriaStartDt!=null ? criteriaStartDt : "Start Date");

                ((TextView)dialog.findViewById(R.id.textView2)).setText(criteriaEndDt!=null ? criteriaEndDt : "End Date");

                ((EditText)dialog.findViewById(R.id.editText1)).setText(criteriaMsg);
                break;

            case DIALOG_CLIPBOARD:
                ((EditText)dialog.findViewById(R.id.editText1)).setText(SMS.getClipboardData());
                break;
        }
    }

    private View initFilterLayout() {
        View root = getLayoutInflater().inflate(R.layout.filter, null);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, addressArr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spn = (Spinner) root.findViewById(R.id.spinner1);
        spn.setAdapter(adapter);
        spn.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                criteriaAddr = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        EditText et = (EditText) root.findViewById(R.id.editText1);
        et.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                criteriaMsg = s.toString();
            }
        });

        return root;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent();
        intent.setClass(this, ReadActivity.class);
        intent.putExtra("id", String.valueOf(id));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File f = new File(Environment.getExternalStorageDirectory(), TEMP_FILE);
            f.delete();
        }

        super.onDestroy();
    }

    //--------------------------------------------------------------------------

    class ExportTask extends AsyncTask<Void, Integer, Uri> {

        ProgressDialog pDialog;
        String errorMsg = "";

        public void finish() {
            cancel(false);
            pDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Exporting to file ...");
            pDialog.setIndeterminate(false);
            pDialog.setMax(100);
            pDialog.setProgress(0);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(false);
            pDialog.show();
            inprogress = true;
        }

        @Override
        protected Uri doInBackground(Void... params) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                FileOutputStream fos = null;
                Cursor cursor = null;
                Calendar cal = Calendar.getInstance();
                DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

                try {
                    File f = new File(Environment.getExternalStorageDirectory(), TEMP_FILE);
                    fos = new FileOutputStream(f);
                    cursor = createCursor(false);
                    int count = cursor.getCount(), i = 0;

                    StringBuilder sb = new StringBuilder();
                    sb.append("====================").append("\n");
                    sb.append("Generated by SMS Xp").append("\n");
                    sb.append("on ").append(df.format(cal.getTime())).append("\n");
                    sb.append("with filters: ").append("\n");
                    if (criteriaAddr != 0) sb.append(addressArr[criteriaAddr]).append("\n");
                    if (criteriaStartDt != null) sb.append("after ").append(criteriaStartDt).append("\n");
                    if (criteriaEndDt != null) sb.append("before ").append(criteriaEndDt).append("\n");
                    if (!TextUtils.isEmpty(criteriaMsg)) sb.append(criteriaMsg).append("\n");
                    sb.append("====================").append("\n");
                    sb.append("\n");
                    fos.write(sb.toString().getBytes());

                    if (cursor.moveToFirst()) {
                        do {
                            sb.setLength(0);
                            cal.setTimeInMillis(cursor.getLong(2)); //date

                            sb.append(cursor.getString(1)).append("\n"); //address
                            sb.append(df.format(cal.getTime())).append("\n");
/*							sb.append("person:" + getVal(cursor, "person")).append(",");
							sb.append("protocol:" + getVal(cursor, "protocol")).append(",");
							sb.append("read:" + getVal(cursor, "read")).append(",");
							sb.append("status:" + getVal(cursor, "status")).append(",");
							sb.append("type:" + getVal(cursor, "type")).append(",");
							sb.append("subject:" + getVal(cursor, "subject")).append("\n");*/
                            sb.append(cursor.getString(3)).append("\n"); //body
                            sb.append("\n");

                            fos.write(sb.toString().getBytes());

                            publishProgress(++i*100/count);
                        } while (!isCancelled() && cursor.moveToNext());
                    }
                    return Uri.fromFile(f);

                } catch (Exception e) {
                    //Log.e(TAG, e.getMessage(), e);
                    errorMsg = "Export failed!";
                } finally {
                    if (cursor != null) cursor.close();
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {}
                    }
                }
            } else {
                errorMsg = "SD card not accessible!";
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Uri result) {
            super.onPostExecute(result);
            inprogress = false;
            pDialog.dismiss();
            task = null;

            if (result == null) {
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                return;
            }

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/*");// text/plain
            shareIntent.putExtra(Intent.EXTRA_STREAM, result);

            startActivity(Intent.createChooser(shareIntent, "Send file to"));
        }

    }

}