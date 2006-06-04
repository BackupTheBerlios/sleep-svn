/** 
   SLEEP - Simple Language for Environment Extension Purposes
 .-----------------------------.
 | sleep.engine.GeneratedSteps |______________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/
 
   Description: A class providing methods for constructing an instance of each
     of the sleep atomic steps. :)
 
   Documentation: 

   Changelog:
 
 |____________________________________________________________________________|
*/

package sleep.engine;

import java.util.*;
import sleep.interfaces.*;
import sleep.engine.atoms.*;
import sleep.runtime.*;

import java.io.Serializable;

/** A class providing static methods for constructing an atomic step of a specific type */
public class GeneratedSteps
{
    public static Step Operate(String oper)
    {
       Step temp = new Operate(oper);
       return temp;
    }

    public static Step Return(int type)
    {
       Step temp = new Return(type);
       return temp;
    }

    public static Step SValue(Scalar value)
    {
       Step temp = new SValue(value);
       return temp;
    }

    public static Check Check(String nameOfOperator, Block setupOperands)
    {
       Check temp = new Check(nameOfOperator, setupOperands);
       return temp;
    }

    public static Step Goto(Check conditionForGoto, Block ifTrue, Block ifFalse, Block increment, boolean shouldLoop)
    {
       Goto temp = new Goto(conditionForGoto);
       temp.setChoices(ifTrue, ifFalse);
       temp.setLoop(shouldLoop, increment);
       return temp;
    }
 
    public static Step PLiteral(String[] fragments, Block[] code, Block[] align, String evaluator)
    {
       Step temp = new PLiteral(fragments, code, align, evaluator);
       return temp;
    }

    public static Step Assign(Block variable)
    {
       Step temp = new Assign(variable);
       return temp;
    }

    public static Step AssignT()
    {
       Step temp = new AssignT();
       return temp;
    }

    public static Step CreateFrame()
    {
       Step temp = new CreateFrame();
       return temp;
    }

    public static Step Get(String value)
    {
       Step temp = new Get(value);
       return temp;
    }

    public static Step Index(String value, Block index)
    {
       Step temp = new Index(value, index);
       return temp;
    }

    public static Step Call(String function)
    {
       Step temp = new Call(function);
       return temp;
    }

    public static Step Foreach(Block source, String value, Block code)
    {
       Step temp = new Foreach(source, value, code);
       return temp;
    }

    public static Step Foreach(Block source, String key, String value, Block code)
    {
       Step temp = new Foreach(source, key, value, code);
       return temp;
    }

    public static Step CreateClosure(Block code)
    {
       Step temp = new CreateClosure(code);
       return temp;
    }

    public static Step Bind(String functionEnvironment, Block name, Block code)
    {
       Step temp = new Bind(functionEnvironment, name, code);
       return temp;
    }

    public static Step BindPredicate(String functionEnvironment, Check predicate, Block code)
    {
       Step temp = new BindPredicate(functionEnvironment, predicate, code);
       return temp;
    }

    public static Step BindFilter(String functionEnvironment, String name, Block code, String filter)
    {
       Step temp = new BindFilter(functionEnvironment, name, code, filter);
       return temp;
    }

    public static Step ObjectNew(Class name)
    {
       Step temp = new ObjectNew(name);
       return temp;
    }

    public static Step ObjectAccess(String name)
    {
       Step temp = new ObjectAccess(name, null);
       return temp;
    }

    public static Step ObjectAccessStatic(Class aClass, String name)
    {
       Step temp = new ObjectAccess(name, aClass);
       return temp;
    }
}
