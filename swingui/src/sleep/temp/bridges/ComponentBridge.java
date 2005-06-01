package sleep.bridges.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.lang.reflect.*;

import java.util.*;

import sleep.bridges.swing.menu.*;
import sleep.bridges.*;

import sleep.engine.*;
import sleep.interfaces.*;
import sleep.runtime.*;

/** This bridge allows for the setup and modification of properties in various swing components. */
public class ComponentBridge implements Function, Environment
{
    protected static HashMap functionData = new HashMap();
    protected HashMap dialogData   = new HashMap();

    private static Class C_COLOR, C_RECTANGLE, C_DIMENSION, C_ICON;

    static
    {
       try
       {
          C_COLOR      = Class.forName("java.awt.Color");
          C_RECTANGLE  = Class.forName("java.awt.Rectangle");
          C_DIMENSION  = Class.forName("java.awt.Dimension");
          C_ICON       = Class.forName("javax.swing.Icon");
       }
       catch (Exception ex) { }
    }

    private static String[] COMPONENTS = new String[] {
         "Button",
         "CheckBox",
         "RadioButton",
         "ToggleButton",
         "Label",
         "List",
         "Separator",
         "Spinner",
         "TabbedPane",
         "TextField",
         "EditorPane",
         "ToolBar",
         "Tree",
         "Dialog",
         "ToolBar",
         "ProgressBar",
         "Slider",
         "ScrollPane",
         "Table",
         "ComboBox"
    };

    private static class ScriptedComponent
    {
       public JPanel component = null;
       public SleepClosure closure = null;
    }

    public void bindFunction(ScriptInstance si, String type, String label, Block code)
    {
       ScriptedComponent comp = new ScriptedComponent();
       comp.component = new JPanel();
       comp.closure   = new SleepClosure(si, code);
       
       dialogData.put(label, comp);
    }

    public void scriptLoaded(ScriptInstance si)
    {
       Hashtable env = si.getScriptEnvironment().getEnvironment();

       for (int z = 0; z < COMPONENTS.length; z++)
       {
          env.put("&" + COMPONENTS[z], this);
       }

       env.put("&update", this);
       env.put("dialog", this); 
       env.put("&showFrame", this);
    }

    public Scalar evaluate(String id, ScriptInstance si, Stack locals)
    {
       id = id.substring(1);

       try
       {
          Object value;
          Class  myClass;
          
          if (id.equals("update"))
          {
             value      = BridgeUtilities.getObject(locals);
             myClass    = value.getClass();
          }
          else
          {
             myClass    = Class.forName("javax.swing.J" + id);
             value      = myClass.getConstructor(new Class[0]).newInstance(new Object[0]);
          }
      
          return SleepUtils.getScalar(updateAttributes(value, locals));
       }
       catch (Exception ex)
       {
          ex.printStackTrace(); 
      }
       return SleepUtils.getEmptyScalar();
    }

    public static Object updateAttributes(Object component, Stack locals)
    {
       try
       {
          HashMap properties = getFunctionData(component.getClass());

          while (!locals.isEmpty())
          {
             KeyValuePair kvp = BridgeUtilities.getKeyValuePair(locals);

             if (properties.containsKey(kvp.getKey().toString()))
             {
                modifyObject(component, (Method)properties.get(kvp.getKey().toString()), kvp.getValue());
             }
          }

          return component;
       }
       catch (Exception ex)
       {
          ex.printStackTrace();
       }
       return null;
    }


    public static void modifyObject(Object modify, Method setter, Scalar value) throws Exception
    {
       Object args[] = new Object[1];

       Class type = setter.getParameterTypes()[0];

       if (SleepUtils.isEmptyScalar(value))
       {
          args[0] = null;
       }
       else if (type == C_RECTANGLE)
       {
          String[] dump = value.toString().split(",");
          Rectangle rect = new Rectangle(Integer.parseInt(dump[0].trim()), Integer.parseInt(dump[1].trim()), Integer.parseInt(dump[2].trim()), Integer.parseInt(dump[3].trim()));
          args[0] = rect;
       }
       else if (type == C_DIMENSION)
       {
          String[] dump = value.toString().split("x");
          Dimension dims = new Dimension(Integer.parseInt(dump[0].trim()), Integer.parseInt(dump[1].trim()));
          args[0] = dims;          
       }
       else if (type == C_COLOR)
       {
          Color temp = Color.decode(value.toString());
          args[0] = temp;
       }
       else if (type == C_ICON)
       {
          ImageIcon temp = new ImageIcon(value.toString());
          args[0] = temp;
       }
       else
       {
          args[0] = ObjectUtilities.buildArgument(type, value, null);
       }

       setter.invoke(modify, args);
    }

    public static HashMap getFunctionData(Class FuncClass)
    {
        if (functionData.containsKey(FuncClass))
             return (HashMap)functionData.get(FuncClass);

        HashMap temp = new HashMap();
        functionData.put(FuncClass, temp);

        Method[] methods = FuncClass.getMethods();

        for (int x = 0; x < methods.length; x++)
        {
            String name = methods[x].getName();
            if (name.startsWith("set") && methods[x].getParameterTypes().length == 1)
                 temp.put((name.charAt(3) + "").toLowerCase() + name.substring(4), methods[x]);
        }

        return temp;
    }

    public JComponent getComponent(String name)
    {
        ScriptedComponent temp = (ScriptedComponent)dialogData.get(name);

        temp.closure.getVariables().putScalar("$component", SleepUtils.getScalar(temp.component));
        temp.closure.callClosure(name, null, null);

        return temp.component;
    }
}


