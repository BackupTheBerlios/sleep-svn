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
         if (((Scalar)i.next()).getValue() == ntype)
            i.remove();
      }

      return new CollectionWrapper(values.keySet());
   }

   public void remove(Scalar value)
   {
      Iterator i = values.keySet().iterator();
      while (i.hasNext())
      {
         if (i.next().toString().equals(value.toString()))
            i.remove();
      }
   }

   public String toString()
   {
      return values.toString();
   }
}
