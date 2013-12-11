package edu.cmu.nhahn.ngramprocess;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 
public class PreProcess {

	//static final String USER_HOME = System.getProperty("user.home") + "/Desktop/";
	final static String USER_HOME = "H:\\";
	final static String countsPath = USER_HOME + "googlebooks-eng-all-totalcounts-20120701";
	final static String ngramFilesPath = USER_HOME + "ngram";
	final static int startDate = 1840;
	final static int endDate = 1860;
		
	public PreProcess() {
		// TODO Auto-generated constructor stub
	}

	public static void dbSetup(Connection c){
	    Statement stmt = null;
	    try {
	      stmt = c.createStatement();
	      String sql = "DROP TABLE IF EXISTS WORDS;"; 
	      stmt.executeUpdate(sql);
	      
	      sql = "CREATE TABLE WORDS " +
	                   "(ID INTEGER PRIMARY KEY     AUTOINCREMENT," +
	                   " WORD           TEXT    NOT NULL, " + 
	                   " FREQUENCY      BLOB    NOT NULL, " + 
	                   " MAX        	FLOAT		NOT NULL)"; 
	      stmt.executeUpdate(sql);
	      stmt.close();
	      c.commit();
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    System.out.println("Table created successfully");
	}
	
	public static void updateWord(int word, double[] map, double max, Connection c) {
	    PreparedStatement stmt = null;
		
		try {

	        stmt = c.prepareStatement("UPDATE WORDS set FREQUENCY = ?, max = ? where ID= ?;");
	        stmt.setBytes(1, frequencyToBlob(map).toByteArray());
	        stmt.setDouble(2, max);
	        stmt.setInt(3, word);
	        stmt.executeUpdate();
	        c.commit();
	        stmt.close();
	        
	      } catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        System.exit(0);
	      }
	 //     System.out.println("Inserted " + word+ " into the database");
	}
	
