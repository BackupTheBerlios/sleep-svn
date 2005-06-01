package sleep.bridges.swing.menu;

import javax.swing.*;

public interface MenuParent
{
   public void addSeparator();
   public JMenuItem add(JMenuItem menuItem);
   public MenuData getMenuData();
}
