/* 
   SLEEP - Simple Language for Environment Extension Purposes 
 .----------------------------.
 | sleep.parser.CodeGenerator |_______________________________________________
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
import java.io.*;

import sleep.engine.*;
import sleep.engine.atoms.*;
import sleep.error.*;
import sleep.runtime.*;

/** Generates code for the parser.  The main thing here developers might concern themselves with is the installEscapeConstant 
function */
public class CodeGenerator implements ParserConstants
{
   protected Block CURRENT_BLOCK;
   protected Stack BACKUP_BLOCKS;

   protected Parser parser;

   protected static HashMap escape_constants = new HashMap();

   static
   {
      installEscapeConstant('t', "\t");
      installEscapeConstant('n', "\n");
      installEscapeConstant('r', "\r");
   }

   /** install a constant for escapes within strings i.e. \n could be "\n" */
   public static void installEscapeConstant(char c, String value)
   {
      escape_constants.put(c+"", value);
   }

   public Block getRunnableBlock()
   {
      return CURRENT_BLOCK;
   }

   public void add(Step n, Token token)
   {
      CURRENT_BLOCK.add(n);
      n.setInfo(token.getHint());
   }

   public void backup()
   {
      BACKUP_BLOCKS.push(CURRENT_BLOCK);
      CURRENT_BLOCK = new Block();
   }

   public Block restore()
   {
      Block temp = CURRENT_BLOCK;
      CURRENT_BLOCK = (Block)(BACKUP_BLOCKS.pop());
      return temp;
   }

   public CodeGenerator(Parser _parser)
   {
      parser = _parser;

      CURRENT_BLOCK = new Block();
      BACKUP_BLOCKS = new Stack();
   }

   public Check parsePredicate(Token data)
   {
       /** send the data string through the parser pipeline - errors earlier in the pipeline are assumed to not exist as they would have been found the first time we processed it as a block */
       Statement allData = TokenParser.ParsePredicate(parser, LexicalAnalyzer.GroupBlockTokens(parser, new StringIterator(data.toString(), data.getHint())));

       return parsePredicate(allData);
   }

   public Check parsePredicate(Statement parsePred)
   {
       Token[]  tokens  = parsePred.getTokens();
       String[] strings = parsePred.getStrings();

       Step atom;
       Check tempc;
       Block backup, a, b;
       Stack queue;  // locals is a stack created at parse time but used by the operators for keeping track of stuff.. yeah

       switch (parsePred.getType())
       {
         case PRED_EXPR:
           return parsePredicate(ParserUtilities.extract(tokens[0]));
         case PRED_IDEA: // comparing the idea != 0 to say its true... -istrue predicate provided in BasicUtilities
           if (strings[0].charAt(0) == '!' && strings[0].length() > 1)
           {
              return parsePredicate(tokens[0].copy("!-istrue (" + strings[0].substring(1, strings[0].length()) + ")"));
           }
           else
           {
              return parsePredicate(tokens[0].copy("-istrue (" + strings[0] + ")"));
           }
         case PRED_BI:
           // <idea> <string> <idea>
           backup();

           atom = GeneratedSteps.CreateFrame();
           add(atom, tokens[0]);

           parseIdea(tokens[0]);
           atom = GeneratedSteps.Push();
           add(atom, tokens[0]);

           parseIdea(tokens[2]);
           atom = GeneratedSteps.Push();
           add(atom, tokens[2]);

           tempc = GeneratedSteps.Check(strings[1], restore()); // a KillFrame is implied here
           tempc.setInfo(tokens[1].getHint());

           return tempc;
         case PRED_UNI:
           backup();

           atom = GeneratedSteps.CreateFrame();
           add(atom, tokens[1]);

           parseIdea(tokens[1]);
             
           atom = GeneratedSteps.Push();
           add(atom, tokens[1]);

           tempc = GeneratedSteps.Check(strings[0], restore());
           tempc.setInfo(tokens[0].getHint());

           return tempc;
         case PRED_AND:
           tempc = null;
           queue = new Stack();
           
           for (int x = 0; x < tokens.length; x++)
           {
              queue.push(tokens[x]);
           }           
           while (!queue.isEmpty())
           { 
              Token t = (Token)(queue.pop());
              if (!t.toString().equals("&&"))
              {
                 Check oldtemp = tempc;
                 tempc = parsePredicate(t);
                 tempc.setChoices(oldtemp, null);
              }
           }
           return tempc;
         case PRED_OR:
           tempc = null;
           queue = new Stack();

           for (int x = 0; x < tokens.length; x++)
           {
              queue.push(tokens[x]);
           }          

           while (!queue.isEmpty())
           { 
              Token t = (Token)(queue.pop());
              if (!t.toString().equals("||"))
              {
                 Check oldtemp = tempc;
                 tempc = parsePredicate(t);
                 tempc.setChoices(null, oldtemp);
              }
           }

           return tempc;
      }

      parser.reportError("Unknown predicate.", tokens[0].copy(parsePred.toString()));
      return null;   
   }