	public static void insertWord(String word, double[] map, double max, Connection c) {
	    PreparedStatement stmt = null;
	    try {
	        	        
	        stmt = c.prepareStatement("INSERT INTO WORDS (WORD,FREQUENCY,MAX) VALUES (?,?,?);");
	        stmt.setString(1, word);
	        stmt.setBytes(2, frequencyToBlob(map).toByteArray());
	        stmt.setDouble(3, max);
	        stmt.executeUpdate();
	        stmt.close();
	        c.commit();
	      } catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        System.exit(0);
	      }
	//      System.out.println("Inserted " + word+ " into the database");
	}

	private static ByteArrayOutputStream frequencyToBlob(double[] map)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(map);
		out.close();
		return baos;
	}
	
	private static String lookupWord (String word, String tag)
	{
		
//			HashSet morph = morphy.getBaseForm(word,morphy.lookupPOS(tag));
//			if(morph.isEmpty())
				return word;
//			else
//				return (String) morph.iterator().next();
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {        
		
		FileReader fr;
		int[] tmpCounts = new int[endDate - startDate+1];
		try {
			fr = new FileReader(countsPath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		String line = "";
		BufferedReader bf = new BufferedReader(fr);
		while((line = bf.readLine()) != null) {
			String[] yearGroups = line.split("\t");
			for(String str : yearGroups)
			{
				if (str.matches("^\\s*$"))
					continue;
				String[] yearGroup = str.split(",");
				int year = Integer.parseInt(yearGroup[0].trim());
				if(year >= startDate && year <= endDate)
					tmpCounts[tmpCounts.length - (endDate - year) -1] = Integer.parseInt(yearGroup[1].trim());
			}
		}
		bf.close();
	
		final int[] totalCounts = tmpCounts;
		
		Morphy morphy = new Morphy();
		File path = new File(ngramFilesPath);
		File[] files = path.listFiles();
		final PriorityQueue<File> q = new PriorityQueue<File>();
		q.addAll(Arrays.asList(files));
		
		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(4);
		
		while(q.peek() != null)
		{
			final File item = q.poll();
			Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						processFile(item,totalCounts);
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	
			};
			executor.execute(r);
		}

	}
	
	private static void processFile(File f, int[] totalCounts) throws ClassNotFoundException, SQLException, NumberFormatException, IOException
	{
		String line = "";
		String prevWord = "";
		double maxCount = 0;
		double[] counts = new double[endDate - startDate+1];
		String morphWord = "";
		int existingWord = -1;
		
		if(f.getName().startsWith("."))
			return;
		
		FileReader file = new FileReader(f);
		int lineCount = 0;
		BufferedReader ngramReader = new BufferedReader(file);
		
		Class.forName("org.sqlite.JDBC");
        //Connection c = DriverManager.getConnection("jdbc:sqlite:/Users/nhahn/Desktop/" + f.getName() + ".db");
        Connection c = DriverManager.getConnection("jdbc:sqlite:C:/Users/Nathan/" + f.getName() + ".db");
        c.setAutoCommit(false);
		dbSetup(c);
		
		while((line = ngramReader.readLine()) != null) {
			//Split off the individual pieces and the main word
			lineCount++;
		    String[] splitLine = line.split("\t");
		    
		    //TODO ignore part of speech tags jk they are mutually exclusive
		    //if (splitLine[0].matches("_[A-Z]+_$"))
		    	//continue;
		    
		    String word = splitLine[0].replaceAll("_[A-Z]+$", "").trim().toLowerCase();
		    
		    //Find the tag
		    String[] tags = splitLine[0].split("_");
		    String tag = tags[tags.length-1];
		    
		    //Parse the integer values
		    int date = Integer.parseInt(splitLine[1].trim());
		    double count = Double.parseDouble(splitLine[2].trim());
		    double vol = Double.parseDouble(splitLine[3].trim());
		    
		    if (!prevWord.equals(word))
	    	{
	    		//Save the previous word if it has a large enough max count
	    		addWord(totalCounts, maxCount, counts, morphWord, existingWord,c);
	    		
	    		System.out.println("Working on " + f.getName() +  " : " + lineCount);
	    		//Check for a lemma -- ignoring this for now

	    		//Reset the vars for a new word
	    		counts = new double[endDate - startDate+1];
	    		maxCount = 0;
	    		existingWord = -1;
	    		
	    		//Find the new word with our lookup
	    		morphWord = lookupWord(word, tag);
	    		
	    		//Look in our database for any words words that are similar
	    		PreparedStatement stmt = c.prepareStatement("SELECT * FROM WORDS WHERE WORD=?;");
	    		stmt.setString(1, morphWord);
	    		ResultSet rs = stmt.executeQuery();
	    		if ( rs.next() ) {
	    			maxCount = rs.getDouble("max");
	    			counts = (double[]) new ObjectInputStream(new ByteArrayInputStream(rs.getBytes("frequency"))).readObject();
	    			existingWord = rs.getInt("id");
	    		}
	    		rs.close();
	    		stmt.close();
	    	}
		    
		    if(date >= startDate && date <= endDate)
		    {
			    vol /= totalCounts[counts.length - (endDate - date) -1];
	    		//counts[counts.length - (endDate - date) -1] += (count/totalCounts[counts.length - (endDate - date) -1]);
	    		counts[counts.length - (endDate - date) -1] += count;

	    		if (vol > maxCount)
	    			maxCount = vol;
		    }
    		prevWord = word;
		}
		addWord(totalCounts, maxCount, counts, morphWord, existingWord,c);
		ngramReader.close();
        c.close();
	}

	private static void addWord(int[] totalCounts, double maxCount,
			double[] counts, String morphWord, int existingWord, Connection c) {
		if(!morphWord.isEmpty() && maxCount > (10.0 / totalCounts[counts.length/2]))
			if(existingWord >= 0)
				updateWord(existingWord,counts,maxCount, c);
			else
				insertWord(morphWord,counts,maxCount, c);
	}
	
	

}
