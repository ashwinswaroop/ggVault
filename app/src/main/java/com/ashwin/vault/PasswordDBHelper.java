package com.ashwin.vault;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.R.attr.id;

public class PasswordDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "passwordDB";

    private static final String TABLE_PASSWORD = "Password";

    private static final String PASSWORD = "password";

    public PasswordDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_PASSWORD + "("
                + PASSWORD + " TEXT PRIMARY KEY"+ ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORD);
        onCreate(db);
    }

    void addPassword(String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PASSWORD, password);
        db.insert(TABLE_PASSWORD, null, values);
        db.close();
    }

    String getPassword() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT password FROM " + TABLE_PASSWORD , null);
        cursor.moveToFirst();
        return cursor.getString(0);
    }

    boolean isEmpty(){
        boolean empty = true;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PASSWORD, null);
        if (cur != null && cur.moveToFirst()) {
            empty = (cur.getInt (0) == 0);
        }
        cur.close();

        return empty;
    }



}