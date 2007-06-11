package sleep.engine.types;

import sleep.runtime.*;

import java.util.*;

public class HashContainer implements ScalarHash
{
   protected HashMap values;

   public HashContainer()
   {
      values = new HashMap();
   }

   public Scalar getAt(Scalar key)
   {
      Scalar value = (Scalar)values.get(key.getValue().toString());

      if (value == null)
      {
         value = SleepUtils.getEmptyScalar();
         values.put(key.getValue().toString(), value);
      }

      return value;
   }

   public ScalarArray keys()
   {
      ScalarType ntype = SleepUtils.getEmptyScalar().getValue();

      Iterator i = values.values().iterator();
      while (i.hasNext())
      {
         Scalar next = (Scalar)i.next();

         if (next.getArray() == null && next.getHash() == null && next.getActualValue() == ntype)
         {
            i.remove();
         }
      }

      return new CollectionWrapper(values.keySet());
   }

   public void remove(Scalar value)
   {
      SleepUtils.removeScalar(values.values().iterator(), value);
   }

   public String toString()
   {
      return values.toString();
   }
}
