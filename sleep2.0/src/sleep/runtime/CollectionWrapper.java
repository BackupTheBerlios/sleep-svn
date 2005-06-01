package sleep.runtime;

import java.util.*;

/** A read only scalar array for wrapping data structures that implement the java.util.Collection interface. Values wrapped 
within this class will be converted to object scalars whenever accessed. */
public class CollectionWrapper implements ScalarArray
{
   protected Collection values;
   protected Object[]   array  = null;

   public CollectionWrapper(Collection _values)
   {
      values = _values;
   }

   public String toString()
   {
      return "(read-only array: " + values.toString() + ")";
   }

   public Scalar pop()
   {
      return SleepUtils.getEmptyScalar();
   }

   public void sort(Comparator compare)
   {

   }

   public Scalar push(Scalar value)
   {
      return SleepUtils.getEmptyScalar();
   }

   public int size()
   {
      return values.size();
   }

   public Scalar remove(int index)
   {
      return SleepUtils.getEmptyScalar();
   }

   public Scalar getAt(int index)
   {
      if (array == null)
      {
         array = values.toArray();
      }

      if (array[index] instanceof String)
      {
         return SleepUtils.getScalar((String)array[index]);
      }

      return SleepUtils.getScalar(array[index]);
   }

   public Iterator scalarIterator()
   {
      return new ProxyIterator();
   }

   public Scalar add(Scalar value, int index)
   {
      return SleepUtils.getEmptyScalar();
   }

   public void remove(Scalar value)
   {
      // do nothing
   }

   protected class ProxyIterator implements Iterator
   {
      protected Iterator realIterator;

      public ProxyIterator()
      {
         realIterator = values.iterator();
      }

      public boolean hasNext()
      {
         return realIterator.hasNext(); 
      }

      public Object next()
      {
         Object temp = realIterator.next();
 
         if (temp instanceof String)
         {
            return SleepUtils.getScalar((String)temp);
         }

         return SleepUtils.getScalar(temp);
      }

      public void remove()
      {
          // no dice
      }
   }
}
