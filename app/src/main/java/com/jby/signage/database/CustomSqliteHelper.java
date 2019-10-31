package com.jby.signage.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by user on 3/11/2018.
 */

public class CustomSqliteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Signage";
    private static final int DATABASE_VERSION = 12;

    public static final String TB_GALLERY = "tb_gallery";

    private static final String CREATE_TB_GALLERY = "CREATE TABLE " + TB_GALLERY +
            "(local_gallery_id INTEGER PRIMARY KEY," +
            "link_id INTEGER," +
            "gallery_id INTEGER," +
            "path Text," +
            "priority Text," +
            "timer Text," +
            "display_type Text," +
            "status INTEGER DEFAULT 0," +
            "default_display Text," +
            "created_at Text," +
            "updated_at Text)";


    public CustomSqliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TB_GALLERY);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TB_GALLERY);
        onCreate(sqLiteDatabase);
    }
}
