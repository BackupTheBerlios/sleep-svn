package sleep.runtime;

import java.util.*;

/**
 * <p>This interface lets you implement your own data structure behind a scalar
 * array.</p>
 * 
 * <p>To instantiate a custom scalar array:</p>
 * 
 * <code>Scalar temp = SleepUtils.getArrayScalar(new MyScalarArray());</code>
 * 
 * <p>When implementing the following interface, keep in mind you are implementing an
 * interface to an array data structure.</p>
 */
public interface ScalarArray extends java.io.Serializable
{
   public Scalar   pop();
   public Scalar   push(Scalar value);
   public int      size();
   public Scalar   getAt(int index);
   public Iterator scalarIterator();
   public Scalar   add(Scalar value, int index); 
   public void     remove(Scalar value);
   public Scalar   remove(int index);
   public void     sort(Comparator compare);
}
