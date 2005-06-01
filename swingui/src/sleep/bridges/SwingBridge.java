package sleep.bridges;

import sleep.runtime.*;
import java.util.*;
import sleep.interfaces.*;

import sleep.bridges.swing.MenuBridge;
import sleep.bridges.swing.ComponentBridge;
import sleep.bridges.swing.HelperBridge;
import sleep.bridges.SleepClosure;

import javax.swing.*;

/** A bridge for scripting UI's with the Swing toolkit and accessing these scripted UI's in your application */
public class SwingBridge implements Loadable
{
   private MenuBridge      menuBridge;
   private ComponentBridge componentBridge;
   private HelperBridge    helperBridge;

   /** Instantiates a new instance of this swing bridge.  Please note that the swing bridge can be setup as a global
       bridge with no issues. */
   public SwingBridge()
   {
      menuBridge      = new MenuBridge();
      componentBridge = new ComponentBridge();
      helperBridge    = new HelperBridge();
   }

   /** Something called by the script loader to install this bridge into a script instance */
   public boolean scriptLoaded(ScriptInstance si) 
   {
      Hashtable env = si.getScriptEnvironment().getEnvironment();

      menuBridge.scriptLoaded(si);
      componentBridge.scriptLoaded(si);
      helperBridge.scriptLoaded(si);

      return true;
   }


   /** Something called by the script loader to clean up any resources related to the specified script instance */
   public boolean scriptUnloaded(ScriptInstance si)
   {
      return true;
   }
  
   /** returns a component registered by the name id */
   public JComponent getComponent(String id)
   {
      return componentBridge.getComponent(id);
   }

   /** returns a scripted top-level popup menu named, name.  The popup menu is regenerated each time this method is called. */
   public JPopupMenu getPopupMenu(String name)
   {
      return menuBridge.getPopupMenu(name);
   }

   /** returns a scripted top-level menu named, name.  The menu is regenerated each time this method is called. */
   public JMenu getMenu(String name)
   {
      return menuBridge.getMenu(name);
   }

   /** returns a scripted top-level popup menu named, name.  The popup menu is regenerated each time this method is called. */
   public JPopupMenu getPopupMenu(String name, Variable context)
   {
      return null;
   }

   /** convienence method to iterate through the rest of the sleep key=value pair arguments on the stack and update the
       properties of the passed in component.  This is something like what would be in BridgeUtilities but since it is swing
       related it is located here.    

       @param component the component to update the attributes for
       @param locals a Stack of Key/Value Pair scalars outlining which properties to change in the component

       @return the passed in component
     */
   public static Object updateAttributes(Object component, Stack locals)
   {
      return ComponentBridge.updateAttributes(component, locals);
   }
}
