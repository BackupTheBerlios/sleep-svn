package sleep.bridges.swing.menu;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import sleep.runtime.*;
import sleep.engine.*;

import sleep.bridges.SleepClosure;

public class ScriptedItem extends JMenuItem implements ActionListener
{
   protected SleepClosure code;

   public ScriptedItem(SleepClosure _code)
   {
       code = _code;
       addActionListener(this);
   }

   public void actionPerformed(ActionEvent e) 
   { 
       code.callClosure(e.getActionCommand(), null, new Stack());        
   }
}
