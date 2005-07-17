/*
   SLEEP - Simple Language for Environment Extension Purposes
 .------------------------------.
 | sleep.engine.ObjectUtilities |_____________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.hick.org/~raffi/

   Description: A bunch of utility methods for Sleeps object interface: HOES
           Haphazard Object Extensions for Sleep

   Documentation:

   Changelog:
   03/19/2005 - rewrote the argument matching stuff...
   03/12/2005 - date created

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.engine;

import java.lang.reflect.*;
import java.util.*;

import sleep.runtime.*;

import sleep.engine.types.*;
import sleep.interfaces.Function;

import sleep.bridges.*;

public class ObjectUtilities
{
   private static Class STRING_SCALAR;
   private static Class INT_SCALAR; 
   private static Class DOUBLE_SCALAR;
   private static Class LONG_SCALAR;
   public  static Class BOOLEAN_TYPE, BYTE_TYPE, CHARACTER_TYPE, DOUBLE_TYPE, FLOAT_TYPE, INTEGER_TYPE, LONG_TYPE, STRING_TYPE, OBJECT_TYPE;

   static
   {
      try
      {
         STRING_SCALAR = Class.forName("sleep.engine.types.StringValue");
         INT_SCALAR    = Class.forName("sleep.engine.types.IntValue");
         DOUBLE_SCALAR = Class.forName("sleep.engine.types.DoubleValue");
         LONG_SCALAR   = Class.forName("sleep.engine.types.LongValue");

         BOOLEAN_TYPE    = Class.forName("java.lang.Boolean");
         BYTE_TYPE       = Class.forName("java.lang.Byte");
         CHARACTER_TYPE  = Class.forName("java.lang.Character");
         DOUBLE_TYPE     = Class.forName("java.lang.Double");
         FLOAT_TYPE      = Class.forName("java.lang.Float");
         INTEGER_TYPE    = Class.forName("java.lang.Integer");
         LONG_TYPE       = Class.forName("java.lang.Long");
         OBJECT_TYPE     = Class.forName("java.lang.Object");
         STRING_TYPE     = Class.forName("java.lang.String");
      }
      catch (Exception ex) { }
   }

   public static final int ARG_MATCH_YES   = 1;
   public static final int ARG_MATCH_NO    = 2;
   public static final int ARG_MATCH_MAYBE = 3;

   public static int isArgMatch(Class[] check, Stack arguments)
   {
      int value = ARG_MATCH_YES;

      for (int z = 0; z < check.length; z++)
      {
         Scalar scalar = (Scalar)arguments.get(check.length - z - 1);
         Class stemp   = scalar.getClass();
         String sstring = scalar.toString();

         if (SleepUtils.isEmptyScalar(scalar))
         {
             // do nothing, this argument is a give me
         }
         else if (check[z].isArray() && scalar.getArray() != null)
         {
             if (check[z].getComponentType() == OBJECT_TYPE)
             {
                value = ARG_MATCH_MAYBE;
             }
             else
             {
                Class mytype = getArrayType(scalar, null);
 
                if (mytype == check[z].getComponentType())
                {
                   value = ARG_MATCH_YES;
                }
                else
                {
                   value = ARG_MATCH_NO;
                }
             }
         }
         else if (check[z].isPrimitive() && !(stemp == INT_SCALAR || stemp == DOUBLE_SCALAR || stemp == LONG_SCALAR))
         {
             value = ARG_MATCH_MAYBE;
         }
         else if (check[z].isInterface())
         {
             if (!SleepUtils.isFunctionScalar(scalar) && !check[z].isInstance(scalar.objectValue()))
                  return ARG_MATCH_NO;
         }
         else if (check[z] == STRING_TYPE && stemp != STRING_SCALAR)
         {
             value = ARG_MATCH_MAYBE;
         }
         else if (!check[z].isInstance(scalar.objectValue()))
         {
             return ARG_MATCH_NO;
         }
      }
 
      return value;
   }

   public static Method findMethod(Class theClass, String method, Stack arguments)
   {
      int      size    = arguments.size();

      Method   temp    = null;
      Method[] methods = theClass.getMethods();

      for (int x = 0; x < methods.length; x++) 
      {
         if (methods[x].getName().equals(method) && methods[x].getParameterTypes().length == size)
         {
             if (size == 0)
                   return methods[x];

             int value = isArgMatch(methods[x].getParameterTypes(), arguments);
             if (value == ARG_MATCH_YES) 
                   return methods[x];

             if (value == ARG_MATCH_MAYBE)
                   temp = methods[x];
         }
      }

      return temp;
   }

   public static Constructor findConstructor(Class theClass, Stack arguments)
   {
      int      size    = arguments.size();

      Constructor   temp         = null;
      Constructor[] constructors = theClass.getConstructors();

      for (int x = 0; x < constructors.length; x++) 
      {
         if (constructors[x].getParameterTypes().length == size)
         {
             if (size == 0)
                   return constructors[x];

             int value = isArgMatch(constructors[x].getParameterTypes(), arguments);
             if (value == ARG_MATCH_YES)
                   return constructors[x];

             if (value == ARG_MATCH_MAYBE)
                   temp = constructors[x];
         }
      }

      return temp;
   }

   public static Object buildArgument(Class type, Scalar value, ScriptInstance script)
   {
      if (type.isArray() && value.getArray() != null)
      {
         Class atype = getArrayType(value, type.getComponentType());

         Object arrayV = Array.newInstance(atype, value.getArray().size());
         Iterator i = value.getArray().scalarIterator();
         int x = 0;
         while (i.hasNext())
         {
            Scalar temp = (Scalar)i.next();
            Array.set(arrayV, x, buildArgument(atype, temp, script));
            x++;
         }

         return arrayV;
      }
      else if (type.isPrimitive())
      {
         if (type == Boolean.TYPE)
         {
            return Boolean.valueOf(value.intValue() != 0);
         }
         else if (type == Byte.TYPE)
         {
            return new Byte((byte)value.intValue());
         }
         else if (type == Character.TYPE)
         {
            return new Character(value.toString().charAt(0));
         }
         else if (type == Double.TYPE)
         {
            return new Double(value.doubleValue());
         }
         else if (type == Float.TYPE)
         {
            return new Float((float)value.doubleValue());
         }
         else if (type == Integer.TYPE)
         {
            return new Integer(value.intValue());
         }
         else if (type == Short.TYPE)
         {
            return new Short((short)value.intValue());
         }
         else if (type == Long.TYPE)
         {
            return new Long(value.longValue());
         }
      }
      else if (SleepUtils.isEmptyScalar(value))
      {
         return null;
      }
      else if (type.isInterface() && SleepUtils.isFunctionScalar(value))
      {
         return BuildInterface(type, SleepUtils.getFunctionFromScalar(value, script), script);
      }
      else if (type == STRING_TYPE)
      {
         return value.toString();
      }

      return value.objectValue();
   }

   public static String buildArgumentErrorMessage(Class theClass, String method, Class[] expected, Object[] parameters)
   {
      StringBuffer tempa = new StringBuffer(method + "(");
      
      for (int x = 0; x < expected.length; x++)
      {
         tempa.append(expected[x].getName());

         if ((x + 1) < expected.length)
            tempa.append(", ");
      }
      tempa.append(")");

      StringBuffer tempb = new StringBuffer("(");
      for (int x = 0; x < parameters.length; x++)
      {
         if (parameters[x] != null)
            tempb.append(parameters[x].getClass().getName());
         else
            tempb.append("null");

         if ((x + 1) < parameters.length)
            tempb.append(", ");
      }
      tempb.append(")");

      return "bad arguments " + tempb.toString() + " for " + tempa.toString() + " in " + theClass;
   } 

   public static Object[] buildArgumentArray(Class[] types, Stack arguments, ScriptInstance script)
   {
      Object[] parameters = new Object[types.length];

      for (int x = 0; x < parameters.length; x++)
      {
         Scalar temp = (Scalar)arguments.pop();
         parameters[x] = buildArgument(types[x], temp, script);
      }
 
      return parameters;
   }


   public static Scalar BuildScalar(boolean primitives, Object value)
   {
      if (value == null)
         return SleepUtils.getEmptyScalar();

      if (value.getClass().isArray())
      {
         Scalar array = SleepUtils.getArrayScalar();
         for (int x = 0; x < Array.getLength(value); x++)
         {
            array.getArray().push(BuildScalar(true, Array.get(value, x)));
         }

         return array;
      }

      Class check = value.getClass();

      if (primitives)
      {
         if (check == BOOLEAN_TYPE)
         {
            return SleepUtils.getScalar(  ((Boolean)value).booleanValue() ? 1 : 0 );
         }
         else if (check == BYTE_TYPE)
         {
            return SleepUtils.getScalar(  (int)( ((Byte)value).byteValue() )  );
         }
         else if (check == CHARACTER_TYPE)
         {
            return SleepUtils.getScalar(  value.toString()  );
         }
         else if (check == DOUBLE_TYPE)
         {
            return SleepUtils.getScalar(  ((Double)value).doubleValue()   );
         }
         else if (check == FLOAT_TYPE)
         {
            return SleepUtils.getScalar(  (double)( ((Float)value).floatValue() )  );
         }
         else if (check == INTEGER_TYPE)
         {
            return SleepUtils.getScalar(  ((Integer)value).intValue()   );
         }
         else if (check == LONG_TYPE)
         {
            return SleepUtils.getScalar(  ((Long)value).longValue()   );
         }
      }

      if (check == STRING_TYPE)
      {
         return SleepUtils.getScalar(value.toString());
      }
      else 
      {
         return SleepUtils.getScalar(value);
      }

   }

   public static Object BuildInterface(Class className, Function subroutine, ScriptInstance script)
   {
      InvocationHandler temp = new ProxyInterface(subroutine, script);
      return Proxy.newProxyInstance(className.getClassLoader(), new Class[] { className }, temp);
   } 

   private static class ProxyInterface implements InvocationHandler
   {
      protected ScriptInstance    script;
      protected Function          func;

      public ProxyInterface(Function _method, ScriptInstance _script)
      {
         func        = _method;
         script      = _script;
      }

      public Object invoke(Object proxy, Method method, Object[] args)
      {
         Stack temp = new Stack();

         if (args != null)
         {
            for (int z = args.length - 1; z >= 0; z--)
            { 
               temp.push(BuildScalar(true, args[z]));
            }
         }

         Scalar value = func.evaluate(method.getName(), script, temp); 
         script.getScriptEnvironment().clearReturn();

         if (value != null)
            return value.objectValue();

         return null;
      }
   }

   /** Determines the primitive type of the specified array.  Primitive Sleep values (int, long, double) will return the appropriate Number.TYPE class.  This is an important distinction as Double.TYPE != new Double().getClass() */
   public static Class getArrayType(Scalar value, Class defaultc)
   {
      if (value.getArray() != null && value.getArray().size() > 0 && (defaultc == null || defaultc == sleep.engine.ObjectUtilities.OBJECT_TYPE))
      {
          for (int x = 0; x < value.getArray().size(); x++)
          {
             if (value.getArray().getAt(x).getArray() != null)
             {
                return getArrayType(value.getArray().getAt(x), defaultc);
             }

             Class  elem  = value.getArray().getAt(x).getValue().getClass();
             Object tempo = value.getArray().getAt(x).objectValue();

             if (elem == DOUBLE_SCALAR)
             {
                return Double.TYPE;
             }
             else if (elem == INT_SCALAR)
             {
                return Integer.TYPE;
             }
             else if (elem == LONG_SCALAR)
             {
                return Long.TYPE;
             }
             else if (tempo != null)
             {
                return tempo.getClass();
             }
          }
      }

      return defaultc;
   }
}
