package sleep.bridges;

import sleep.runtime.Scalar;

public class KeyValuePair
{
   protected Scalar key;
   protected Scalar value;

   public KeyValuePair(Scalar _key, Scalar _value)
   {
      key   = _key;
      value = _value;
   }

   public Scalar getKey() { return key; }
   public Scalar getValue() { return value; }

   public String toString()
   {
      return key.toString() + "=" + value.toString();
   }
}

