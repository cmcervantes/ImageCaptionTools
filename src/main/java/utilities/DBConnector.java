package utilities;

import com.sun.rowset.CachedRowSetImpl;

import javax.sql.rowset.CachedRowSet;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DBConnector
{
    private Connection _conn;
    private Statement _stmt;
    private String _connStr;
    private DBType _type;

    /**Creates a new DBConnector for a Sqllite DB
     *
     * @param path The path to the Sqllite DB
     */
    public DBConnector(String path)
    {
        _connStr = "jdbc:sqlite:" + path;
        _type = DBType.SQLITE;
    }

    /**Creates a new DBConnector for a MySQL DB
     *
     * @param host
     * @param user
     * @param password
     * @param name
     */
    public DBConnector(String host, String user,
                       String password, String name)
    {
        _connStr = "jdbc:mysql://" + host + "/" + name +"?";

        //use client side prep statements to reduce
        //communication with the DB until we commit
        _connStr += "useServerPrepStmts=false";
        _connStr += "&user=" + user;
        _connStr += "&password=" + password;

        //add some weird annoying stuff new mysql needs
        _connStr += "&useSSL=false";
        _connStr += "&useUnicode=true";
        _connStr += "&useJDBCCompliantTimezoneShift=true";
        _connStr += "&useLegacyDatetimeCode=false";
        _connStr += "&serverTimezone=America/Chicago";

        _type = DBType.MYSQL;
    }

    /**Queries the table specified in the given query and returns
     * a CachedRowSet object; Note that this function opens and
     * closes a new connection for the query
     *
     * @param query
     * @return
     * @throws Exception
     */
    public CachedRowSet query(String query) throws Exception
    {
        openConn();
        ResultSet rs = _stmt.executeQuery(query);
        CachedRowSetImpl crs = new CachedRowSetImpl();
        crs.populate(rs);
        closeConn();

        if(crs.size() == 0)
            throw new Exception("Query returned no results.");

        return crs;
    }

    /**Updates the table specified in the given query using the
     * given params. Optional arguments numThreads and batchSize
     * specify how many rows should be sent to the database
     * simultaneously; each thread performs a single transaction
     * with batchSize rows - if params.size() is less than batchSize,
     * they're split equally between the rows instead
     *
     * @param query         The query to execute
     * @param params        The set of values to update the table with
     * @param batchSize     The number of rows in each transaction  (params size by default)
     * @param numThreads    The number of threads to run simultaneously (1 by default)
     */
    public void update(String query, Collection<Object[]> params,
                              int batchSize, int numThreads) throws Exception
    {
        if(params.size() < batchSize)
            batchSize = params.size() / numThreads;

        //for the purposes of partitioning our params into threads,
        //create a list
        List<Object[]> paramList = new ArrayList<>(params);

        //create a threadpool
        UpdateThread[] threadPool = new UpdateThread[numThreads];
        int paramIdx = 0;
        for(int i=0; i<threadPool.length; i++){
            int endParamIdx = Math.min(paramIdx + batchSize,
                    paramList.size());
            List<Object[]> paramSubList =
                    paramList.subList(paramIdx, endParamIdx);
            paramIdx = endParamIdx;
            threadPool[i] = new UpdateThread(query, paramSubList);
            threadPool[i].start();
            Logger.log("Started thread (%.2f%% rows working or complete)",
                       100*(double)paramIdx/(double) paramList.size());
        }

        //keep walking through our params until we've gone through
        while(paramIdx < paramList.size()){
            //look for dead threads
            for(int i=0; i<threadPool.length; i++){
                if(!threadPool[i].isAlive()){
                    Logger.log("Thread complete; starting new one (%.2f%% rows working or complete)",
                            100*(double)paramIdx/(double) paramList.size());
                    int endParamIdx =
                            Math.min(paramIdx + batchSize, paramList.size());
                    List<Object[]> paramSubList =
                            paramList.subList(paramIdx, endParamIdx);
                    paramIdx = endParamIdx;
                    threadPool[i] = new UpdateThread(query, paramSubList);
                    threadPool[i].start();
                }
            }
            //Sleep for a tenth of a second before checking for thread life again
            try{Thread.sleep(100);}catch(Exception ex){/*do nothing*/}
        }

        //wait until all the threads have completed
        boolean foundLiveThread = true;
        while(foundLiveThread) {
            foundLiveThread = false;
            for(int i=0; i<threadPool.length; i++)
                foundLiveThread |= threadPool[i].isAlive();
            try{Thread.sleep(100);}catch(Exception ex){/*do nothing*/}
        }
        Logger.log("Threads complete");
    }

    /**Updates the table specified in the given query using the
     * given params. Optional arguments numThreads and batchSize
     * specify how many rows should be sent to the database
     * simultaneously; each thread performs a single transaction
     * with batchSize rows - if params.size() is less than batchSize,
     * they're split equally between the rows instead
     *
     * @param query         The query to execute
     * @param params        The set of values to update the table with
     * @param batchSize     The number of rows in each transaction  (params size by default)
     */
    public void update(String query, Collection<Object[]> params,
                              int batchSize) throws Exception
    {
        update(query, params, batchSize, 1);
    }

    /**Updates the table specified in the given query using the
     * given params. Optional arguments numThreads and batchSize
     * specify how many rows should be sent to the database
     * simultaneously; each thread performs a single transaction
     * with batchSize rows - if params.size() is less than batchSize,
     * they're split equally between the rows instead
     *
     * @param query         The query to execute
     * @param params        The set of values to update the table with
     */
    public void update(String query,
                       Collection<Object[]> params) throws Exception
    {
        update(query, params, params.size());
    }

    /**Executes a single update with the given query to
     * create a table on the initialized connection
     *
     * @param query
     * @throws Exception
     */
    public void createTable(String query) throws Exception
    {
        openConn();
        _stmt.executeUpdate(query);
        _conn.commit();
        closeConn();
    }

    /**Returns the enum for the database type
     *
     * @return
     */
    public DBType getDBType(){return _type;}

    /**Creates a new Sqllite database at the given path
     *
     * @param path
     * @throws Exception
     */
    public static void createDatabase(String path) throws Exception
    {
        //For sqlite, we can simply open and close the connection,
        String connStr = "jdbc:sqlite:" + path;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(connStr);
        conn.close();
    }

    /**Creates a new MySql database at the given host, with the given
     * user, password, and name
     *
     * @param host
     * @param name
     * @param user
     * @param password
     */
    public static void createDatabase(String host, String user,
                            String password, String name) throws Exception
    {
        //for mysql, we have to connect to the host with the user/pw
        //without specifying the name, which we'll then create in a call
        String connStr = "jdbc:mysql://" + host + "?";
        connStr += "user=" + user;
        connStr += "&password=" + password;

        //add some weird annoying stuff new mysql needs
        connStr += "&useSSL=false";
        connStr += "&useUnicode=true";
        connStr += "&useJDBCCompliantTimezoneShift=true";
        connStr += "&useLegacyDatetimeCode=false";
        connStr += "&serverTimezone=America/Chicago";
        Class.forName("com.mysql.cj.jdbc.Driver");

        Connection conn = DriverManager.getConnection(connStr);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE DATABASE " + name);
        stmt.close();
        conn.close();
    }

    /**Opens a connection to the initialized database
     *
     * @throws Exception
     */
    private void openConn() throws Exception
    {
        if(_type == DBType.SQLITE)
            Class.forName("org.sqlite.JDBC");
        else if(_type == DBType.MYSQL)
            Class.forName("com.mysql.cj.jdbc.Driver");

        _conn = DriverManager.getConnection(_connStr);
        _conn.setAutoCommit(false);
        _stmt = _conn.createStatement();
    }

    /**Closes the connection to the initialized database
     *
     * @throws Exception
     */
    private void closeConn() throws Exception
    {
        if(_stmt != null)
            _stmt.close();
        _conn.close();
    }

    /**The Database type, which occasionally effects
     * the structure of queries
     */
    public enum DBType
    {
        SQLITE, MYSQL
    }

    /**Internal UpdateThread class uses the static
     * conn string to execute updates in simultaneous
     * batches
     */
    public class UpdateThread extends Thread
    {
        private String _query;
        private Collection<Object[]> _params;

        /**Initializes a new UpdateThread with a given
         * query and set of values
         *
         * @param query
         * @param params
         * @throws Exception
         */
        public UpdateThread(String query,
            Collection<Object[]> params) throws Exception
        {
            _query = query;
            _params = params;
        }

        /**Executes the UpdateThread, updating all specified
         * rows into the database
         */
        public void run()
        {
            Connection conn;
            Object[] currentParams = {};
            try {
                //initialize the conn
                if(_connStr.contains("sqlite"))
                    Class.forName("org.sqlite.JDBC");
                else if(_connStr.contains("mysql"))
                    Class.forName("com.mysql.cj.jdbc.Driver");

                conn = DriverManager.getConnection(_connStr);
                conn.setAutoCommit(false);
                PreparedStatement prepStmt = conn.prepareStatement(_query);

                //set up the bindings, given our param list
                //(row counting starts at 1)
                for(Object[] p : _params){
                    //store our current params in case this query
                    //fails
                    currentParams = p;

                    //execute the prepared update (columns start at idx 1)
                    for(int i=0; i<p.length; i++)
                        prepStmt.setObject(i+1, p[i]);
                    prepStmt.executeUpdate();
                }

                //commit the update in one large batch
                conn.commit();

                //clear the bindings and close the connection
                prepStmt.clearParameters();
                prepStmt.close();
                conn.close();
            } catch(Exception ex) {
                //if we've encountered an exception, log it and the query
                //that caused it
                Logger.log(_query);
                Logger.log(StringUtil.listToString(currentParams, "|"));
                Logger.log(ex);
            }
        }
    }
}



