package sleep.bridges.swing.models;

import sleep.runtime.*;
import java.util.*;

import javax.swing.*;

public class ScriptedListModel extends AbstractListModel implements ScalarArray, ComboBoxModel
{
   /***** Everything below this line is ListModel stuff ******/

   public int getSize()
   {
      return size();
   }

   public Object getElementAt(int index)
   {
      return getAt(index).objectValue();
   }

   public void fireChange()
   {
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {         
            fireContentsChanged(this, 0, size());
         }
      });
   }

   Object selected = null;

   public Object getSelectedItem()
   {
      return selected;
   }

   public void setSelectedItem(Object item)
   {
      selected = item;
   }

   /***** Everything below this line is Scalar Array stuff ******/

   protected Stack values;

   public ScriptedListModel()
   {
      values = new Stack();
   }

   /** initial values must be a collection of Scalar's */
   public ScriptedListModel(ScalarArray populate)
   {
      values = new Stack();

      Iterator i = populate.scalarIterator();
      while (i.hasNext())
          values.add(i.next());
   }

   public Scalar pop()
   {
      Scalar temp = (Scalar)values.pop();

      fireChange();

      return temp;
   }

   public Scalar push(Scalar value)
   {
      values.push(value);

      fireChange();

      return value;
   }

   public int size()
   {
      return values.size();
   }

   public void sort(Comparator compare)
   {
      Collections.sort(values, compare);

      fireChange();
   }

   public Scalar getAt(int index)
   {
      if (index >= size())
      {
          Scalar temp = SleepUtils.getEmptyScalar();
          values.add(temp);

          fireChange();

          return temp;   
      }

      return (Scalar)values.get(index);
   }

   public void remove(Scalar key)
   {
      Iterator i = values.iterator();
      while (i.hasNext())
      {
         String value = i.next().toString();

         if (value.equals(key.toString()))
         {
            i.remove();
         }
      }

      fireChange();
   }

   public Scalar remove(int index)
   {
      Scalar temp = (Scalar)values.remove(index);

      fireChange();

      return temp;
   }

   public Iterator scalarIterator()
   {
      return values.iterator();
   }

   public Scalar add(Scalar value, int index)
   {
      values.add(index, value);

      fireChange();

      return value;
   }

   public String toString()
   {
      return values.toString();
   }
}
