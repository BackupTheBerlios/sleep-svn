/*
   SLEEP - Simple Language for Environment Extension Purposes
 .-------------------------------.
 | sleep.bridges.BridgeUtilities |____________________________________________
 |                                                                            |
   Author: Raphael Mudge (raffi@hick.org)
           http://www.hick.org/~raffi/

   Description:
       utilities for bridge writers

   Documentation:

   Changelog:

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.bridges;

import sleep.runtime.*;
import java.io.File;
import sleep.interfaces.Function;
import java.util.*;

/**
 * A bridge is a class that bridges your applications API and sleep.  Bridges are created using interfaces from the sleep.interfaces package.  Arguments are passed to bridges generally in a java.util.Stack form.  The Stack of arguments contains sleep Scalar objects.  The BridgeUtilities makes it safer and easier for you to extract Java types from arguments.
 * 
 * <pre>
 * // some code to execute an internal add function, not a complete example
 * 
 * public class MyAddFunction implements Function
 * {
 *    public Scalar evaluate(String name, ScriptInstance script, Stack arguments) 
 *    {
 *       if (name.equals("&add"))
 *       {
 *          int a = BridgeUtilities.getInt(arguments, 0);  
 *          int b = BridgeUtilities.getInt(arguments, 0); 
 *  
 *          return SleepUtils.getScalar(a + b); 
 *       }
 * 
 *       return SleepUtils.getEmptyScalar();
 *    }
 * }
 * </pre>
 *
 */
public class BridgeUtilities
{
   /** grab an integer. if the stack is empty 0 will be returned. */
   public static int getInt(Stack arguments)
   {
      return getInt(arguments, 0);
   }

   /** grab an integer, if the stack is empty the default value will be returned */
   public static int getInt(Stack arguments, int defaultValue)
   {
      if (arguments.isEmpty())
         return defaultValue;

      return ((Scalar)arguments.pop()).intValue();
   }

   /** grab a long.  if the stack is empty 0 will be returned. */
   public static long getLong(Stack arguments)
   {
      return getLong(arguments, 0L);
   }

   /** grab a long, if the stack is empty the default value will be returned */
   public static long getLong(Stack arguments, long defaultValue)
   {
      if (arguments.isEmpty())
         return defaultValue;

      return ((Scalar)arguments.pop()).longValue();
   }

   /** grab a double.  if the stack is empty a 0 will be returned */
   public static double getDouble(Stack arguments)
   {
     return getDouble(arguments, 0.0); 
   }

   /** grab a double, if the stack is empty the default value will be returned */
   public static double getDouble(Stack arguments, double defaultValue)
   {
      if (arguments.isEmpty())
         return defaultValue;

      return ((Scalar)arguments.pop()).doubleValue();
   }

   /** extracts all named parameters from the argument stack.  this method returns a Map whose keys are strings
       and values are Scalars. */
   public static Map extractNamedParameters(Stack args)
   {
      Map rv = new HashMap();
      Iterator i = args.iterator();
      while (i.hasNext())
      {
         Scalar temp = (Scalar)i.next();
         if (temp.objectValue() != null && temp.objectValue().getClass() == KeyValuePair.class)
         {
            i.remove();
            KeyValuePair value = (KeyValuePair)temp.objectValue();
            rv.put(value.getKey().toString(), value.getValue());
         }
      }

      return rv;
   }

   /** grabs a scalar iterator, this can come from either an array or a closure called continuously until $null is returned. */
   public static Iterator getIterator(Stack arguments, ScriptInstance script)
   {
      if (arguments.isEmpty())
        return getArray(arguments).scalarIterator();

      Scalar temp = (Scalar)arguments.pop();

      if (temp.getArray() != null)
      {
         return temp.getArray().scalarIterator();
      }      

      return SleepUtils.getFunctionFromScalar(temp, script).scalarIterator();
   }

   /** grab a sleep array, if the stack is empty a scalar array with no elements will be returned. */
   public static ScalarArray getArray(Stack arguments)
   {
      Scalar s = getScalar(arguments);
      if (s.getArray() == null)
         return SleepUtils.getArrayScalar().getArray();

      return s.getArray();
   }

   /** grab a sleep hash, if the stack is empty a scalar hash with no members will be returned. */
   public static ScalarHash getHash(Stack arguments)
   {
      if (arguments.isEmpty())
         return SleepUtils.getHashScalar().getHash();

      return ((Scalar)arguments.pop()).getHash();
   }


