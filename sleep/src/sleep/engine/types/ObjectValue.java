package sleep.engine.types;

import sleep.runtime.ScalarType;

public class ObjectValue implements ScalarType
{
   protected Object value;

   public ObjectValue(Object _value)
   {
      value = _value;
   }

   public ScalarType copyValue()
   {
      return this;
   }

   public int intValue()
   {
      return value.hashCode();
   }

   public long longValue()
   {
      return (long)intValue();
   }

   public double doubleValue()
   {
      return (double)intValue();
   }

   public String toString()
   {
      return value.toString();
   }

   public Object objectValue()
   {
      return value;
   }
}
