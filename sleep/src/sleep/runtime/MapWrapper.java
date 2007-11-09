package sleep.runtime;

import java.util.*;
import sleep.engine.ObjectUtilities;

/** A class for creating accessing a Map data structure in your application in a ready only way.  It is assumed that your map 
data structure uses strings for keys.  Accessed values will be marshalled into Sleep scalars */
public class MapWrapper implements ScalarHash
{
   protected Map values;

   public MapWrapper(Map _values)
   {
      values = _values;
   }

   public Scalar getAt(Scalar key)
   {
      Object o = values.get(key.getValue().toString());
      return ObjectUtilities.BuildScalar(true, o);
   }

   /** this operation is kind of expensive... should be fixed up to take care of that */
   public ScalarArray keys()
   {
      return new CollectionWrapper(values.keySet());
   }

   public void remove(Scalar key)
   {
      throw new RuntimeException("hash is read-only");
   }

   public String toString()
   {
      return "(read-only hash " + values.toString() + ")";
   }
}
