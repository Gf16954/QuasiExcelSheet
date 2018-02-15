package com.gf169.gfexceltable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB {

    public static  class DatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG = "DatabaseHelper";

        private static final int DATABASE_VERSION = 4;
        private static final String DATABASE_NAME = "db";
        private static final int nTables=4;

        private static void createTable(SQLiteDatabase db, int iTable) {
            int nFields=iTable==0 ? 10 :  // Number of fields minus 1
                        iTable==1 ? 8 :
                        iTable==2 ? 8 :
                        iTable==3 ? 2 :0;
            String s="CREATE TABLE IF NOT EXISTS "+getTableName(iTable)
                    +" (_id integer PRIMARY KEY autoincrement";
            for (int i=1; i<nFields+1; i++) {
                s+=" ,Column"+i;
            }
            db.execSQL(s+")");
        }
        private static void dropTable(SQLiteDatabase db, int iTable) {
            db.execSQL("DROP TABLE IF EXISTS "+getTableName(iTable));
        }
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            for (int i=0; i<nTables; i++) {
                createTable(db, i);
            }
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for (int i=0; i<nTables; i++) {
                dropTable(db,i);
                createTable(db,i);
            }
        }
    }
    public static DatabaseHelper helper;
    public static SQLiteDatabase db=null;

    public static void dbInit(Context context) {
        if (db==null) {
            helper=new DatabaseHelper(context);
            db=helper.getWritableDatabase();
        }
    }
    public static String[] getTableColumnNames(int iTable) {
        return db.query(getTableName(iTable),
                null, null, null, null, null, null,
                "1")
                .getColumnNames();
    }
    public static String getTableName(int iTable) {
        if (iTable<4) return "Sheet"+(iTable+1);
        return null;
    }
}
