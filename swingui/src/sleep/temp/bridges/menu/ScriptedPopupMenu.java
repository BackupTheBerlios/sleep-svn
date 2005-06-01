package sleep.bridges.swing.menu;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import sleep.runtime.*;
import sleep.engine.*;

import java.util.*;

public class ScriptedPopupMenu extends JPopupMenu implements MenuParent
{
   MenuData data = new MenuData();

   public MenuData getMenuData()
   {
       return data;
   }
}
