package com.ashwin.vault;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.R.attr.id;

public class KeyDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "keyDB";

    private static final String TABLE_KEY = "Key";

    private static final String KEY = "key";

    public KeyDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_KEY + "("
                + KEY + " BLOB PRIMARY KEY"+ ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEY);
        onCreate(db);
    }

    void addKey(byte[] key) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY, key);
        db.insert(TABLE_KEY, null, values);
        db.close();
    }

    byte[] getKey() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT key FROM " + TABLE_KEY , null);
        cursor.moveToFirst();
        return cursor.getBlob(0);
    }

    boolean isEmpty(){
        boolean empty = true;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_KEY, null);
        if (cur != null && cur.moveToFirst()) {
            empty = (cur.getInt (0) == 0);
        }
        cur.close();

        return empty;
    }



}