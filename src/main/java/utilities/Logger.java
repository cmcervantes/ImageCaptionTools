package utilities;

/**The Logger class contains a set of static
 * functions for writing debug messages.
 *
 * NOTE: Though I'm tempted to reimplement this using JUL or
 *       log4j, this is a simple solution that, for now, fits
 *       my needs of 'writing nicely formatted debug statements
 *       to the console'
 *
 * @author ccervantes
 */
public class Logger
{
    private static long startTS;
    private static boolean verbose = false;
    private static long lastLogTS;
    private static int _statusDelay;

    /**Sets the internal verbose flag to true.
     * When verbose is turned off, logging messages
     * do not appear.
     */
    public static void setVerbose()
    {
        verbose = true;
    }

    public static void setStatusDelay(int statusDelay)
    {
        _statusDelay = statusDelay;
    }

    /**Creates the start timestamp from
     * which all future logging timestamps
     * will be measured
     */
    public static void startClock()
    {
        startTS = System.currentTimeMillis();
    }

    /**Logs a message to the console, using the
     * printf <b>format</b> and <b>args</b> structure
     * NOTE: automatically appends a newline to <b>format</b>
     *
     * @param format - The printf format string
     * @param args	 - The arguments to send to printf
     */
    public static void log(String format, Object... args)
    {
        if(verbose) {
            String formatStr = "[%.2f] " + format + "\n";
            Object[] completeArgs = new Object[args.length+1];
            completeArgs[0] = (System.currentTimeMillis() - startTS)/1000.0;
            for(int i=0; i<args.length; i++)
                completeArgs[i+1] = args[i];
            System.out.printf(formatStr, completeArgs);
        }
    }

    /**Logs a status message to the console, using the printf
     * <b>format</b> and <b>args</b> structure; Status messages
     * differ from normal logs in that they are only printed
     * if <i>_statusDelay</i> seconds have passed since the last
     * status message. <i>_statusDelay</i> can be set with
     * setStatusDelay()
     *
     * @param format    - The printf format string
     * @param args	    - The arguments to send to printf
     */
    public static void logStatus(String format, Object... args)
    {
        if(canLogStatus())
            log(format, args);
    }

    /**Logs a status <b>message</b> to the console. Status messages
     * differ from normal logs in that they are only printed
     * if <i>_statusDelay</i> seconds have passed since the last
     * status message. <i>_statusDelay</i> can be set with
     * setStatusDelay()
     *
     * @param message - The message to log
     */
    public static void logStatus(String message)
    {
        if(canLogStatus())
            Logger.log(message);
    }

    /**Returns whether the logger is allowed to log the
     * status message, depending on how much time has passed
     * since the last status message.
     *
     * NOTE: This also internally sets the lastLogTS
     *       if it's unset or if sufficient time has passed
     *
     * @return  - Whether a new status message should be logged
     */
    private static boolean canLogStatus()
    {
        long currentTS = System.currentTimeMillis();
        if(verbose){
            if(lastLogTS == 0)
                lastLogTS = currentTS;
            if(currentTS >= lastLogTS + _statusDelay * 1000.0){
                lastLogTS = currentTS;
                return true;
            }
        }
        return false;
    }

    /**Logs an exception (<b>ex</b>) and its stack trace
     */
    public static void log(Exception ex)
    {
        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
        ex.printStackTrace();
    }

    /**Logs <b>message</b> to the console
     *
     * @param message - The message to log
     */
    public static void log(String message)
    {
        if(verbose) {
            System.out.printf("[%07.2f] %s\n",
                    (System.currentTimeMillis() - startTS)/1000.0,
                    message);
        }
    }
}