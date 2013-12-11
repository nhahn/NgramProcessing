package edu.cmu.nhahn.ngramprocess;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMerge {

	private static Connection end = null;
	
	static final String USER_HOME = System.getProperty("user.home") + "/Desktop";
	static final String DATABASES = USER_HOME + "/nGram_databases";
	public DatabaseMerge() {
		// TODO Auto-generated constructor stub
	}

	public static void dbSetup(){
	    Statement stmt = null;
	    try {
	      stmt = end.createStatement();
	      String sql = "DROP TABLE IF EXISTS WORDS;"; 
	      stmt.executeUpdate(sql);
	      
	      sql = "CREATE TABLE WORDS " +
	                   "(ID INTEGER PRIMARY KEY     AUTOINCREMENT," +
	                   " WORD           TEXT    NOT NULL, " + 
	                   " FREQUENCY      BLOB    NOT NULL, " + 
	                   " MAX        	FLOAT		NOT NULL)"; 
	      stmt.executeUpdate(sql);
	      stmt.close();
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    System.out.println("Table created successfully");
	}
	
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub

		Class.forName("org.sqlite.JDBC");
		end = DriverManager.getConnection("jdbc:sqlite:"+USER_HOME+"/test.db");
		dbSetup();
		
		File path = new File(DATABASES);
		File[] files = path.listFiles();
		
		for(File f : files)
		{
			if(!f.getName().startsWith("googlebooks"))
				continue;
			
	        Statement stmt = end.createStatement();
	        String sql = "attach '" + DATABASES + "/" + f.getName() + "' as toMerge;";
	        stmt.execute(sql);
	        sql = "insert into WORDS select NULL, WORD, FREQUENCY, MAX from toMerge.WORDS;";
	        stmt.execute(sql);
	        sql = "detach database toMerge;";
	        stmt.execute(sql);
	        stmt.close();
	        System.out.println("Added " + f.getName() + " to the end database");
		}
		end.close();
		
	}

}
