package com.jby.signage.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.jby.signage.shareObject.CustomToast.CustomToast;

public class FrameworkClass {
    private Context context;
    private SQLiteOpenHelper sqLiteOpenHelper;
    private String table;
    private static final String TAG = "FrameworkClass";

    public FrameworkClass(Context context, SQLiteOpenHelper sqLiteOpenHelper, String table) {
        this.context = context;
        this.sqLiteOpenHelper = sqLiteOpenHelper;
        this.table = table;
    }

    private ContentValues getContentValues(String[] storeColumn, String[] storeValue) {
        ContentValues contentValues = new ContentValues();
        if (storeColumn.length == storeValue.length)
            for (int i = 0; i < storeColumn.length; i++) {
                contentValues.put(storeColumn[i], storeValue[i]);
            }
        return contentValues;
    }

    private String[] getArrayFromString(String input) {
        if (!input.equals("")) {
            String[] stringArray = input.split(",");
            for (int i = 0; i < stringArray.length; i++) {
                stringArray[i] = stringArray[i].trim();
            }
            return stringArray;
        } else return null;
    }
    /*-----------------------------------------------------------Create----------------------------------------------------------------------*/
    //store chat detail into chat room

    public class Create {
        SQLiteDatabase db;
        String storeColumn, storeValue;
        String[] storeColumnArray, storeValueArray, storeValueInArray;

        public Create(String storeColumn, String storeValue) {
            this.db = sqLiteOpenHelper.getWritableDatabase();
            this.storeColumn = storeColumn;
            this.storeValue = storeValue;
        }

        public Create(String storeColumn, String[] storeValueInArray) {
            this.db = sqLiteOpenHelper.getWritableDatabase();
            this.storeColumn = storeColumn;
            this.storeValueInArray = storeValueInArray;
        }

        public void perform(ResultCallBack.OnCreate resultCallBack) {
            ContentValues contentValues;
            if (storeValueInArray != null ? getStoreColumnArray(storeColumn).length != storeValueInArray.length : getStoreColumnArray(storeColumn).length != getStoreValueArray(storeValue).length) {
                CustomToast(context, "Number of parameter not matched!");
                return;
            } else
                contentValues = getContentValues(getStoreColumnArray(storeColumn), storeValueInArray != null ? storeValueInArray : getStoreValueArray(storeValue));
            //perform
            try {
                if (contentValues != null) db.insert(table, null, contentValues);
                //read result
                resultCallBack.createResult("Success");
            } catch (SQLException e) {
                //read result
                resultCallBack.createResult("Fail");
                CustomToast(context, "Invalid Parameter!");
            }
        }

        private String[] getStoreColumnArray(String storeColumn) {
            storeColumnArray = storeColumn.split(",");
            for (int i = 0; i < storeColumnArray.length; i++) {
                storeColumnArray[i] = storeColumnArray[i].trim();
            }
            return storeColumnArray;
        }

        private String[] getStoreValueArray(String storeValue) {
            storeValueArray = storeValue.split(",");
            for (int i = 0; i < storeValueArray.length; i++) {
                storeValueArray[i] = storeValueArray[i].trim();
            }
            return storeValueArray;
        }
    }

    /*---------------------------------------------------------------read-----------------------------------------------------------------*/
    public class Read {
        private String selectColumn = null;
        private SQLiteDatabase db;
        private String query = "";
        private String[] selectColumnArray;

        public Read(String selectColumn) {
            this.selectColumn = selectColumn;
            db = sqLiteOpenHelper.getReadableDatabase();
            select();
        }

        //select query
        private void select() {
            query = "SELECT  " + selectColumnValue(selectColumn) + "FROM " + table;
        }

        //left join table
        public Read leftJoinTable(String leftJoinTable) {
            if (leftJoinTable != null) query += " LEFT JOIN " + leftJoinTable;
            return this;
        }

        public Read leftJoinTableCondition(String condition) {
            if (condition != null) query += " ON " + condition;
            return this;
        }

        //where query
        public Read where(String whereCondition) {
            if (whereCondition != null) query += " WHERE " + whereCondition;
            return this;
        }

        //ascending order
        public Read orderByAsc(String column) {
            if (column != null) query += " ORDER BY " + column + " ASC";
            return this;
        }

        //descending order
        public Read orderByDesc(String column) {
            if (column != null) query += " ORDER BY " + column + " DESC";
            return this;
        }

        //descending order
        public Read limitBy(String limit) {
            if (limit != null) query += " LIMIT " + limit;
            return this;
        }

        //count row
        public void count(ResultCallBack.OnCount resultCallBack) {
            int count;
            try {
                //perform read action
                Cursor crs = db.rawQuery(query, null);
                count = crs.getCount();

                resultCallBack.countResult(count);
                crs.close();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FrameworkClass","Counting Error!");
                resultCallBack.countResult(-1);
                CustomToast(context, "Invalid Parameter!");
            }

        }

