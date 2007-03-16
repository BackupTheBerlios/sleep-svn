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
    public static Step PopTry()
    {
       Step temp = new PopTry();
       return temp;
    }
 
    public static Step Try(Block owner, Block handler, String var)
    {
       Step temp = new Try(owner, handler, var);
       return temp;
    }

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

    public static Step IteratorCreate(String key, String value)
    {
       return new Iterate(key, value, Iterate.ITERATOR_CREATE);
    }

    public static Step IteratorNext()
    {
       return new Iterate(null, null, Iterate.ITERATOR_NEXT);
    }

    public static Step IteratorDestroy()
    {
       return new Iterate(null, null, Iterate.ITERATOR_DESTROY);
    }

    public static Check Check(String nameOfOperator, Block setupOperands)
    {
       Check temp = new Check(nameOfOperator, setupOperands);
       return temp;
    }

    public static Step Goto(Check conditionForGoto, Block ifTrue, Block increment)
    {
       Goto temp = new Goto(conditionForGoto);
       temp.setChoices(ifTrue);
       temp.setIncrement(increment);
       return temp;
    }

    public static Step Decide(Check conditionForGoto, Block ifTrue, Block ifFalse)
    {
       Decide temp = new Decide(conditionForGoto);
       temp.setChoices(ifTrue, ifFalse);
       return temp;
    }
 
    public static Step PLiteral(String evaluator)
    {
       Step temp = new PLiteral(evaluator);
       return temp;
    }

    public static Step Assign(Block variable)
    {
       Step temp = new Assign(variable);
       return temp;
    }

    public static Step AssignAndOperate(Block variable, String operator)
    {
       Step temp = new Assign(variable, new Operate(operator));
       return temp;
    }

    public static Step AssignT()
    {
       Step temp = new AssignT();
       return temp;
    }

    public static Step AssignTupleAndOperate(String operator)
    {
       Step temp = new AssignT(new Operate(operator));
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
