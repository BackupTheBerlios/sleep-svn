package sleep.runtime;

import java.util.*;
import sleep.engine.ObjectUtilities;

/** A read only scalar array for wrapping data structures that implement the java.util.Collection interface. 
Values will be marshalled into Sleep scalars when accessed. */
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
      throw new RuntimeException("array is read-only");
   }

   public void sort(Comparator compare)
   {
      throw new RuntimeException("array is read-only");
   }

   public Scalar push(Scalar value)
   {
      throw new RuntimeException("array is read-only");
   }

   public int size()
   {
      return values.size();
   }

   public Scalar remove(int index)
   {
      throw new RuntimeException("array is read-only");
   }

   public Scalar getAt(int index)
   {
      if (array == null)
      {
         array = values.toArray();
      }

      return ObjectUtilities.BuildScalar(true, array[index]);
   }

   public Iterator scalarIterator()
   {
      return new ProxyIterator();
   }

   public Scalar add(Scalar value, int index)
   {
      throw new RuntimeException("array is read-only");
   }

   public void remove(Scalar value)
   {
      throw new RuntimeException("array is read-only");
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
         return ObjectUtilities.BuildScalar(true, temp);
      }

      public void remove()
      {
          throw new RuntimeException("array is read-only");
          // no dice
      }
   }
}
