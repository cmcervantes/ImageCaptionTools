package utilities;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;
import net.sourceforge.argparse4j.internal.HelpScreenException;
import net.sourceforge.argparse4j.internal.UnrecognizedArgumentException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**The ArgParser class functions as a wrapper for
 * various common argparse functions and exists
 * primarily for convenience; directly using
 * the Argparse objects is much more flexible,
 * but is messier as a result
 *
 * @@author ccervantes
 */
public class ArgParser
{
    private ArgumentParser _parser;
    private Subparsers _subparsers;
    private Map<String, Subparser> _subparserDict;
    private Namespace _namespace;

    /**Default ArgParser requires a project name and description
     *
     * @param projectName Name of this project
     * @param description Description of this project
     */
    public ArgParser(String projectName, String description)
    {
        _parser = ArgumentParsers.newArgumentParser(projectName);
        _parser.defaultHelp(true);
        _parser.description(description);
        _subparsers = null;
        _subparserDict = new HashMap<>();
        _namespace = null;
    }

    /**Returns an argument to the specified subparser (null if
     * adding to the main parser); returns null if the add fails
     *
     * @param name      Name of the argument to add
     * @param subparser Subparser to which to add the argument
     *                  (null if main parser)
     * @return          An Argument object or null if no
     *                  subparser was found
     */
    private Argument addArgument(String name, String subparser)
    {
        Argument arg = null;
        if(subparser == null)
            arg = _parser.addArgument(name);
        else if(_subparserDict.containsKey(subparser))
            arg = _subparserDict.get(subparser).addArgument(name);
        else
            Logger.log(new Exception("Specified subparser (" + subparser + ") not found"));
        return arg;
    }

    /**Adds a subparser with the given name to the parser
     *
     * @param subparserName The subparser name
     */
    public void addSubParser(String subparserName)
    {
        if(_subparsers == null)
            _subparsers = _parser.addSubparsers();
        _subparserDict.put(subparserName, _subparsers.addParser(subparserName));
    }

    /**Sets up a flag argument, which stores true if specified
     *
     * @param name Argument name
     * @param help Help text
     */
    public void setArgument_flag(String name, String help)
    {
        setArgument_flag(name, help, null);
    }

    /**Sets up a flag argument, which stores true if specified
     * Optional argument subparser specifies which subparser to
     * add the flag argument to
     *
     * @param name          Argument name
     * @param help          Help text
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument_flag(String name, String help, String subparser)
    {
        Argument arg = addArgument(name, subparser);
        if(arg != null){
            arg.help(help);
            arg.action(Arguments.storeTrue());
        }
    }


    /**Sets up an options argument, which requires one of
     * the given opts to be chosen
     *
     * @param name          Argument name
     * @param opts          Argument options
     * @param defaultVal    Default option
     * @param help          Help text
     */
    public void setArgument_opts(String name, String[] opts, String defaultVal, String help)
    {
        setArgument_opts(name, opts, defaultVal, help, null);
    }

    /**Sets up an options argument, which requires one of
     * the given opts to be chosen
     *
     * @param name          Argument name
     * @param opts          Argument options
     * @param defaultVal    Default option
     * @param help          Help text
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument_opts(String name, String[] opts, String defaultVal, String help, String subparser)
    {
        Argument arg = addArgument(name, subparser);
        if(arg != null){
            arg.choices(Arrays.asList(opts));
            if(defaultVal != null)
                arg.setDefault(defaultVal);
            arg.help(help);
        }
    }

    /**Adds a new argument with the specified options
     *
     * @param name          Argument name
     * @param help          Help text
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument(String name, String help, String subparser)
    {
        setArgument(name, help, null, null, null, null, subparser);
    }

    /**Adds a new argument with the specified options
     *
     * @param name          Argument name
     * @param help          Help text
     * @param type          Argument type as Class
     * @param defaultVal    Default argument value
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument(String name, String help, Class type, Object defaultVal, String subparser)
    {
        setArgument(name, help, type, defaultVal, null, null, subparser);
    }

    /**Adds a new argument with the specified options
     *
     * @param name          Argument name
     * @param help          Help text
     * @param metavar       Meta-variable string in help
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument(String name, String help, String metavar, String subparser)
    {
        setArgument(name, help, null, null, metavar, null, subparser);
    }

    /**Adds a new argument with the specified options
     *
     * @param name          Argument name
     * @param help          Help text
     * @param metavar       Meta-variable string in help
     * @param required      Whether this option is required
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument(String name, String help, String metavar, boolean required, String subparser)
    {
        setArgument(name, help, null, null, metavar, required, subparser);
    }

    /**Adds a new argument with the specified options
     *
     * @param name          Argument name
     * @param help          Help text
     * @param type          Argument type as Class
     * @param defaultVal    Default argument value
     * @param metavar       Meta-variable string in help
     * @param required      Whether this option is required
     * @param subparser     Subparser to which to add the argument
     *                      (null if adding to main parser)
     */
    public void setArgument(String name, String help, Class type, Object defaultVal, String metavar, Boolean required, String subparser)
    {
        Argument arg = addArgument(name, subparser);
        if(arg != null){
            arg.help(help);
            if(metavar != null)
                arg.metavar(metavar);
            if(defaultVal != null)
                arg.setDefault(defaultVal);
            if(type != null)
                arg.type(type);
            if(required != null)
                arg.required(required);
        }
    }

    /**Parses the given arguments for retrieval; exits
     * on failed argparse
     *
     * @param args  Arguments to aprse
     */
    public void parseArgs(String[] args)
    {
        try{
            _namespace = _parser.parseArgs(args);
        } catch(ArgumentParserException apEx) {
            //Only print help if the exception wasn't a help
            //exception (I guess those are printed
            //automatically)
            if(!apEx.getClass().equals(HelpScreenException.class)) {
                _parser.printHelp();
                if(apEx.getClass().equals(UnrecognizedArgumentException.class)) {
                    UnrecognizedArgumentException unArgEx =
                            (UnrecognizedArgumentException)apEx;
                    System.out.println("\n***ERROR*** Unrecognized argument: " +
                            unArgEx.getArgument());
                }
                else if(apEx.getMessage().contains("ambiguous option") ||
                        apEx.getMessage().contains("invalid choice") ||
                        apEx.getMessage().contains("is required") ||
                        apEx.getMessage().contains("unrecognized arguments") ||
                        apEx.getMessage().contains("too few arguments"))
                {
                    System.out.println("\n***ERROR*** " + apEx.getMessage());
                } else {
                    System.out.println(apEx.getMessage());
                    apEx.printStackTrace();
                }
            }
            System.exit(0);
        }
    }

    /**Returns the parsed string value of the given argName
     *
     * @param argName Argument from which to retrieve the value
     * @return        Argument's string value
     */
    public String getString(String argName)
    {
        return _namespace.getString(argName);
    }

    /**Returns the parsed boolean value of the given argName
     *
     * @param argName Argument from which to retrieve the value
     * @return        Argument's boolean value
     */
    public Boolean getBoolean(String argName)
    {
        return _namespace.getBoolean(argName);
    }

    /**Returns the parsed integer value of the given argName
     *
     * @param argName Argument from which to retrieve the value
     * @return        Argument's int value
     */
    public Integer getInt(String argName)
    {
        return _namespace.getInt(argName);
    }

    /**Returns the parsed double value of the given argName
     *
     * @param argName Argument from which to retrieve the value
     * @return        Argument's double value
     */
    public Double getDouble(String argName)
    {
        return _namespace.getDouble(argName);
    }
}
