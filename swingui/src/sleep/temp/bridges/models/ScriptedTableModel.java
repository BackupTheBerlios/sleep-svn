package sleep.bridges.swing.models;

import sleep.runtime.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import sleep.bridges.*;

public class ScriptedTableModel extends AbstractTableModel
{
   protected SleepClosure  rows;
   protected ScalarArray    columns;

   /***** Everything below this line is TableModel stuff ******/

   public int getRowCount()
   {
      return rows.callClosure("size", null, null).intValue();
   }

   public int getColumnCount() 
   {
      return columns.size();
   }

   public String getColumnName(int column)
   {
      return columns.getAt(column).toString();
   }

   public Object getValueAt(int row, int col)
   {
      Stack temp = new Stack();
      temp.push(SleepUtils.getScalar(col));
      temp.push(SleepUtils.getScalar(row));

      return rows.callClosure("get", null, temp);
   }

   public ScriptedTableModel(SleepClosure closure, ScalarArray array)
   {
      columns = array;
      rows    = closure;
      rows.getVariables().putScalar("$model", SleepUtils.getScalar(this));
   }
}
