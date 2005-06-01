package sleep.bridges.swing.menu;

import sleep.engine.Block;
import sleep.bridges.SleepClosure;
import sleep.runtime.*;
import sleep.interfaces.*;

import java.util.*;

public class MenuData
{
   protected static Stack ParentMenu = new Stack();

   public static void SetParent(MenuParent m)
   {
       ParentMenu.push(m);
   }

   public static MenuParent GetParent()
   {
       return (MenuParent)ParentMenu.peek();
   }

   public static void FinishParent()
   {
       ParentMenu.pop();
   }

   protected LinkedList     code       = new LinkedList();
   protected HashMap        containers = new HashMap();

   public ScriptedMenu getSubMenu(String name)
   {
       ScriptedMenu menu = (ScriptedMenu)containers.get(name.toUpperCase());
       if (menu == null)
       {
          menu = new ScriptedMenu();
          containers.put(name.toUpperCase(), menu);
          MenuData.GetParent().add(menu);
       }
       
       return menu;
   }

   public void addLevel(SleepClosure closure)
   {
       code.add(closure);
   }

   public boolean isValidCode()
   {
       Iterator i = code.iterator();
       while (i.hasNext())
       {
          SleepClosure temp = (SleepClosure)i.next();
          if (!temp.getOwner().isLoaded())
          {
             i.remove();
          }
       }

       return code.size() > 0;
   }

   public void BuildMenu(MenuParent parent)
   {
       SetParent(parent);

       Iterator i = code.iterator();
       while (i.hasNext())
       {
          SleepClosure temp = (SleepClosure)i.next();
          if (temp.getOwner().isLoaded())
          {
             temp.callClosure("", null, null);                     
          }
          else
          {
             i.remove();
          }
       }

       FinishParent();
   }
}
