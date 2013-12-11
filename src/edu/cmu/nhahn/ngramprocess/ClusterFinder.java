package edu.cmu.nhahn.ngramprocess;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.javaml.clustering.KMedoids;
import net.sf.javaml.clustering.evaluation.SumOfCentroidSimilarities;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.PearsonCorrelationCoefficient;

public class ClusterFinder {

	private Dataset s = new DefaultDataset();
	private Dataset[] results;
	private Connection c = null;
	private String[] words = null;
	private int num = 10000;
	private int iterations = 40;
	private int clusters = 1000;
	private static ClusterFinder cluster = null;
	
	public ClusterFinder(Connection c, int num, int iterations, int clusters) {
		// TODO Auto-generated constructor stub
		this.c = c;
		this.num = num;
		this.iterations = iterations;
		this.clusters = clusters;
		words = new String[num];
		
	}
	
	public ClusterFinder(Dataset[] data)
	{
		this.results = data;
	}
	
	public Dataset[] getResults()
	{
		return results;
	}
	
	public void importData() throws IOException, SQLException, ClassNotFoundException
	{
		PreparedStatement stmt = c.prepareStatement("SELECT * FROM (SELECT * FROM WORDS ORDER BY MAX DESC LIMIT ?) ORDER BY WORD ASC");
		stmt.setInt(1, num);
		ResultSet rs = stmt.executeQuery();
		System.out.println("Importing Data");
		int i = 0;
		while ( rs.next() ) {
			double [] counts = (double[]) new ObjectInputStream(new ByteArrayInputStream(rs.getBytes("frequency"))).readObject();
			String word = rs.getString("word");
			Instance inst = new DenseInstance(counts, word);
			s.add(inst);
			words[i] = word;
			i++;
		}
		rs.close();
		stmt.close();
		c.close();
		System.out.println("Finished import");

	}
	
	public void runCluster()
	{
		System.out.println("Clustering");
		KMedoids cluster = new KMedoids(clusters,iterations,new PearsonCorrelationCoefficient());
		results = cluster.cluster(s);
		
	}
	
	public static Dataset findWord(Dataset[] clusters, String word)
	{
		System.out.println("Looking up Word");
		
		for(Dataset data : clusters)
		{
			if(data.classes().contains(word))
				return data;
		}
		return null;
	}
	
	
	
	public void compareClusters(int numCompare)
	{
		Dataset[][] tmp = new Dataset[numCompare][];
		
		for(int i = 0; i < numCompare; i++)
		{
			System.out.print("Run number "+ (i+1) + " --- ");
			runCluster();
			tmp[i] = this.results;
		}
		
		System.out.print("Comparing scores from runs\n");
		SumOfCentroidSimilarities comp = new SumOfCentroidSimilarities();
		HashMap<Dataset[], Double> scores = new HashMap<Dataset[], Double>();
		
		for (Dataset[] data : tmp)
		{
			scores.put(data,comp.score(data));
		}
		
		
		Iterator<Dataset[]> it = scores.keySet().iterator();
		Dataset[] max = it.next();
		while(it.hasNext())
		{
			Dataset[] next =  it.next();
			if(comp.compareScore(scores.get(max),scores.get(next)))
				max = next;
		}
		
		this.results = max;
	}
	
	public static double[] calculateMean(Dataset d) {
		double[] retVal = new double[d.get(0).keySet().size()];
	
		for(Instance i : d)
		{ 
			System.out.print(i.classValue() + "\t");
			int count = 0;
			for (double j : i)
			{
				System.out.print(j + "\t");
				retVal[count] = j;
				count ++;
			}
			System.out.println();
		}
		for(int i = 0; i < retVal.length; i++)
		{
			retVal[i] = retVal[i]/d.size();
		}
		
		return retVal;
	}

    
	
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		// TODO Auto-generated method stub
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
			Class.forName("org.sqlite.JDBC");
			Connection c = DriverManager.getConnection("jdbc:sqlite:"+DatabaseMerge.USER_HOME+"/test.db");

			System.out.print("Number of Words to Search: ");
			int words = Integer.parseInt(br.readLine());
			System.out.print("Number of Iterations: ");
			int iterations = Integer.parseInt(br.readLine());
			System.out.print("Number of Clusters: ");
			int clusters = Integer.parseInt(br.readLine());

			cluster = new ClusterFinder(c,words,iterations,clusters);
			
			cluster.importData();
			cluster.compareClusters(10);

			try
			{
				FileOutputStream fileOut =
						new FileOutputStream(DatabaseMerge.USER_HOME + "/cluster.ser");
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(cluster.results);
				out.close();
				fileOut.close();
				System.out.printf("Serialized data is saved in /tmp/employee.ser");
			}catch(IOException i)
			{
				i.printStackTrace();
			}

		
        System.out.print("Enter Word to Find Cluster: ");
        String s = br.readLine();
		
        while(!s.equals(""))
        {
        	Dataset result = findWord(cluster.results,s);
        	if (result == null) {
            	System.out.print("Enter Word to Find Cluster: ");
            	s = br.readLine();
            	continue;
        	}
        		
        	System.out.println(result.classes());
        	for (double speed : calculateMean(result)) {
        		System.out.print(speed + "\n");
        	}
        	System.out.println();

        	System.out.print("Enter Word to Find Cluster: ");
        	s = br.readLine();
        }


	}

}