   /** grab a sleep array, if the grabbed array is a readonly array, a copy is returned.  if the stack is empty an array with no elements will be returned. */
   public static ScalarArray getWorkableArray(Stack arguments)
   {
      if (arguments.isEmpty())
         return SleepUtils.getArrayScalar().getArray();

      Scalar temp = (Scalar)arguments.pop();

      if (temp.getArray().getClass() == roarray)
      {
         ScalarArray array = SleepUtils.getArrayScalar().getArray();
         Iterator i = temp.getArray().scalarIterator();
         while(i.hasNext())
         {
            array.push((Scalar)i.next());
         } 

         return array;
      }

      return temp.getArray();
   }

   /** grab an object, if the stack is empty then null will be returned. */
   public static Object getObject(Stack arguments)
   {
      if (arguments.isEmpty())
         return null;

      return ((Scalar)arguments.pop()).objectValue();
   }

   /** retrieves an executable Function object from the stack.  Functions can be passed as closures
       or as a reference to a built-in Sleep subroutine i.e. &my_func. */
   public static SleepClosure getFunction(Stack arguments, ScriptInstance script)
   {
      Scalar temp = getScalar(arguments);
      return SleepUtils.getFunctionFromScalar(temp, script);
   }

   /** grab a scalar, if the stack is empty the empty/null scalar will be returned. */
   public static Scalar getScalar(Stack arguments)
   {
      if (arguments.isEmpty())
         return SleepUtils.getEmptyScalar();

      return ((Scalar)arguments.pop());
   }

   /** grab a string, if the stack is empty or if the value is null the default value will be returned. */
   public static String getString(Stack arguments, String defaultValue)
   {
      if (arguments.isEmpty())
         return defaultValue;

      String temp = arguments.pop().toString();

      if (temp == null)
         return defaultValue;

      return temp;
   }

   private static final boolean doReplace = File.separatorChar != '/';

   /** returns a File object from a string argument, the path in the string argument is transformed such 
       that the character / will refer to the correct path separator for the current OS.  Returns null if
       no file is specified as an argument. */
   public static File getFile(Stack arguments)
   {
      if (arguments.isEmpty())
         return null;

      String temp = arguments.pop().toString();

      if (doReplace)
      {
         temp = temp.replace('/', File.separatorChar); 
      }

      return new File(temp);
   }
 
   private static Class kvpair;
   private static Class roarray;

   static
   { 
      try
      {
         kvpair  = Class.forName("sleep.bridges.KeyValuePair");
         roarray = Class.forName("sleep.runtime.CollectionWrapper");
      }
      catch (Exception ex) { }
   }

   /** Pops a Key/Value pair object off of the argument stack.  A Key/Value pair is created using
       the => operator within Sleep scripts.  If the top argument on this stack was not created using
       =>, this function will try to parse a key/value pair using the pattern: [key]=[value] */
   public static KeyValuePair getKeyValuePair(Stack arguments)
   {
      Scalar temps = getScalar(arguments);

      if (temps.objectValue() != null && temps.objectValue().getClass() == kvpair)
         return (KeyValuePair)temps.objectValue();

      Scalar key, value;

      String temp = temps.toString();

      if (temp.indexOf('=') > -1)
      {
         key   = SleepUtils.getScalar(temp.substring(0, temp.indexOf('=')));
         value = SleepUtils.getScalar(  temp.substring( temp.indexOf('=') + 1, temp.length() ) );
      }
      else
      {
         key   = SleepUtils.getScalar(temp);
         value = SleepUtils.getEmptyScalar();
      }

      return new KeyValuePair(key, value);
   }

   /** Flattens the specified scalar array.  The <var>toValue</var> field can be null. */
   public static Scalar flattenArray(Scalar fromValue, Scalar toValue)
   {
      if (toValue == null) { toValue = SleepUtils.getArrayScalar(); }

      Iterator i = fromValue.getArray().scalarIterator();
      while (i.hasNext())
      {
         Scalar temp = (Scalar)i.next();

         if (temp.getArray() != null)
         {
            flattenArray(temp, toValue);
         }
         else
         {
            toValue.getArray().push(temp);
         }
      }

      return toValue;
   }

   /** normalizes the index value based on the specified length */
   public static final int normalize(int value, int length)
   {
      return value < 0 ? value + length : value;
   }
}
