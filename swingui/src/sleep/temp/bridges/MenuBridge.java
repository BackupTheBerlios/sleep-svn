package sleep.bridges.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import sleep.bridges.swing.menu.*;
import sleep.bridges.*;

import sleep.engine.*;
import sleep.interfaces.*;
import sleep.runtime.*;

public class MenuBridge implements Environment
{
    protected MenuData toplevel  = new MenuData();
    protected Variable variables = null;

    public MenuBridge()
    {
       MenuData.SetParent(new MenuParent() 
       {
           public MenuData getMenuData() { return toplevel; }
           public JMenuItem add(JMenuItem item) { return null; }
           public void addSeparator() { }
       });
    }

    public void scriptLoaded(ScriptInstance si)
    {
       Hashtable env = si.getScriptEnvironment().getEnvironment();

       env.put("menu", this);
       env.put("item", this);
       env.put("&addSeparator", new addSeparator());
       env.put("&getPopupMenu", new getPopupMenu());
       env.put("&getMenu", new getMenu());
    }

    public void bindFunction(ScriptInstance si, String type, String label, Block code)
    {
       JMenuItem     currentMenu;
       SleepClosure  closure      = new SleepClosure(si, code);    
       String        id;

       if (label.indexOf('&') > -1)
          id = label.substring(0, label.indexOf('&')) + label.substring(label.indexOf('&') + 1, label.length());
       else
          id = label;

       if (type.equals("menu"))
       {
          currentMenu = MenuData.GetParent().getMenuData().getSubMenu(id);
          ((ScriptedMenu)currentMenu).getMenuData().addLevel(closure); // says add this closure to create the next level deep when this menu is shown
       }
       else
       {
          currentMenu = new ScriptedItem(closure);
          MenuData.GetParent().add(currentMenu);  // adds the "item" to the current menu item
       }

       currentMenu.setText(id);

       if (label.indexOf('&') > -1)
          currentMenu.setMnemonic(label.charAt(label.indexOf('&') + 1));
    }
    
    private static class addSeparator implements Function
    {
       public Scalar evaluate(String function, ScriptInstance script, Stack locals)
       {
          MenuData.GetParent().addSeparator();
          return SleepUtils.getEmptyScalar();
       }
    }

    private class getPopupMenu implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack locals)
       {
          return SleepUtils.getScalar(getPopupMenu(BridgeUtilities.getString(locals, "__default__")));
       }
    }

    private class getMenu implements Function
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack locals)
       {
          return SleepUtils.getScalar(getMenu(BridgeUtilities.getString(locals, "__default__")));
       }
    }

    public JPopupMenu getPopupMenu(String description)
    {
       ScriptedMenu menu = toplevel.getSubMenu(description);       

       ScriptedPopupMenu temp = new ScriptedPopupMenu();
       menu.getMenuData().BuildMenu(temp);

       return temp;
    }

    public JMenu getMenu(String description)
    {
       ScriptedMenu menu = toplevel.getSubMenu(description);       
       menu.getMenuData().BuildMenu(menu);

       return menu;
    }
}
