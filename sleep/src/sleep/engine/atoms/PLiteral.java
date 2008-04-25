package sleep.engine.atoms;

import java.util.*;
import sleep.interfaces.*;
import sleep.engine.*;
import sleep.runtime.*;

import java.io.Serializable;

public class PLiteral extends Step
{
   String evaluator;

   public String toString(String prefix)
   {
      StringBuffer temp = new StringBuffer();
      temp.append(prefix);
      temp.append("[Parsed Literal]  "+evaluator+"\n");

      Iterator i = fragments.iterator();

      while (i.hasNext())
      {
         Fragment f = (Fragment)i.next();

         switch (f.type)
         {
            case STRING_FRAGMENT:
              temp.append(prefix + "   [String]: " + f.element + "\n");
              break;
            case ALIGN_FRAGMENT:
              temp.append(prefix + "   [Align Next Value]\n");
              break;
            case VAR_FRAGMENT:
              temp.append(prefix + "   [Access Variable]\n");
              break;
         }
      }

      return temp.toString();
   }

   public PLiteral(String _evaluator)
   {
       evaluator = _evaluator;
   }

   public Scalar evaluate(ScriptEnvironment e)
   {
      String value = buildString(e);
      Scalar rv;

      if (evaluator != null && e.getEnvironment().get(evaluator) != null)
      {
         Evaluation temp = (Evaluation)e.getEnvironment().get(evaluator);
         rv = temp.evaluateString(e.getScriptInstance(), value);
      }
      else
      {
         rv = SleepUtils.getScalar(value);
      }

      e.getCurrentFrame().push(rv);
      return rv;
   }

   public static final int STRING_FRAGMENT = 1;
   public static final int ALIGN_FRAGMENT  = 2;
   public static final int VAR_FRAGMENT    = 3;
  
   private static final class Fragment implements Serializable
   {
      public Object element;
      public int    type;
   }

   private List fragments = new LinkedList();

   public void addFragment(int type, Object element)
   {
      Fragment f = new Fragment();
      f.element  = element;
      f.type     = type;
      fragments.add(f);
   }

   private String buildString(ScriptEnvironment e)
   {
      StringBuffer result = new StringBuffer();
      int          align  = 0;

      String       temp;
      Iterator i = fragments.iterator();

      while (i.hasNext())
      {
         Fragment f = (Fragment)i.next();

         switch (f.type)
         {
            case STRING_FRAGMENT:
              result.append(f.element);
              break;
            case ALIGN_FRAGMENT:
              align = ((Scalar)e.getCurrentFrame().remove(0)).getValue().intValue();
              break;
            case VAR_FRAGMENT:
              temp  = ((Scalar)e.getCurrentFrame().remove(0)).getValue().toString();

              for (int z = 0 - temp.length(); z > align; z--)
              {
                 result.append(" ");
              }

              result.append(temp);

              for (int y = temp.length(); y < align; y++)
              {
                 result.append(" ");
              }

              align = 0;              
              break;
         }
      }

      e.KillFrame();
      return result.toString();
   }
}