        //perform select action
        public void perform(ResultCallBack.OnRead resultCallBack) {
            StringBuilder result = new StringBuilder();
            try {
                //perform read action
                Cursor crs = db.rawQuery(query, null);
                //bind result
                while (crs.moveToNext()) {
                    //first
                    if (crs.getPosition() == 0)
                        //only one item
                        if (crs.getCount() == 1)
                            result.append("{ result:[").append(bindResult(crs, selectColumn)).append("]}");
                        else result.append("{ result:[").append(bindResult(crs, selectColumn));
                        //last
                    else if (crs.getPosition() == crs.getCount() - 1)
                        result.append(",").append(bindResult(crs, selectColumn)).append("]}");
                    else
                        result.append(",").append(bindResult(crs, selectColumn));
                }
                db.close();
                crs.close();

            } catch (SQLException e) {
                resultCallBack.readResult("Fail");
                CustomToast(context, "Invalid Parameter!");
                return;
            }
            catch (IllegalStateException e){
                return;
            }
            resultCallBack.readResult(result.toString().equals("") ? "Nothing" : result.toString());
        }

        //bind fetch result
        private String bindResult(Cursor cursor, String selectColumn) {
            StringBuilder bindResult = new StringBuilder("{");
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                //if select all
                if (selectColumn.equals("*")) {
                    if (i == cursor.getColumnCount() - 1)
                        bindResult.append(cursor.getColumnName(i)).append(":\"").append(cursor.getString(cursor.getColumnIndex(cursor.getColumnName(i)))).append("\"}");
                    else
                        bindResult.append(cursor.getColumnName(i)).append(":\"").append(cursor.getString(cursor.getColumnIndex(cursor.getColumnName(i)))).append("\",");
                }
                //else
                else {
                    if (i == cursor.getColumnCount() - 1) {
                        bindResult.append(cursor.getColumnName(i)).append(":\"").append(cursor.getString(cursor.getColumnIndex(selectColumnArray[i]))).append("\"}");
                    } else {
                        bindResult.append(cursor.getColumnName(i)).append(":\"").append(cursor.getString(cursor.getColumnIndex(selectColumnArray[i]))).append("\",");
                    }
                }
            }
            return bindResult.toString();
        }

        //set select field
        private String selectColumnValue(String selectColumn) {
            StringBuilder selectColumns = new StringBuilder();
            //if select all then return *
            if (selectColumn.equals("*")) {
                return "*";
            }
            //else have to go through every select columns
            else {
                try {
                    //convert SELECT column from string to array
                    selectColumnArray = getSelectColumnArray(selectColumn);
                    for (int i = 0; i < selectColumnArray.length; i++) {
                        //if i != last item
                        if (i != selectColumnArray.length - 1)
                            selectColumns.append(selectColumnArray[i]).append(" , ");
                            //if i == last item
                        else selectColumns.append(selectColumnArray[i]).append(" ");
                    }
                } catch (IndexOutOfBoundsException e) {
                    Log.d("", "" + e);
                }
                return selectColumns.toString();
            }
        }

        private String[] getSelectColumnArray(String selectColumn) {
            selectColumnArray = selectColumn.split(",");
            for (int i = 0; i < selectColumnArray.length; i++) {
                selectColumnArray[i] = selectColumnArray[i].trim();
            }
            return selectColumnArray;
        }
    }

    /*---------------------------------------------------------------edit------------------------------------------------------------------*/
    public class Update {
        SQLiteDatabase db;
        String updateColumn, updateValue, whereValue = "";
        String[] updateColumnArray, updateValueArray;

        String where = null;

        public Update(String updateColumn, String updateValue) {
            this.db = sqLiteOpenHelper.getWritableDatabase();
            this.updateColumn = updateColumn;
            this.updateValue = updateValue;
        }

        public Update where(String whereCondition, String whereValue) {
            where = whereCondition;
            this.whereValue = whereValue;
            return this;
        }

        public void perform(ResultCallBack.OnUpdate resultCallBack) {
            ContentValues contentValues;
            updateColumnArray = getArrayFromString(updateColumn);
            updateValueArray = getArrayFromString(updateValue);
            //length not match then stop
            assert updateValueArray != null;
            if (updateColumnArray.length != updateValueArray.length) {
                CustomToast(context, "Number of parameter not matched!");
                return;
            } else contentValues = getContentValues(updateColumnArray, updateValueArray);
            try {
                if (contentValues != null)
                    db.update(table, contentValues, where, getArrayFromString(whereValue));

                resultCallBack.updateResult("Success");
            } catch (SQLException e) {
                resultCallBack.updateResult("Fail");
                CustomToast(context, "Invalid Parameter!");
            } catch (IllegalArgumentException e) {
                resultCallBack.updateResult("Fail");
                CustomToast(context, "Format Not Matched!");
            }
        }
    }

    public class Delete {
        SQLiteDatabase db;
        String whereValue = "";
        String where = "";

        public Delete() {
            this.db = sqLiteOpenHelper.getWritableDatabase();
        }

        public Delete where(String whereCondition, String whereValue) {
            where = whereCondition;
            this.whereValue = whereValue;
            return this;
        }

        public long perform(ResultCallBack.OnDelete resultCallBack) {
            long result = -1;
            try {
                result = db.delete(table, where, getArrayFromString(whereValue));
                //delete result
                resultCallBack.deleteResult("Success");
            } catch (SQLException e) {
                //delete result
                resultCallBack.deleteResult("Fail");
                CustomToast(context, "Invalid Parameter!");
            }
            return result;
        }
    }
}
