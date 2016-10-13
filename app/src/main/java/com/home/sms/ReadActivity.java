package com.home.sms;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;



public class ReadActivity extends AppCompatActivity {
TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        tv=(TextView)findViewById(R.id.textView1);
        String id=getIntent().getStringExtra("id");
        String[] projection={"_id","address","date","body"};
        String selection="_id=?";
        String[] selectionArgs={id};
        Cursor c=getContentResolver().query(SMS.INBOX_URI,projection,selection,selectionArgs,null);
        if(c.moveToFirst()){
            setTitle(c.getString(c.getColumnIndex("address")));
            tv.setText(c.getString(c.getColumnIndex("body")));

        }
    }
    public void onClick(View v){
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/*");// text/plain
        shareIntent.putExtra(Intent.EXTRA_TEXT, tv.getText().toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "subject"); // TODO
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}