   public void parseObject(Token data)
   {
      Statement stmt = TokenParser.ParseObject(parser, LexicalAnalyzer.GroupExpressionIndexTokens(parser, new StringIterator(data.toString(), data.getHint())));

      if (parser.hasErrors())
      {
         return;
      }

/*      System.out.println(stmt);

      for (int x = 0; x < stmt.getStrings().length; x++)
      {
          System.out.println(">>> " + stmt.getStrings()[x]);
      } */

      parseObject(stmt);
   }

   public void parseObject(Statement datum)
   {
       Step     atom;

       String[] strings = datum.getStrings(); // was "temp"
       Token[]  tokens  = datum.getTokens();

       Class aClass = null;

       switch (datum.getType())
       {
         case OBJECT_NEW:
           if (tokens.length > 1)
           {
              parseParameters(tokens[1]);
           }
           else
           {
              atom = GeneratedSteps.CreateFrame();
              add(atom, tokens[0]);
           }

           aClass = parser.findImportedClass(strings[0]);

           if (aClass == null)
              parser.reportError("Class " + strings[0] + " was not found", tokens[0]);

           atom    = GeneratedSteps.ObjectNew(aClass);
           add(atom, tokens[0]);
           break;
        case OBJECT_CL_CALL: 
           if (tokens.length > 1)
           {
              parseParameters(tokens[1]);
           }
           else
           {
              atom = GeneratedSteps.CreateFrame();
              add(atom, tokens[0]);
           }

           parseIdea(tokens[0]);

           atom    = GeneratedSteps.ObjectAccess(null);
           add(atom, tokens[0]);
           break;
        case OBJECT_ACCESS:
           if (tokens.length > 2)
           {
              parseParameters(tokens[2]);
           }
           else
           {
              atom = GeneratedSteps.CreateFrame();
              add(atom, tokens[0]);
           }

           parseIdea(tokens[0]);

           atom    = GeneratedSteps.ObjectAccess(strings[1]);
           add(atom, tokens[0]);
           break;
         case OBJECT_ACCESS_S:
           if (tokens.length > 2)
           {
              parseParameters(tokens[2]);
           }
           else
           {
              atom = GeneratedSteps.CreateFrame();
              add(atom, tokens[0]);
           }
 
           aClass = parser.findImportedClass(strings[0]);

           if (aClass == null)
              parser.reportError("Class " + strings[0] + " was not found", tokens[0]);
           
           atom    = GeneratedSteps.ObjectAccessStatic(aClass, strings[1]);
           add(atom, tokens[0]);
           break;
       }
   }
   
   public void parseBlock(Token data)
   {
      /** send the data string through the parser pipeline - errors earlier in the pipeline are assumed to not exist as they would have been found the first time we processed it as a block */
      LinkedList allData = TokenParser.ParseBlocks(parser, LexicalAnalyzer.GroupBlockTokens(parser, new StringIterator(data.toString(), data.getHint())));

      if (parser.hasErrors())
      {
         return;
      }

      parseBlock(allData);
   }

