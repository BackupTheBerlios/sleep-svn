package sleep.parser;

/**
 * <p>This class offers access to modify some settings within the sleep parser.</p>
 * 
 * <h2>Install an Escape Constant</h2>
 * 
 * <p>In sleep a character prefixed by a \ backslash within a "double quoted" string is said to be escaped.  Typically an 
 * escaped character is just skipped over during processing.  It is possible in sleep to add meaning to different 
 * characters by installing an escape.   For example to add the escape \r to mean the new line character one would do the 
 * following:</p>
 * 
 * <code>ParserConfig.installEscapeConstant('m', "MONKEY");</code>
 * 
 * <p>Once the above code is executed the value "blah\m" inside of sleep would be equivalent in java to "blahMONKEY".</p>
 * 
 * <h2>Register a Keyword</h2>
 * 
 * <p>The sleep parser requires that all environment "keywords" be registered before any scripts are parsed.  Bridges
 * that should register their keywords are Environment, PredicateEnvironment, FilterEnvironment, Operator, and Predicate.</p>
 * 
 * @see sleep.interfaces.Environment
 * @see sleep.interfaces.PredicateEnvironment
 * 
 */
public class ParserConfig
{
   /** Installs an escape constant into the sleep parser.  Any time the escape constant escape is encountered inside of a 
       parsed literal with a \ backslash before it, sleep will substitute that string with the value specified here. */
   public static void installEscapeConstant(char escape, String value)
   {
      CodeGenerator.installEscapeConstant(escape, value);
   }

   /** registers "keyword" as a keyword with the parser.  This is a necessity if you have environment bridges in sleep */
   public static void addKeyword(String keyword)
   {
      Checkers.addKeyword(keyword);
   }
}
