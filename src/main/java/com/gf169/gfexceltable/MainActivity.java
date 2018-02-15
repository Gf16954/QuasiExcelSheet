package com.gf169.gfexceltable;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public final String TAG = "gfMainActivity";

    int nRows = 25;  // Number of rows in every sheet - 1 (without header row) !!!
    QuasiExcelSheet qes;
    int curSheet;
    String curTableName;
    String[] curTableColumnNames;
    Context context=this;
    FloatingActionButton fab;
    boolean flHelpIsShowing=false;
    final int curSheetItemId[]={0};
    View mainLayout=null;
    Runnable toDoAfter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*
        // This can be used to suppress the soft-keyboard until the
        // user actually touches the editText View
        getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
*/
        curSheet=0;
        curSheetItemId[0]=R.id.action_sheet1;
        String focusAddr=null;
        if (savedInstanceState != null) {  // Первый запуск
            curSheet = savedInstanceState.getInt("curSheet");
            curSheetItemId[0]=savedInstanceState.getInt("curSheetItemId");
            focusAddr=savedInstanceState.getString("focusAddr",null);
        }
        DB.dbInit(this);

        mainLayout = View.inflate(this, R.layout.activity_main, null);
        setContentView(mainLayout);
        try {
            showSheet(focusAddr);
        } catch (Exception e) {};
        fab = findViewById(R.id.fab);

        final PopupMenu popup=new PopupMenu(context, findViewById(R.id.anchorForPopup));
        popup.getMenuInflater()
                .inflate(R.menu.menu_main, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                final int i=item.getItemId();
                if (i==R.id.action_sheet1 ||
                    i==R.id.action_sheet3 ||
                    i==R.id.action_sheet4 ||
                    i==R.id.action_sheet2) {
                    if (i!=curSheetItemId[0]) {
                        toDoAfter=new Runnable() {
                            @Override
                            public void run() {
                                curSheetItemId[0]=i;
                                saveData();
                                curSheet= i==R.id.action_sheet1 ? 0 :
                                          i==R.id.action_sheet2 ? 1 :
                                          i==R.id.action_sheet3 ? 2 :
                                          i==R.id.action_sheet4 ? 3 :
                                                0;
                                showSheet(null);
                            }
                        };
                        myCheckCellValue(qes.curFocus, qes.cellOldValue);
                        if (toDoAfter!=null) {  // May be dropped in myCh...
                            toDoAfter.run();
                            toDoAfter=null;
                        }
                    }
                }
                if (i==R.id.action_current_sheet_description) {
                    showHelp(curSheet==0 ? R.string.sheet1_description :
                             curSheet==1 ? R.string.sheet2_description :
                             curSheet==2 ? R.string.sheet3_description :
                             curSheet==3 ? R.string.sheet4_description :
                             -1);
                }
                if (i==R.id.action_about_the_program) {
                    showHelp(R.string.about_the_program);
                }
                if (i==R.id.action_how_to) {
                    showHelp(R.string.how_to);
                }
                return true;
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.getMenu().findItem(curSheetItemId[0]).setChecked(true);
                popup.show();
            }
        });
    }
    void showSheet(String focusAddr) {
        int anchorRow=0;  // 1-based
        int anchorY=0;
        if (qes != null) {  // Not the first call
            if (qes.relativeLayout != null) {
                if (qes.curFocus!=null) { // Geting Y of currently edit cell
                    String s=qes.getCellAddr(qes.curFocus);
                    anchorRow=Integer.parseInt(s.substring(1,s.indexOf("C")));
                    int xy[]={0,0};
                    qes.curFocus.getLocationOnScreen(xy);
                    anchorY=xy[1];
                }
                // Hiding keyboard if any
                ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(qes.relativeLayout.getWindowToken(), 0);

            }
            qes.suicide();
        }

        curTableName = DB.getTableName(curSheet);
        curTableColumnNames = DB.getTableColumnNames(curSheet);
        String s=curTableName.toLowerCase()+"_";
        qes=new QuasiExcelSheet(
                this,
                s+"upper_left_cell_layout",
                s+"headers_layout",
                s+"first_column_first_data_cell_layout",
                s+"body_row_layout",
                nRows,
                getResources().getColor(R.color.colorBorder),
                getResources().getColor(R.color.colorRow2),
                getResources().getColor(R.color.colorEdit),
                curTableName.equals("Sheet4")
        ) {
            @Override
            public View checkCellValue(View cell, String cellOldValue) {
                return myCheckCellValue(cell,cellOldValue);
            };
        };
        if (qes.relativeLayout!=null) { // Insert view with sheet in the main view
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup) ((ViewGroup) mainLayout).getChildAt(0)).
                    addView(qes.relativeLayout,1,lp);
            if (anchorRow>0) {
                final int anchorRow2=anchorRow;  // 1-based
                final int anchorY2=anchorY;
                mainLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        qes.scrollSheet("R" + anchorRow2 + "C1", -1, anchorY2);
                    }
                });
            }
            if (focusAddr!=null) {
                final String focusAddr2=focusAddr;
                mainLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        qes.findCellByAddr(focusAddr2).requestFocus();
                    }
                });
            }
            qes.relativeLayout.bringToFront();
            flHelpIsShowing=false;
        } else {
            ((TextView) mainLayout.findViewById(R.id.helpView)).
                    setText(qes.erMes);
        }
        loadData();
    }
    View myCheckCellValue(View cell, String cellOldValue) { // May be overriden !
        String cellNewValue = qes.getCellValue(cell);
        if (cell==null ||
                cellNewValue == null ||  // Not able to check
                cellNewValue.equals(cellOldValue)) { // Did not changed
            return null;  // Returning OK
        }
        Log.d(TAG, "CheckCellValue " + cell.getTag() + " " + cellOldValue + "->" + cellNewValue);

        if (qes.getCellCol(cell)==0) { // First (key!) column
            if (cellNewValue.equals("")) {  // Clearing means deleting item
                deleteRow(cell, cellOldValue);
            } else {  // Checking if will be uniqu
                View v = similarRowExists(cell);
                if (v != null) {
                    qes.setCellValue(cell, cellOldValue);
                    messageToUser("Such an item allready exists");
                    return v;
                } else {
                    replicateRow(cell,cellOldValue); // Create similar item in all sheets
                }
            }
        }
        return null; // OK
    };
    Cursor cursor;
    void loadData() {
        Log.d(TAG, "loadData into sheet #"+curSheet);

        int nColumns = curTableColumnNames.length-1;  // Skipping first db column - _id
        if ( nColumns!=qes.getColCount()) {
            messageToUser("Could not load data in the sheet: " +
                    "number of data columns in the table (" + nColumns +
                    ") not equal to number of columns in the sheet (" + qes.getColCount() + ")");
            return;
        }
        if (cursor != null) cursor.close();
        cursor = DB.db.query(curTableName, null,
                null, null, null, null, null);
        if (cursor.getCount() > nRows) {
            messageToUser("Could not load data in the sheet: " +
                    "number of records in the table (" + cursor.getCount() +
                    ") greater than number of rows in the sheet (" + nRows + ")");
            return;
        }
        for (int i = 0; i < nRows; i++) {
            if (i == 0 && !cursor.moveToFirst() ||
                    i > 0 && !cursor.moveToNext()) {
                return;
            }
            for (int j = 0; j < nColumns; j++) {
                qes.setCellValue(qes.getCell(i+1, j), cursor.getString(j+1));  // Skipping header row and _id column
            }
        }
    }
    void saveData() {
        Log.d(TAG, "saveData");

        DB.db.delete(curTableName,null,null); // Clearing

        int nColumns = curTableColumnNames.length-1;  // Skipping first db column - _id
        for (int i = 0; i < nRows; i++) {
            boolean flDontSave=false;
            ContentValues values=new ContentValues();
            for (int j=0; j<nColumns; j++) {
                String s=qes.getCellValue(qes.getCell(i + 1, j));
                values.put(curTableColumnNames[j + 1],s); // Skipping header row
                if (j == 0 && s.equals("")) { // Dont save rows with empty first column
                    flDontSave = true;
                    break;
                }
            }
            if (!flDontSave) DB.db.insert(curTableName, curTableColumnNames[0], values);
        }
    }
    View similarRowExists(View firstCell) {
        String cellValue=qes.getCellValue(firstCell);
        int iRow=qes.getCellRow(firstCell);
        for (int i=1; i<nRows; i++) {  // Skipping header
            if (qes.getCellValue(qes.getCell(i,0)).
                    equalsIgnoreCase(cellValue) && i!=iRow) {
                return qes.getCell(i,0);
            }
        }
        return null;
    }
    void replicateRow(View firstCell, String firstCellOldValue) {
        long k2=0;
        for (int iTable=0; iTable<99; iTable++) {
            String tableName = DB.getTableName(iTable);
            if (tableName == null) break;

            ContentValues values=new ContentValues();
            values.put(DB.getTableColumnNames(iTable)[1], qes.getCellValue(firstCell));
            if (iTable==curSheet) { // Saving entire row, на случай если нормального сохранения в конце
                                    // не будет, например, пользователь убьет приложение из recent applications
                int iRow=qes.getCellRow(firstCell);
                for (int j=1; j<qes.getColCount(); j++) {
                    values.put(DB.getTableColumnNames(iTable)[j + 1],
                            qes.getCellValue(qes.getCell(iRow, j)));
                }
            }
            long k=0;
            if (!firstCellOldValue.equals("")) { // replacing records with old first column
                k=DB.db.update(tableName, values,
                        DB.getTableColumnNames(iTable)[1] + "==\"" + firstCellOldValue + "\"",
                        null);
            }
            if (k==0) { // There was not such a record
                k=DB.db.update(tableName, values,  // На всякий случай, вдруг уже есть, хотя и не должно
                        DB.getTableColumnNames(iTable)[1] + "==\"" + qes.getCellValue(firstCell) + "\"",
                        null);
                if (k==0) {
                    k = DB.db.insert(tableName, null, values);
                }
            }
            k2=k2+k;
        }
        Log.d(TAG,"replicateRow: "+k2+" records updated or added");
    }
    void deleteRow(final View cell, final String cellOldValue) {
        final Runnable toDoAfter2=toDoAfter;
        toDoAfter=null; // !!!
        new AlertDialog.Builder(this)
                .setTitle(qes.getCellValue(cell))
                .setMessage("Are you sure you want to delete this row \""+cellOldValue+
                        "\" and such similar rowws in all other sheets?")
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        qes.setCellValue(cell,cellOldValue);
                        cell.requestFocus();
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRow2(cellOldValue);
                        if (toDoAfter2!=null) {
                            toDoAfter2.run();
                        }
                    }
                }).create().show();
    }
    void deleteRow2(String cellOldValue) {
        for (int i=0; i<99; i++) {
            String s = DB.getTableName(i);
            if (s == null) break;
            DB.db.delete(s, DB.getTableColumnNames(i)[1] + "==\"" + cellOldValue+"\"", null);
        }
        showSheet(null);
    }
    void showHelp(int resourceId) {
        flHelpIsShowing=true;
        TextView tv=findViewById(R.id.helpView);
        tv.setText(Html.fromHtml(getResources().getString(resourceId)));
        ((View) tv.getParent()).bringToFront();
    }
    private void messageToUser(String text) {
        Log.e(TAG,text);
        Snackbar.make(fab, text, Snackbar.LENGTH_LONG).show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveData();
    }

    @Override
    public void onBackPressed() {
        if (flHelpIsShowing) {
            flHelpIsShowing=false;
            if (qes.relativeLayout!=null) {
                qes.relativeLayout.bringToFront();
            }
        } else {
            toDoAfter=new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            };
            myCheckCellValue(qes.curFocus, qes.cellOldValue);
            if (toDoAfter!=null) {  // May be dropped in myCh...
                toDoAfter.run();
                toDoAfter=null;
            }
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("curSheet", curSheet);
        savedInstanceState.putInt("curSheetItemId", curSheetItemId[0]);
        if (qes != null && qes.curFocus != null) {
            savedInstanceState.putString("focusAddr", qes.getCellAddr(qes.curFocus));
        }
    }
}
/* ToDo
1. Победить размножение listener'ов, проверить необходимость suicide
2. Увеличить шрифт меню
*/
