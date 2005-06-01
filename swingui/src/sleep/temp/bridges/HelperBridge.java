package sleep.bridges.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.lang.reflect.*;

import java.util.*;

import sleep.bridges.swing.models.*;
import sleep.bridges.*;

import sleep.engine.*;
import sleep.interfaces.*;
import sleep.runtime.*;

/** This bridge allows for the setup and modification of properties in various swing components. */
public class HelperBridge
{
    public void scriptLoaded(ScriptInstance si)
    {
       Hashtable env = si.getScriptEnvironment().getEnvironment();

       env.put("&buildListModel", new buildListModel());
       env.put("&buildTableModel", new buildTableModel());
    }

    private static class buildListModel implements Function 
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack locals)
       {
          ScalarArray value = BridgeUtilities.getArray(locals);
          ScalarArray temp  = new ScriptedListModel(value);
          return SleepUtils.getArrayScalar(temp);
       }
    }

    private static class buildTableModel implements Function 
    {
       public Scalar evaluate(String name, ScriptInstance si, Stack locals)
       {
          SleepClosure rows = BridgeUtilities.getFunction(locals, si);
          ScalarArray  cols = BridgeUtilities.getArray(locals);

          return SleepUtils.getScalar(new ScriptedTableModel(rows, cols));
       }
    }
}

