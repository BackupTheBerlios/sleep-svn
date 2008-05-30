/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved. 
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * SleepScriptEngine.java
 * @author A. Sundararajan
 */

/* Derived from code created by A. Sundararajan (sundararajana@dev.java.net) at Sun 
   What remains of this class is almost unrecognizable from the original. */

package org.dashnine.sleep;

import javax.script.*; 
import java.lang.reflect.*; 
import java.io.*; 
import java.util.*;

import sleep.bridges.*; 
import sleep.engine.*; 
import sleep.interfaces.*; 
import sleep.runtime.*;

import sleep.error.*;

public class SleepScriptEngine extends AbstractScriptEngine
{
    // my factory, may be null
    private ScriptEngineFactory factory;

    private ScriptLoader    loader;
    private Hashtable       sharedEnvironment;
    private ScriptVariables variables;

    public SleepScriptEngine()
    {
        loader = new ScriptLoader();
        sharedEnvironment = new Hashtable();
    }

    /** executes a console command */
    public Object eval(String str, ScriptContext ctx) throws ScriptException
    {
        ScriptInstance script = compile(str, ctx);
        return evalScript(script, ctx);
    }

    /** executes a script */
    public Object eval(Reader reader, ScriptContext ctx) throws ScriptException
    {
        ScriptInstance script = compile(readFully(reader), ctx);
        return evalScript(script, ctx);
    }

    private Object evalScript(ScriptInstance script, ScriptContext context)
    {
        /* install global bindings */
        Bindings global = context.getBindings(ScriptContext.GLOBAL_SCOPE);

        if (global != null)
        {
           Iterator i = global.entrySet().iterator();
           while (i.hasNext())
           {
              Map.Entry value = (Map.Entry)i.next();
              script.getScriptVariables().putScalar("$" + value.getKey().toString(), ObjectUtilities.BuildScalar(true, value.getValue()));
           }
        }

        /* install local bindings */
        Bindings local = context.getBindings(ScriptContext.ENGINE_SCOPE);
        Map locals = new HashMap();

        if (local != null)
        {
           Iterator i = local.entrySet().iterator();
           while (i.hasNext())
           {
              Map.Entry value = (Map.Entry)i.next();
              locals.put("$" + value.getKey().toString(), ObjectUtilities.BuildScalar(true, value.getValue())  );
           }
        }

        if (locals.get("$" + ScriptEngine.FILENAME) != null)
        {
           script.getScriptVariables().putScalar("$__SCRIPT__", (Scalar)locals.get("$" + ScriptEngine.FILENAME));
        }        

        if (locals.get("$" + ScriptEngine.ARGV) != null)
        {
           script.getScriptVariables().putScalar("@ARGV", (Scalar)locals.get("$" + ScriptEngine.ARGV));
        }

        return SleepUtils.runCode(script.getRunnableScript(), "eval", script, SleepUtils.getArgumentStack(locals)).objectValue();
    }

    private static class WarningWatcher implements RuntimeWarningWatcher
    {
        protected ScriptContext context;

        public WarningWatcher(ScriptContext _context)
        {
           context = _context;
        }

        public void processScriptWarning(ScriptWarning warning)    
        {
           System.out.println(warning.toString());
        }
    }

    private ScriptInstance compile(String text, ScriptContext context) throws ScriptException
    {
        try
        {
           ScriptInstance script = loader.loadScript("eval", text, sharedEnvironment);
           script.addWarningWatcher(new WarningWatcher(context));
           return script;
        }
        catch (YourCodeSucksException ex)
        {
           throw new ScriptException(ex.formatErrors());
        }
    }

    public ScriptEngineFactory getFactory()
    {
	synchronized (this)
        {
	    if (factory == null)
            {
	    	factory = new SleepScriptEngineFactory();
	    }
        }
	return factory;
    }

    public Bindings createBindings()
    {
        return new SimpleBindings();
    }

    // package-private methods
    void setFactory(ScriptEngineFactory factory)
    {
        this.factory = factory;
    }

    private String readFully(Reader reader) throws ScriptException 
    {
        char[] arr = new char[8*1024]; // 8K at a time
        StringBuilder buf = new StringBuilder();
        int numChars;
        try 
        {
            while ((numChars = reader.read(arr, 0, arr.length)) > 0) 
            {
                buf.append(arr, 0, numChars);
            }
        } 
        catch (IOException exp) 
        {
            throw new ScriptException(exp);
        }
        return buf.toString();
    }
}
