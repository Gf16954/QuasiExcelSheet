/**
 * Created by gf on 10.11.2017.
 * Idea: https://www.androidcode.ninja/android-scroll-table-fixed-header-column/
*/
package com.gf169.gfexceltable;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class QuasiExcelSheet {
    public final String TAG = "gfQuasiExcelSheet";

    public RelativeLayout relativeLayout;
    public String erMes;
    public View curFocus;
    public String cellOldValue;

    private static final int ROW_AUTOFIT = -9;

    private String dbTableName;
    private String[] dbTableColumnNames;

    private Context context;
    private RelativeLayout rl;
    private int nRows;

    // A B
    // C D
    private MyHorizontalScrollView hsvB;
    private MyScrollView svC;
    private MyHorizontalScrollView hsvD;
    private MyScrollView svD;

    private TableLayout tlA; // Upper left cell (A1)
    private TableLayout tlB; // Headers row (B1:B?)
    private TableLayout tlC; // First column (A2:A1000)
    private TableLayout tlD; // Data (B2:X1000)

    static int viewId=0;
    private int viewIdA;
    private int viewIdB;
    private int viewIdC;
    private int viewIdDH;
    private int viewIdDV;

    private TableRow.LayoutParams cellOldLayoutParams;
    private int cellOldBackground;
    private int colorEditCell;
    private boolean autoFit;
//    private RowLoader rowLoader;
//    private RowSaver rowSaver;

    static QuasiExcelSheet lastCreatedThis;
    ViewTreeObserver viewTreeObserver;
    ViewTreeObserver.OnGlobalFocusChangeListener onGlobalFocusChangeListener;

    public QuasiExcelSheet(Context context
                           // Next 4 parameres - names of resource files without ".xml"
            , String upperLeftCellLayout // May be any kind of View
            , String headersLayout  // Must be TableRow layout
            , String firstColumnFirstDataCellLayout // May be any kind of View
            , String bodyRowLayout  //  Must be TableRow layout
            , int nRows // Without header !!!
            , int colorBackground  // of rl = borders color
            , int colorOddRow
            , final int colorEditCell // background
            , final boolean autoFit // If Excel autofit (autoadjusting row height) feature is needed
    ) {
        TableRow tr;
        int resId;
        RelativeLayout.LayoutParams lp;
        ViewTreeObserver vto;

        // Start
        lastCreatedThis=this;

        this.context = context;
        this.nRows=nRows;
        this.colorEditCell=colorEditCell;
        this.autoFit=autoFit;
//        this.rowLoader=rowLoader;
//        this.rowSaver=rowSaver;

        rl=new RelativeLayout(context);
        viewTreeObserver=rl.getViewTreeObserver();

        viewTreeObserver.addOnGlobalFocusChangeListener(
                new ViewTreeObserver.OnGlobalFocusChangeListener() {
                    @Override
                    public void onGlobalFocusChanged(View oldFocus,
                                                     View newFocus) {
                        onGlobalFocusChangeListener = this;
                        myOnGlobalFocusChanged(oldFocus, newFocus);
                    }
                });
/* Не работает this
        viewTreeObserver.addOnGlobalFocusChangeListener(
                (newFocus, oldFocus)-> {
                        onGlobalFocusChangeListener = this;
                        myOnGlobalFocusChanged(oldFocus, newFocus);
                    });
*/
        rl.setBackgroundColor(colorBackground);

        // This is to intercept focus from EditText in order the keyboard not to appear
        LinearLayout ll=new LinearLayout(context);
        ll.setBackgroundColor(Color.TRANSPARENT);
        ll.setFocusable(true);
        ll.setFocusableInTouchMode(true);
        lp=new RelativeLayout.LayoutParams(0,0);
        rl.addView(ll,lp);

        // Upper left cell
        // View -> TableRow -> TableLayout -> root RelativeLayout
        tlA = new TableLayout(context);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
        rl.addView(tlA,lp);
        viewIdA=/*View.*/generateViewId();
        rl.getChildAt(rl.getChildCount()-1).setId(viewIdA);

        tr = new TableRow(context);
        resId=context.getResources().getIdentifier(
                upperLeftCellLayout,"layout",context.getPackageName());
        if (resId==0) {
            reportError("Resource file "+upperLeftCellLayout+".xml not found");
            return;
        }
        View.inflate(context, resId, tr);

        addDummyCell(tr);
        for (int j=0; j<tr.getChildCount(); j++) {
            tr.getChildAt(j).setTag("A0:" + j );
        }

        tlA.addView(tr);

        // Headers - rest of the first row
        // TableRow -> TableLayout -> HorisontalScrollView -> root RelativeLayout
        hsvB = new MyHorizontalScrollView(context);
        viewIdB=/*View.*/generateViewId();
        hsvB.setId(viewIdB);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.RIGHT_OF, viewIdA);
        rl.addView(hsvB,lp);

        tlB = new TableLayout(context);
        hsvB.addView(tlB);

        resId=context.getResources().getIdentifier(
                headersLayout,"layout",context.getPackageName());
        if (resId==0) {
            reportError("Resource file "+headersLayout+".xml not found");
            return;
        }
        View.inflate(context,resId,tlB);

        tr=((TableRow) tlB.getChildAt(0));
        tr.setBaselineAligned(false); // Nessesary for proper vertical alignment of cells inside the row !!!

        addDummyCell(tr);

        TableRow.LayoutParams lpULC= // From upper left cell
                (TableRow.LayoutParams) ((TableRow) tlA.getChildAt(0)).getChildAt(1).getLayoutParams();
        for (int j=0; j<tr.getChildCount(); j++) {
            setCellLP(tr.getChildAt(j),tr.getChildAt(j), // Copying all vertical parameters
                    null, lpULC.height,
                    null, lpULC.topMargin,null, lpULC.bottomMargin);
            tr.getChildAt(j).setTag("B0:" + j );
        }

        // First column
        // View -> TableRow -> TableLayout -> ScrollView -> root RelativeLayout ->
        svC = new MyScrollView(context);
        viewIdC=/*View.*/generateViewId();
        svC.setId(viewIdC);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW, viewIdA);
        rl.addView(svC,lp);

        tlC = new TableLayout(context);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        svC.addView(tlC,lp);

        resId=context.getResources().getIdentifier(
                firstColumnFirstDataCellLayout,"layout",context.getPackageName());;
        if (resId==0) {
            reportError("Resource file "+firstColumnFirstDataCellLayout+".xml not found");
            return;
        }
        for (int i=0; i<nRows; i++) {
            tr = new TableRow(context);
            View.inflate(context, resId, tr);

            addDummyCell(tr);

            setCellLP(tr.getChildAt(1),tr.getChildAt(1), // Copying all horizontal parameters
                    lpULC.width, null, lpULC.leftMargin,  null, lpULC.rightMargin, null);

            for (int j=0; j<tr.getChildCount(); j++) {
                tr.getChildAt(j).setTag("C"+i+":"+j);
            }

            tlC.addView(tr);
        }

        // Body - cells to the right and below A1
        // TableRow -> TableLayout -> HorisontalScrollView -> ScrollView -> root RelativeLayout
        svD = new MyScrollView(context);
        viewIdDV=/*View.*/generateViewId();
        svD.setId(viewIdDV);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW, viewIdA);
        lp.addRule(RelativeLayout.RIGHT_OF, viewIdA);
        rl.addView(svD,lp);

        hsvD = new MyHorizontalScrollView(context);
        viewIdDH=/*View.*/generateViewId();
        hsvD.setId(viewIdDH);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        svD.addView(hsvD,lp);

        tlD = new TableLayout(context);
        lp=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        hsvD.addView(tlD,lp);

        resId=context.getResources().getIdentifier(
                bodyRowLayout,"layout",context.getPackageName()); // Must be TableRow layout
        if (resId==0) {
            reportError("Resource file "+bodyRowLayout+".xml not found");
            return;
        }
        for (int i=0; i<nRows; i++) {
            View.inflate(context, resId, tlD);
            tr=((TableRow) tlD.getChildAt(i));
            tr.setBaselineAligned(false); // Nessesary for proper vertical alignment of cells inside the row !!!

            addDummyCell(tr);

            for (int j=0; j<tr.getChildCount(); j++) {

                TableRow.LayoutParams lpUC=(TableRow.LayoutParams)  // From cell in the header  in the same column
                        ((TableRow) tlB.getChildAt(0)).getChildAt(j).getLayoutParams();
                TableRow.LayoutParams lpLC=(TableRow.LayoutParams)  // From cell in the first column in the same row
                        ((TableRow) tlC.getChildAt(i)).getChildAt(1).getLayoutParams();
                setCellLP(tr.getChildAt(j),tr.getChildAt(j), // Copying all parameters
                        lpUC.width, lpLC.height,
                        lpUC.leftMargin, lpLC.topMargin, lpUC.rightMargin, lpLC.bottomMargin);

                tr.getChildAt(j).setTag("D"+i+ ":" + j);

                if (colorOddRow != 0 && (i % 2 == 1)) {
                    tr.getChildAt(j).setBackgroundColor(colorOddRow);
                }
            }
            if (autoFit) {
                setHeightOfAllCellsInRow(tr.getChildAt(0),ROW_AUTOFIT,null);
            }
        }
        relativeLayout=rl;
    }
    class MyScrollView extends ScrollView{
        public MyScrollView(Context context) {
            super(context);
        }
        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            if(getId()==viewIdC){
                svD.scrollTo(0, t);
            }else{
                svC.scrollTo(0, t);
            }
        }
    }
    class MyHorizontalScrollView extends HorizontalScrollView{
        public MyHorizontalScrollView(Context context) {
            super(context);
        }
        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            if(getId()==viewIdB){
                hsvD.scrollTo(l,0);
            }else{
                hsvB.scrollTo(l,0);
            }
        }
        // TWO folowing overridings prevent automatic resetting of focus when it moves off the screen
        // https://stackoverflow.com/questions/5375838/scrollview-disable-focus-move
        @Override
        protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
            return true;
        }
        @Override
        public ArrayList<View> getFocusables(int direction) {
            return new ArrayList<View>();
        }
    }
    private void addDummyCell(TableRow tr) { // dummy view, not to let cell shrink vertically
        tr.addView(new TextView(context), 0,  // Will be the very first cell
                cloneCellLP(tr.getChildAt(0),
                        0,null,0,null,0,null));
    }
    private TableRow.LayoutParams cloneCellLP(View sourceCell,
                                              Integer width,
                                              Integer height,
                                              Integer leftMargin,
                                              Integer topMargin,
                                              Integer rightMargin,
                                              Integer bottomMargin) {
        TableRow.LayoutParams lp =
                (TableRow.LayoutParams) sourceCell.getLayoutParams();
        TableRow.LayoutParams lp2 = new TableRow.LayoutParams(
                (width==null ? lp.width : width), (height==null ? lp.height : height));
        lp2.setMargins(
                (leftMargin==null ? lp.leftMargin : leftMargin),
                (topMargin==null ? lp.topMargin : topMargin),
                (rightMargin==null ? lp.rightMargin : rightMargin),
                (bottomMargin==null ? lp.bottomMargin : bottomMargin));
        return lp2;
    }
    private void setCellLP(View cell, View sourceCell,
                           Integer width,
                           Integer height,
                           Integer leftMargin,
                           Integer topMargin,
                           Integer rightMargin,
                           Integer bottomMargin) {
        cell.setLayoutParams(
                cloneCellLP(sourceCell,
                    width,
                    height,
                    leftMargin,
                    topMargin,
                    rightMargin,
                    bottomMargin));
    }
    private View getFirstCellInTheSameRowOfAdjacentTable(View cell) {
        String s=cell.getTag().toString();
        s = s.replace("A", "b")
                .replace("B", "a")
                .replace("C", "d")
                .replace("D", "c")
                .toUpperCase()
                .substring(0, s.indexOf(':') + 1) + "0";
        return rl.findViewWithTag(s);
    }
    private void setHeightOfAllCellsInRow(View cell, int height, final Object cellExceptTag) {
        Log.d(TAG, "setHeightOfAllCellsInRow");

        if (height!=ROW_AUTOFIT) {  // May be real height or constants MATCH_PARENT (-1) and WRAP_CONTENT (-2)
            for (int i = 0; i < 2; i++) {
                if (i == 1) { // Getting first cell in the same row of the adjacent table
                    cell = getFirstCellInTheSameRowOfAdjacentTable(cell);
                }
                TableRow tr = (TableRow) cell.getParent();
                for (int j = 0; j < tr.getChildCount(); j++) {
                    View v = tr.getChildAt(j);
                    if (v.getTag() != cellExceptTag) {
                        setCellLP(v,v,null,height,null,null,null,null);
                    }
                }
            }
        } else { // Excel Autofit
            setHeightOfAllCellsInRow(cell, TableRow.LayoutParams.WRAP_CONTENT, cellExceptTag);
            final View cell2 = cell;
            cell.post(new Runnable() {
                @Override
                public void run() {   // post !!!
                    int h = Math.max(
                            ((View) cell2.getParent()).getHeight(),
                            ((View) getFirstCellInTheSameRowOfAdjacentTable(cell2).
                                    getParent()).getHeight());
                    setHeightOfAllCellsInRow(cell2, h, cellExceptTag);
                }
            });
        };
    }
    public void myOnGlobalFocusChanged(View oldFocus, View newFocus) {
        String s=rl.toString();
        s="relativeLayout id "+s;
        s += "\noldFocus ";
        if (oldFocus != null ) {
            s += oldFocus.toString();
            if (oldFocus.getTag() != null) {
                s += "\noldFocus.tag "+oldFocus.getTag().toString();
            }
        }
        s += "\nnewFocus ";
        if (newFocus != null ) {
            s += newFocus.toString();
            if (newFocus.getTag() != null) {
                s += "\nnewFocusTag "+newFocus.getTag().toString();
            }
        }
        Log.d(TAG, s);

//        if (viewTreeObserver==null || !viewTreeObserver.isAlive()) {
        if (this!=lastCreatedThis) { // Fighting with listener duplication
            Log.d(TAG, "Excesive listener - exitting");
            return;
        }
        if (newFocus.getTag()==null) return;

        curFocus=newFocus;

        if (oldFocus != null && oldFocus instanceof EditText) {
            if (!((TextView) oldFocus).getText().toString().equals(cellOldValue)) {
                final View newFocus2 = checkCellValue(oldFocus, cellOldValue);
                if (newFocus2 != null) {
                    // Jump oldFocus -> newFocus -> newFocus2
                    cellOldValue = ((EditText) newFocus).getText().toString();
                    oldFocus.setBackgroundColor(cellOldBackground);
                    cellOldBackground = ((ColorDrawable) newFocus.getBackground()).getColor();
                    oldFocus.setLayoutParams(cellOldLayoutParams);
                    cellOldLayoutParams = (TableRow.LayoutParams) newFocus.getLayoutParams();
                    newFocus2.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    newFocus2.requestFocus();
                                }
                            }
                    );
                    return;
                }
            }

            if (!autoFit) { // restore height and margins of the cell OldFocus
                oldFocus.setLayoutParams(cellOldLayoutParams);
            } else {  // Setting row height == height of every cell
                int h = oldFocus.getHeight(); // OldFocus is still WRAP_CONTENT !
                if (h + cellOldLayoutParams.topMargin + cellOldLayoutParams.bottomMargin >
                        ((View) oldFocus.getParent()).getHeight()) {
                    // Height of row must increase
                    setHeightOfAllCellsInRow(oldFocus, h, null);
                } else {
                    // May decrease - autofit
                    setHeightOfAllCellsInRow(oldFocus, ROW_AUTOFIT, newFocus);
                }
            }
            oldFocus.setBackgroundColor(cellOldBackground);
            // The ONLY MAGIC way to scroll to the text beginning!!!
            // Neither the scrollTo, nor the setScroll does not work, nothing :)
            ((TextView) oldFocus).setText(((TextView) oldFocus).getText());
        }
        if (newFocus != null && newFocus instanceof EditText) { //  && newFocus.getTag()!=null
            cellOldValue = ((EditText) newFocus).getText().toString();

            cellOldBackground = ((ColorDrawable) newFocus.getBackground()).getColor();
            newFocus.setBackgroundColor(colorEditCell);

            cellOldLayoutParams = (TableRow.LayoutParams) newFocus.getLayoutParams();
            // In order cell could strech and show all input
            setCellLP(newFocus,newFocus,null,TableRow.LayoutParams.WRAP_CONTENT,
                    null,null,null,null);
        }
    }
    public void scrollSheet(String cellAddr, int x, int y) {
        // Scroll so that the cell will be positioned to x,y (in pixels)
        View cell=findCellByAddr(cellAddr);
        int xy[]={0,0};
        cell.getLocationOnScreen(xy);
        hsvB.scrollBy(xy[0]-x, 0);
        svC.scrollBy(0, xy[1]-y);
    }
    public View findCellByAddr(String addr) { // R123C456
        int i=Integer.parseInt(addr.substring(1,addr.indexOf("C")));
        int j=Integer.parseInt(addr.substring(addr.indexOf("C")+1));
        TableLayout tl= i==1 && j==1 ? tlA :
                        i==1 && j>1 ? tlB :
                        i>1 && j==1 ? tlC :
                        i>1 && j>1 ? tlD :
                        null;
        return ((TableRow) tl.getChildAt(i==1 ? i-1 : i-2)).getChildAt(j==1 ? j : j-1);  // Skip dummy cell at the beginning
    }
    public int getCellRow(View cell) {  // 0-based!
        String s=cell.getTag().toString(); // D123:456 zero-based
        String s1=s.substring(0,1);// D
        String s2=s.substring(1);  // 123:456
        s2=s2.substring(0,s2.indexOf(":")); // 123
        return Integer.parseInt(s2)+(s1.equals("A") || s1.equals("B") ? 1 : 2)-1;
    }
    public int getCellCol(View cell) {  // 0-based!
        String s=cell.getTag().toString(); // D123:456 zero-based
        String s1=s.substring(0,1);// D
        String s2=s.substring(1);  // 123:456
        String s3=s2.substring(s2.indexOf(":")+1); // 456
        return Integer.parseInt(s3)+(s1.equals("A") || s1.equals("C") ? 0 : 1)-1;
    }
    public String getCellAddr(View cell) {  // R1C1  1-based!
        return "R"+(getCellRow(cell)+1)+"C"+(getCellCol(cell)+1);
    }
    public int getColCount() {
        return ((TableRow) tlB.getChildAt(0)).getChildCount(); // -1+1
    }
    public View getCell(int iRow, int iCol) {  // zero based
        View v=null;
        TableLayout tl=null;
        if (iRow==0 && iCol==0) tl=tlA;
        if (iRow==0 && iCol>0) tl=tlB;
        if (iRow>0 && iCol==0) tl=tlC;
        if (iRow>0 && iCol>0) tl=tlD;
        if (tl!=null) {
            TableRow tr=(TableRow) tl.getChildAt(iRow==0 ? 0 : iRow-1);
            if (tr!=null) {
                v=tr.getChildAt(iCol==0 ? 1 : iCol);
            }
        }
        return v;
    }
    public boolean setCellValue(View cell, Object value) {  // 0-based
        boolean r=false;
        if (cell!=null && value !=null) {
            if (value instanceof View) {
                ((TableRow) cell.getParent()).removeView(cell);
                ((TableRow) cell.getParent()).addView(
                        (View) value, (getCellCol(cell) == 0 ? 1 : getCellCol(cell)));
                r = true;
            } else if (cell instanceof TextView) {
                if (cell instanceof CheckBox) {
                    ((CheckBox) cell).setChecked(value.toString().equals("1"));
                } else {
                    ((TextView) cell).setText(value.toString());
                }
                r=true;
            }  // To add other types of view
        }
        return r;
    }
    public String getCellValue(View cell) {
        String s = null;
        if (cell instanceof TextView) {
            if (cell instanceof CheckBox) {
                s = ((CheckBox) cell).isChecked() ? "1" : "0";
            } else {
                s = ((TextView) cell).getText().toString();
            }
        } else {
            // To add other types of view
        }
        return s;
    }
    public View checkCellValue(View cell, String cellOldValue) { // May be overriden !
        return null;
    };
    public void suicide() {
        if (rl!=null) {
            if (!viewTreeObserver.isAlive()) {
                viewTreeObserver = rl.getViewTreeObserver();
            }
            viewTreeObserver.removeOnGlobalFocusChangeListener(onGlobalFocusChangeListener);

            ViewGroup p = (ViewGroup) rl.getParent();
            if (p != null) p.removeView(rl);
        }
    }
    private void reportError(String erMes) {
        this.erMes=erMes;
        Log.e(TAG,erMes);
        Toast.makeText(context, erMes, Toast.LENGTH_LONG).show();  // Does not show, dies immediately :(
    };
    private int generateViewId() { // instead of View.generateViewId() which requires API 17
        return ++viewId;
    };
}