   public void parseBlock(LinkedList data)
   {  
      Iterator i = data.iterator();
      while (i.hasNext())
      {
         parse((Statement)i.next());
      }
   }
  
   public void parseIdea(Token data) 
   {
      LinkedList allData = TokenParser.ParseIdea(parser, LexicalAnalyzer.GroupBlockTokens(parser, new StringIterator(data.toString(), data.getHint())));
      
      if (parser.hasErrors())
      {
         return;
      }

      Iterator i = allData.iterator();
      while (i.hasNext())
      {
         parse((Statement)i.next());
      }
   }

   public void parse(Statement datum)
   {
       Block    a, b;
       String[] scratch;
       Step     atom;
       Scalar   ascalar;

       Iterator i;
       String   mutilate; // mutilate this string as I see fit...
       StringBuffer sb;  

       String[] strings = datum.getStrings(); // was "temp"
       Token[]  tokens  = datum.getTokens();

       switch (datum.getType())
       {
         case IDEA_HASH_PAIR:
           //
           // parsing A => B
           //
           atom = GeneratedSteps.CreateFrame();
           add(atom, tokens[2]);

           //
           // parse B
           //
           parseIdea(tokens[2]);
           atom = GeneratedSteps.Push();
           add(atom, tokens[2]);

           //
           // parse A - or just push it onto the stack as a literal token :)
           //
           ascalar = SleepUtils.getScalar(strings[0]);
           atom    = GeneratedSteps.SValue(ascalar);
           add(atom, tokens[0]);

           atom = GeneratedSteps.Push();
           add(atom, tokens[2]);

           //
           // parse operator
           //
           atom = GeneratedSteps.Operate(strings[1]);
           add(atom, tokens[1]);
           break;
         case IDEA_OPER:
           //
           // parsing A operator B
           //
           atom = GeneratedSteps.CreateFrame();
           add(atom, tokens[2]);

           //
           // parse B
           //
           parseIdea(tokens[2]);
           atom = GeneratedSteps.Push();
           add(atom, tokens[2]);
           //
           // parse A
           //
           parseIdea(tokens[0]);
           atom = GeneratedSteps.Push();
           add(atom, tokens[2]);
           //
           // parse operator
           //
           atom = GeneratedSteps.Operate(strings[1]);
           add(atom, tokens[1]);
           break;
         case IDEA_EXPR_I:
           parseObject(ParserUtilities.extract(tokens[0]));
           break;
         case IDEA_LITERAL: // implemented                   
           sb = new StringBuffer(ParserUtilities.extract(strings[0]));

           for (int x = 0; x < sb.length(); x++)
           {
              if (sb.charAt(x) == '\\' && (x + 1) < sb.length())
              {
                 char tempc = sb.charAt(x + 1);

                 if (tempc == '\'' || tempc == '\\')
                 {
                    sb.deleteCharAt(x);
                 }
              }
           }

           ascalar = SleepUtils.getScalar(sb.toString());
           atom    = GeneratedSteps.SValue(ascalar);
           add(atom, tokens[0]);
           break;
         case IDEA_NUMBER:                         // implemented
           if (strings[0].endsWith("L"))
           {
              ascalar = SleepUtils.getScalar(Long.decode(strings[0].substring(0, strings[0].length() - 1)));
           }
           else
           {
              ascalar = SleepUtils.getScalar(Integer.decode(strings[0]));
           }

           atom    = GeneratedSteps.SValue(ascalar);
           add(atom, tokens[0]);
           break;
         case IDEA_DOUBLE:                         // implemented
           ascalar = SleepUtils.getScalar(Double.parseDouble(strings[0]));
           atom    = GeneratedSteps.SValue(ascalar);
           add(atom, tokens[0]);
           break;
         case IDEA_BOOLEAN:                         // implemented
           ascalar = SleepUtils.getScalar(Boolean.valueOf(strings[0]).booleanValue());
           atom    = GeneratedSteps.SValue(ascalar);
           add(atom, tokens[0]);
           break;
         case VALUE_SCALAR:                       //   implemented
           if (strings[0].equals("$null"))
           {
              ascalar = SleepUtils.getEmptyScalar();
              atom    = GeneratedSteps.SValue(ascalar);
              add(atom, tokens[0]);
           }
           else
           {
              atom = GeneratedSteps.Get(strings[0]);
              add(atom, tokens[0]);
           }
           break;
         case VALUE_INDEXED:

           parseIdea(tokens[0]); // parse the thing we're going to index stuff off of..

           for (int z = 1; z < tokens.length; z++)
           {         
              backup();
              parseIdea(ParserUtilities.extract(tokens[z]));
              atom = GeneratedSteps.Index(strings[0], restore());
              add(atom, tokens[0]);
           }
           break;
         case IDEA_EXPR:                         // implemented
           parseIdea(ParserUtilities.extract(tokens[0]));
           break;
         case EXPR_EVAL_STRING:
         case IDEA_STRING: // implemented -- parsed literals, one of my favorite features in sleep
           int startz = 0; 
           String c = ParserUtilities.extract(strings[0]);
            
           Stack vals, blocks, aligns;
           vals   = new Stack();
           blocks = new Stack();
           aligns = new Stack();
           boolean isVar = false;
           int catpos = -1; // last position of a concatenation..
           for (int x = 0; x < c.length(); x++)
           {
               //
               // check for an escape constant or just to skip over a character
               //
               if (c.charAt(x) == '\\' && (x + 1) < c.length())
               {
                  String lookAhead = ( new Character(c.charAt(x+1)) ).toString();

                  if (escape_constants.containsKey(lookAhead))
                  {
                     String replacedValue = (String)escape_constants.get(lookAhead);
                     c = c.substring(0, x) + replacedValue + c.substring(x + 2, c.length());
                     x += replacedValue.length() - 1;
                  }
                  else  // default behavior is to skip over the character...
                  {
                     c = c.substring(0, x)+c.substring(x+1, c.length());
                     x += 1;
                  }
               }

               //
               // check for the end of our variable...
               //
               if (x < c.length() && isVar && (c.charAt(x) == ' ' || c.charAt(x) == '$'))
               {
                  String varname = c.substring(startz, x);
                   
                  String[] ops = LexicalAnalyzer.CreateTerms(parser, new StringIterator(varname, tokens[0].getHint())).getStrings();
                  String align;
                  if (ops.length == 3)
                  {
                     // ^--- check if our varref has the form $[whatever]varname
                     // in which case we are taking advantage of the align operator inside
                     // parsed literal strings.
                     varname = ops[0] + ops[2];
                     align   = ParserUtilities.extract(ops[1]);

                     if (align.length() > 0)
                     {
                        backup();
                        parseIdea(new Token(align, tokens[0].getHint()));
                        aligns.push(restore());
                     }
                     else
                     {
                        aligns.push(null);
                        parser.reportError("Empty alignment specification for " + varname,  tokens[0]);
                     }
                  }
                  else
                  {
                     aligns.push(null);
                  }

                  backup();
                  parseIdea(new Token(varname, tokens[0].getHint()));
                  blocks.push(restore());

                  startz = x;
                  isVar = false;
               }
            
               //
               // check for the beginning of a new variable
               //
               if (  x < c.length() && c.charAt(x) == '$' && ( x == 0 || c.charAt(x - 1) != '\\' || x == catpos )  ) 
               {
                  //
                  // Check if character is the start of a scalar.  If it is then push the preceding
                  // string onto the stack.  We also make sure the preceding char is not a backslash so
                  // a scalar can be escaped.
                  //
                  if ((x + 3) < c.length() && c.charAt(x+1) == '+' && c.charAt(x+2) == ' ')
                  {
                     if (x > 0)
                     {
                        // we are looking for the $+ operator here, if it is present then this scalar
                        // is meant to just concatenate this string together. 
                        c = c.substring(0, x - 1) + c.substring(x+3, c.length());
                        x = x - 2;
                        catpos = x + 1;
                     }
                     else
                     {
                        parser.reportError("$+ operator found at beginning of string", tokens[0]);
                     }
                  }
                  else
                  {
                     isVar = true;
                     vals.push(c.substring(startz, x));
                     startz = x; 

                     // - give the alignment stuff a little bit of a jumpstart...
                     if ((x + 1) < c.length() && c.charAt(x + 1) == '[')
                     {
                        while (x < c.length() && c.charAt(x) != ']')
                        {
                           x++;
                        }
                     }
                  }
               }
           } 

           if (!isVar)
           {
              vals.push(c.substring(startz, c.length()));
              aligns.push(null);
           }
           else
           {
              String   varname = c.substring(startz, c.length());
              String[] ops     = LexicalAnalyzer.CreateTerms(parser, new StringIterator(varname, tokens[0].getHint())).getStrings();
              String   align;

              if (ops.length == 3)
              {
                 // check if our varref has the form $[whatever]varname
                 // in which case we are taking advantage of the align operator inside
                 // parsed literal strings.
                 varname = ops[0] + ops[2];
                 align   = ParserUtilities.extract(ops[1]);
  
                 if (align.length() > 0)
                 {
                    backup();
                    parseIdea(new Token(align, tokens[0].getHint()));
                    aligns.push(restore());
                 }
                 else
                 {
                    aligns.push(null);
                    parser.reportError("Empty alignment specification for " + varname,  tokens[0]);
                 }
              }
              else
              {
                 aligns.push(null);
              }

              backup();
              parseIdea(new Token(varname, tokens[0].getHint()));
              blocks.push(restore());
           }

           Block[]  tempbl = new Block[blocks.size()];
           Block[]  tempal = new Block[aligns.size()];
           String[] tempst = new String[vals.size()];

           for (int x = 0; x < tempbl.length; x++)
           {
              tempbl[x] = (Block)(blocks.get(x));
           }

           for (int x = 0; x < tempst.length; x++)
           {
              tempst[x] = (String)(vals.get(x));
           }

           for (int x = 0; x < tempal.length; x++)
           {
              tempal[x] = (Block)(aligns.get(x));
           }

           if (datum.getType() == EXPR_EVAL_STRING)
           {
              atom = GeneratedSteps.PLiteral(tempst, tempbl, tempal, "%BACKQUOTE%");
           }
           else
           {
              atom = GeneratedSteps.PLiteral(tempst, tempbl, tempal, null);
           }

           add(atom, tokens[0]);
           break;
         case HACK_INC: // implemented
           mutilate = strings[0].substring(0, strings[0].length() - 2);
           parseBlock(new Token(mutilate + " = " + mutilate + " + 1;", tokens[0].getHint()));
           break;
         case HACK_DEC: // implemented
           //
           // [TRANSFORM]: Reconstructing "+temp[0]+" deccrement hack
           //
           mutilate = strings[0].substring(0, strings[0].length() - 2);
           parseBlock(new Token(mutilate + " = " + mutilate + " - 1;", tokens[0].getHint()));
           break;
         case EXPR_BIND_PRED:
           //
           // [BIND PREDICATE FUNCTION]: "+temp[0]+" "+temp[1]);
           //
           backup();
           parseBlock(tokens[2]);
           atom = GeneratedSteps.BindPredicate(strings[0], parsePredicate(ParserUtilities.extract(tokens[1])), restore());
           add(atom, tokens[0]);
           break; 
         case EXPR_BIND_FILTER:
           //
           // [BIND PREDICATE FUNCTION]: on | EVENT | expression | { code }
           //

           backup();
           parseBlock(tokens[3]);
           b = restore();

           atom = GeneratedSteps.BindFilter(strings[0], strings[1], b, strings[2]);
           add(atom, tokens[0]);
           break; 
         case EXPR_BIND: // implemented
           //
           // [BIND FUNCTION]: "+temp[0]+" "+temp[1]);
           //
           backup();

           if (Checkers.isString(strings[1]) || Checkers.isLiteral(strings[1]))
           {
              parseIdea(tokens[1]);
           }
           else
           {
              parseIdea(new Token("'"+strings[1]+"'", tokens[1].getHint()));
           }

           Block nameBlock = restore();
 
           backup();
           parseBlock(tokens[2]);
           atom = GeneratedSteps.Bind(strings[0], nameBlock, restore());
           add(atom, tokens[0]);
           break; 
         case EXPR_BLOCK:  // implemented
           parseBlock(ParserUtilities.extract(tokens[0]));
           break;
         case IDEA_BLOCK:  // turns our block into a scalar :)
           backup();

           parseBlock(ParserUtilities.extract(tokens[0]));

           atom    = GeneratedSteps.CreateClosure(restore());
           add(atom, tokens[0]);
           break;
         case IDEA_FUNC: // implemented 
           TokenList funcParms = LexicalAnalyzer.CreateTerms(parser, new StringIterator(strings[0], tokens[0].getHint()));

           strings = funcParms.getStrings(); 
           tokens  = funcParms.getTokens();

           if (strings[0].charAt(0) != '&')
           {
              strings[0] = '&' + strings[0];
           }

           if (strings[0].equals("&iff") && tokens.length > 1)
           {
              TokenList terms = ParserUtilities.groupByParameterTerm(parser, ParserUtilities.extract(tokens[1]));

              Token[] termsAr = terms.getTokens();

              if (termsAr.length != 3)
              {
                 parser.reportError("iff(condition, value_t, value_f): invalid form.", tokens[0].copy(strings[0] + strings[1]));
                 break;
              }

              backup();
              parseIdea(termsAr[1]);
              a = restore();

              backup();
              parseIdea(termsAr[2]);
              b = restore();

              atom = GeneratedSteps.Goto(parsePredicate(termsAr[0]), a, b, false);
              add(atom, tokens[0]); 
           }
           else if (tokens.length > 1)
           {
              parseParameters(ParserUtilities.extract(tokens[1]));

              atom = GeneratedSteps.Call(strings[0]);
              add(atom, tokens[0]);
           }
           else
           {
              // retrieve a function literal... 

              atom = GeneratedSteps.Get(strings[0]);
              add(atom, tokens[0]);
           }
           break;
         case EXPR_WHILE:                                        // done
           backup();
           parseBlock(tokens[2]);    
           atom = GeneratedSteps.Goto(parsePredicate(ParserUtilities.extract(tokens[1])), restore(), null, true);
           add(atom, tokens[1]);
           break;
         case EXPR_ASSIGNMENT_T:                                  // implemented
           atom = GeneratedSteps.CreateFrame();
           add(atom, tokens[0]);

           TokenList terms2 = ParserUtilities.groupByParameterTerm(parser, ParserUtilities.extract(tokens[0]));
           Token[] termsAr2 = terms2.getTokens();

           for (int x = 0; x < termsAr2.length; x++)
           {
              parseIdea(termsAr2[x]);
              atom = GeneratedSteps.Push();
              add(atom, termsAr2[x]);
           }

           parseIdea(tokens[2]);

           atom = GeneratedSteps.AssignT();
           add(atom, tokens[0]);
           break;
         case EXPR_ASSIGNMENT:                                  // implemented
           backup();

           parseIdea(tokens[0]);
           atom = GeneratedSteps.Assign(restore());

           parseIdea(tokens[2]);

           add(atom, tokens[2]);
           break;
         case EXPR_IF_ELSE:                                // done
           //
           // if <cond> <block> else -do this again-
           //
           // parse an if-else statement.

           backup();
           parseBlock(tokens[2]);
           a = restore();

           backup();
           String moveon = "";
           if (tokens.length >= 4)
           {
              if (strings[4].equals("if"))
              {
                 parseBlock(ParserUtilities.join(ParserUtilities.get(tokens, 4, tokens.length)));
              }
              else
              {
                 parseBlock(tokens[4]); // get the rest of the arguments after the else clause...  this way we can do really long and big nested if-else statements.
              }
           }
           b = restore();

           atom = GeneratedSteps.Goto(parsePredicate(ParserUtilities.extract(tokens[1])), a, b, false);
           add(atom, tokens[1]); 
           break;
         case EXPR_FOREACH_SPECIAL:
           // |foreach   0
           // |$key      1
           // |=>        2
           // |$value    3
           // |(@temp)   4
           // |{ &printf("hi"); } 5

           backup(); 
           parseIdea(ParserUtilities.extract(tokens[4])); // parse the "source" of the foreach
           a = restore();

           backup();
           parseBlock(ParserUtilities.extract(tokens[5])); // parse the actual block of code to be executed.
           b = restore();

           atom = GeneratedSteps.Foreach(a, strings[1], strings[3], b);

           add(atom, tokens[1]);
           break;
         case EXPR_FOREACH:
           // |foreach
           // |$var
           // |(@temp)
           // |{ &printf("hi"); }

           backup(); 
           parseIdea(ParserUtilities.extract(tokens[2])); // parse the "source" of the foreach
           a = restore();

           backup();
           parseBlock(ParserUtilities.extract(tokens[3])); // parse the actual block of code to be executed.
           b = restore();

           atom = GeneratedSteps.Foreach(a, strings[1], b);

           add(atom, tokens[1]);
           break; 
         case EXPR_FOR:
           // |for
           // |($x = 0; $x < 100; $x++)
           // |{ &printf("hi"); }
           Token extracted_terms[] = ParserUtilities.groupByBlockTerm(parser, ParserUtilities.extract(tokens[1])).getTokens();

           StringBuffer doThis = new StringBuffer();

           TokenList initial_terms = ParserUtilities.groupByParameterTerm(parser, extracted_terms[0]);

           i = initial_terms.getList().iterator();
           while (i.hasNext())
           {
              doThis.append(i.next().toString());
              doThis.append("; ");
           }

           parseBlock(tokens[0].copy(doThis.toString()));

           doThis = new StringBuffer();
           doThis.append(ParserUtilities.extract(strings[2]));

           TokenList final_terms = ParserUtilities.groupByParameterTerm(parser, extracted_terms[2]);

           i = final_terms.getList().iterator();
           while (i.hasNext())
           {
              doThis.append(i.next().toString());
              doThis.append(";\n");
           }

           parseBlock(tokens[0].copy("while ("+extracted_terms[1].toString()+")\n{\n" + doThis.toString() + "}"));
           break;
         case OBJECT_IMPORT:
           parser.importPackage(strings[0]);
           break;           
         case EXPR_BREAK:
           atom = GeneratedSteps.Break();
           add(atom, tokens[0]);
           break;
         case EXPR_RETURN:                     // implemented
           if (strings[0].equals("done"))
           {
              parseIdea(tokens[0].copy("1"));  // in jIRC speak this means just plain old return
           }
           else if (strings[0].equals("halt"))
           {
              parseIdea(tokens[0].copy("2"));  // 2 in jIRC speak means halt the event processing...
           }
           else if (tokens.length >= 2)
           {
              parseIdea(tokens[1]);
           }
           else
           {
              parseIdea(tokens[0].copy("$null"));
           }

           atom = GeneratedSteps.Return();
           add(atom, tokens[0]);
           break;
         default:
      }     
   }

   public void parseParameters(Token token)
   {
      Step atom = GeneratedSteps.CreateFrame();
      add(atom, token);

      TokenList terms   = ParserUtilities.groupByParameterTerm(parser, token);
      Token[]   termsAr = terms.getTokens();

      for (int x = termsAr.length - 1; x >= 0; x--)
      {
         parseIdea(termsAr[x]);
         atom = GeneratedSteps.Push();
         add(atom, termsAr[x]);
      }
   }
}

