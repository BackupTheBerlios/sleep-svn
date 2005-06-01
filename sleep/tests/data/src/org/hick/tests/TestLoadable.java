package org.hick.tests;

import sleep.interfaces.*;
import sleep.runtime.*;

public class TestLoadable implements Loadable
{
   public boolean scriptLoaded(ScriptInstance si)
   {
      si.getScriptEnvironment().getEnvironment().put("&foo", new FooFunction());
      return true;
   }

   public boolean scriptUnloaded(ScriptInstance si)
   {
      return true;
   }
}
