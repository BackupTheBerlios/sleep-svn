/*
   SLEEP - Simple Language for Environment Extension Purposes
 .---------------------.
 | sleep.engine.Parser |______________________________________________________
 |                                                                            |
   Author: Raphael Mudge (rsmudge@mtu.edu)
           http://www.csl.mtu.edu/~rsmudge/

   Description: parser for the SLEEP language.  Returns a sleep.engine.Block
     object suitable for being passed to the interpreter for execution.  Hell
     if you were really creative you could serialize the Block object so you
     don't have to reparse the script each time you load SLEEP.

   Documentation: To see the entire concrete syntax of the SLEEP language
     handled by this parser view the file docs/bnf.txt.

   Changes:
     1.03.2004
     - threw out the old code and redid this bad boy.  *uNF*

    11.15.2002
     - moved a number of static functions to the ParserUtilities class
     - factored out token parser functions into TokenParser class
     - cleaned up lots of code
     - factored out Token Class
     - did some code cleanup

     9.08.2002
     - rewrote and refactored the Parser, dividing the responsibilities
       between a LexicalAnalyzer and a series of Checkers.  What a nightmare.

     4.20.2002
     - added better debugging support.

     4.15.2002
     - added support for parsing native floats.

   * This software is distributed under the artistic license, see license.txt
     for more information. *

 |____________________________________________________________________________|
 */

package sleep.parser;

import java.util.*;
import sleep.error.*;
import sleep.engine.Block;

import java.io.*;

public class Parser
{
   private static final boolean DEBUG_ITER     = false;
   private static final boolean DEBUG_LEX      = false;
   private static final boolean DEBUG_COMMENTS = false;
   private static final boolean DEBUG_TPARSER  = false;

   protected String     code; /** the actual "code" for the script file. */

   protected LinkedList comments   = new LinkedList(); /** a list of all of the comments from the script file */
   protected LinkedList errors     = new LinkedList(); /** a list of all of the parser errors */
   protected LinkedList warnings   = new LinkedList(); /** a list of all of the parser warnings */

   protected TokenList  tokens     = new TokenList();
   protected LinkedList statements = new LinkedList(); /** a list of all of the statements */

   protected Block      executeMe;  // runnable block   

   public    char       EndOfTerm  = ';';

   protected LinkedList imports   = new LinkedList();
   protected HashMap    classes   = new HashMap();

   /** Used by hoes to import package names... */
   public void importPackage(String packagez)
   {
       if (packagez.endsWith(".*"))
       {
          imports.add(packagez.substring(0, packagez.length() - 2));
       }
       else
       {
          try
          {
             Class temp = Class.forName(packagez);
             classes.put(packagez.substring(packagez.lastIndexOf(".") + 1, packagez.length()), temp);
          }
          catch (Exception ex) { }
       }
   }

   public Class findImportedClass(String name)
   {
       if (classes.get(name) == null)
       {
          Class temp;
          try
          {
             temp = Class.forName(name);
             classes.put(name, temp);
             return temp;
          }
          catch (Exception ex) { }
          

          Iterator i = imports.iterator();
          while (i.hasNext())
          {
             try
             {
                temp = Class.forName(i.next().toString() + "." + name);
                classes.put(name, temp);
           
                return temp;
             }
             catch (Exception ex) { }
          }
       }
     
       return (Class)classes.get(name);
   }

   public void setEndOfTerm(char c)
   {
      EndOfTerm = c;
   }

   /** initialize the parser with the code you want me to work with */
   public Parser(String _code)
   {
      imports.add("java.lang");
      imports.add("java.util");

      code = _code;
   }

   public void addStatement(Statement state)
   {
      statements.add(state);
   }

   public LinkedList getStatements()
   {
      return statements;
   }

   public void parse() throws YourCodeSucksException
   {
      parse(new StringIterator(code));
   }

   public void parse(StringIterator siter) throws YourCodeSucksException
   {
      TokenList tokens = LexicalAnalyzer.GroupBlockTokens(this, siter);

      /** debug the tokenizer */
      if (DEBUG_LEX)
      {
         Token[] all = tokens.getTokens();
         for (int x = 0; x < all.length; x++)
         {
            System.out.println(x + ": " + all[x].toString() + " at " + all[x].getHint());
         }
      }

      if (hasErrors())
      {
         errors.addAll(warnings);
         throw new YourCodeSucksException(errors);      
      }

      LinkedList statements = TokenParser.ParseBlocks(this, tokens);

      if (DEBUG_TPARSER && statements != null)
      {
         Iterator i = statements.iterator();
         while (i.hasNext())
         {
            System.out.println("Block\n"+i.next());
         }
      }

      if (hasErrors())
      {
         errors.addAll(warnings);
         throw new YourCodeSucksException(errors);      
      }

      CodeGenerator codegen = new CodeGenerator(this);
      codegen.parseBlock(statements);

      if (hasErrors())
      {
         errors.addAll(warnings);
         throw new YourCodeSucksException(errors);      
      }

      executeMe = codegen.getRunnableBlock();

      if (DEBUG_COMMENTS)
      {
         Iterator i = comments.iterator();
         while (i.hasNext()) { System.out.print("Comment: " + i.next()); }
      }
   }

   public void reportError(String description, Token responsible)
   {
      errors.add(new SyntaxError(description, responsible.toString(), responsible.getHint()));
   }

   public void reportError(SyntaxError error)
   {
      errors.add(error);
   }

   public Block getRunnableBlock()
   {
      return executeMe;
   }

   public void reportWarning(String description, Token responsible)
   {
      warnings.add(new SyntaxError(description, responsible.toString(), responsible.getHint()));
   }

   public boolean hasErrors()
   {
      return errors.size() > 0;
   }

   public boolean hasWarnings()
   {
      return warnings.size() > 0;
   }

   public void addComment(String text)
   {
      comments.add(text);
   }

   public static void main(String args[])
   {
      try
      {
         File afile = new File(args[0]);
         BufferedReader temp = new BufferedReader(new InputStreamReader(new FileInputStream(afile)));

         StringBuffer data = new StringBuffer();

         String text;
         while ((text = temp.readLine()) != null)
         {
            data.append(text);
            data.append('\n');
         }

         Parser p = new Parser(data.toString());
         p.parse();
         System.out.println(p.getRunnableBlock());
      }
      catch (YourCodeSucksException yex)
      {
         LinkedList errors = yex.getErrors();
         Iterator i = errors.iterator();
         while (i.hasNext())
         {
            SyntaxError anError = (SyntaxError)i.next();
            System.out.println("Error: " + anError.getDescription() + " at line " + anError.getLineNumber());
            System.out.println("       " + anError.getCodeSnippet());
            if (anError.getMarker() != null)
               System.out.println("       " + anError.getMarker());
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }
}
