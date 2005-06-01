package sleep.bridges.swing.menu;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import sleep.runtime.*;
import sleep.engine.*;

import java.util.*;

public class ScriptedMenu extends JMenu implements MenuListener, MenuParent
{
   MenuData data = new MenuData();

   public ScriptedMenu()
   {
       addMenuListener(this);
   }

   public MenuData getMenuData()
   {
       return data;
   }

   public void menuSelected(MenuEvent e)
   {
       data.BuildMenu(this);
   }

   public void menuDeselected(MenuEvent e) 
   { 
       removeAll();
   } 

   public void menuCanceled(MenuEvent e) 
   { 
       removeAll();
   }
}
