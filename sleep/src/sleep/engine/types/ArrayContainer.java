package sleep.engine.types;

import sleep.runtime.*;
import java.util.*;

public class ArrayContainer implements ScalarArray
{
   protected Stack values;

   public ArrayContainer()
   {
      values = new Stack();
   }

   /** initial values must be a collection of Scalar's */
   public ArrayContainer(Collection initialValues)
   {
      values = new Stack();
      values.addAll(initialValues);
   }

   public Scalar pop()
   {
      return (Scalar)values.pop();
   }

   public Scalar push(Scalar value)
   {
      values.push(value);
      return value;
   }

   public int size()
   {
      return values.size();
   }

   public void sort(Comparator compare)
   {
      Collections.sort(values, compare);
   }

   public Scalar getAt(int index)
   {
      if (index >= size())
      {
          Scalar temp = SleepUtils.getEmptyScalar();
          values.add(temp);
          return temp;   
      }

      return (Scalar)values.get(index);
   }

   public void remove(Scalar key)
   {
      SleepUtils.removeScalar(values.iterator(), key);
   }

   public Scalar remove(int index)
   {
      return (Scalar)values.remove(index);
   }

   public Iterator scalarIterator()
   {
      return values.iterator();
   }

   public Scalar add(Scalar value, int index)
   {
      values.add(index, value);
      return value;
   }

   public String toString()
   {
      return values.toString();
   }
}
