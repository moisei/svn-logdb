package com.dalet.svnstats;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Properties;

/**
 * This sample program is a minimal JDBC application showing
 * JDBC access to Hsql.
 *
 * Instructions for how to run this program are 
 * given in <A HREF=example.html>example.html</A>.
 *
 * Hsql applications can run against Hsql running in an embedded 
 * or a client/server framework. When Hsql runs in an embedded framework,
 * the Hsql application and Hsql run in the same JVM. The application
 * starts up the Hsql engine. When Hsql runs in a client/server framework,
 * the application runs in a different JVM from Hsql. The application only needs
 * to start the client driver, and the connectivity framework provides network connections. 
 * (The server must already be running.)
 *
 * <p>When you run this application, give one of the following arguments:
 *    * embedded (default, if none specified)
 *    * rmijdbcclient (if Hsql is running embedded in the RmiJdbc Server framework)
 *    * sysconnectclient (if Hsql is running embedded in the Cloudconnector framework)
 *
 * @author janet
 */


public class HsqlSimpleApp {
    /* the default framework is embedded*/
    public String framework = "embedded";
    public String driver = "org.hsqldb.jdbcDriver";
    public String protocol = "jdbc:hsqldb:";


    public static void main(String[] args) {

        new HsqlSimpleApp().go(args);

    }

    void go(String args[]) {

        System.out.println("HsqlSimpleApp starting in " + framework + " mode.");
        try {
            
            /*
				       The driver is installed by loading its class.
	    			   In an embedded environment, this will start up Hsql, since it is not already running.
	    			 */
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver.");
			
            /*
	      			  The connection specifies create=true to cause
				       the database to be created. To remove the database,
				       remove the directory HsqlDB and its contents.
				       The directory HsqlDB will be created under
				       the directory that the system property 
				       Hsql.system.home points to, or the current
				       directory if Hsql.system.home is not set.
	
			*/
            String user          = "sa";
            String password      = "";
            String getColumnName = "false";

            Properties props = new Properties();

            props.put("user", user);
            props.put("password", password);
            props.put("jdbc.strict_md", "false");
            props.put("shutdown", "true");
            props.put("jdbc.get_column_name", getColumnName);


            Connection conn = DriverManager.getConnection(protocol + "myDB;create=true", props);
            System.out.println("Connected to and created database HsqlDB");
			
            /*
	      				We could also turn autocommit off by putting
	      				;autocommit=false on the URL.
	    				*/
            conn.setAutoCommit(false);
			
            /*
	      			 Creating a statement lets us issue commands against
	      			 the connection.
	    				*/
            Statement s = conn.createStatement();
			
            /*
	      				We create a table and adding a few rows.
	    				*/
            s.execute("CREATE TABLE Stuff(Id varchar(10), FirstName varchar(30), LastName varchar(30), Department varchar(255))");
            System.out.println("Created table Stuff");
            s.execute("INSERT INTO Stuff VALUES('033776618','Yinnon', 'Haviv', 'CS')");
            System.out.println("Inserted ('033776618','Yinnon', 'Haviv', 'CS')");
            s.execute("INSERT INTO Stuff VALUES('032290835','Enav', 'Weinrebe', 'CS')");
            System.out.println("Inserted ('032290835','Enav', 'Weinrebe', 'CS')");
            s.execute("INSERT INTO Stuff VALUES('030776163','Luba', 'Kogan', 'MATH')");
            System.out.println("Inserted ('030776163','Luba', 'Kogan', 'MATH')");
            s.execute("INSERT INTO Stuff VALUES('056084080','Amir', 'Sapir', 'CS')");
            System.out.println("Inserted ('056084080','Amir', 'Sapir', 'CS')");
            
            /* 
            		here is an example of updating rows in the table
            */

            s.execute("UPDATE Stuff set LastName='Sapir' where ID='030776163'");
            System.out.println("Changing Luba Kogan -> Luba Sapir");

            /*
            		here we query the database
            		1. getting Names of all the stuff according the LastName,First
            		2. getting ID of all the members of the CS department.
            */


            System.out.println("Stuff members:");
            ResultSet rsAllStuff = s.executeQuery("SELECT FirstName, LastName FROM Stuff ORDER BY LastName, FirstName");
            while (rsAllStuff.next())
            {
                System.out.println(rsAllStuff.getString("LastName") + " " + rsAllStuff.getString("FirstName"));
            }
            System.out.println("----------------");
            rsAllStuff.close(); // don't forget close the recordset.
            System.out.println("rsAllStuff RecordSet closed");

            System.out.println("CS Department members:");
            ResultSet rsCSStuff = s.executeQuery("SELECT Id FROM Stuff WHERE Department='CS' ORDER BY Id");
            while (rsCSStuff.next())
            {
                System.out.println(rsCSStuff.getString("Id"));
            }
            System.out.println("----------------");
            rsCSStuff.close(); // don't forget close the recordset.
            System.out.println("rsCSStuff RecordSet closed");
            
            
							/*
								here we are dropping (deleting) the table - this is no a usual thing to do...
							*/

            s.execute("drop table Stuff");
            System.out.println("Dropped table Stuff");
			
            /*
	    			  We release statement resource.
	    				*/
            s.close();
            System.out.println("Closed statement");
			
            /*
	      				We end the transaction and the connection.
	    				*/
            conn.commit();
            conn.close();
            System.out.println("Committed transaction and closed connection");
			
            /*
				      In embedded mode, an application should shut down Hsql.
				      If the application fails to shut down Hsql explicitly, 
				      the Hsql does not perform a checkpoint when the JVM shuts down, which means
				      that the next connection will be slower.
				      Explicitly shutting down Hsql with the URL is preferred.
			  
				      This style of shutdown will always throw an "exception".
			  	   */
            boolean gotSQLExc = false;
            if (framework.equals("embedded")) {
                try {
                    DriverManager.getConnection("jdbc:Hsql:;shutdown=true");
                } catch (SQLException se) {
                    gotSQLExc = true;
                }
                if (!gotSQLExc)
                    System.out.println("Database did not shut down normally");
                else
                    System.out.println("Database shut down normally");
            }
        }
        catch (Throwable e) {
            System.out.println("exception thrown:");

            if (e instanceof SQLException)
                printSQLError((SQLException)e);
            else
                e.printStackTrace();
        }

        System.out.println("HsqlSimpleApp finished");
    }

    static void printSQLError(SQLException e) {
        while (e != null) {
            System.out.println(e.toString());
            e = e.getNextException();
        }
    }

}



