package edu.cmu.nhahn.ngramprocess;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.javaml.core.Dataset;

public class VisualCluster {
	
	private static ClusterFinder cluster;

    public static void main( String [] args ) throws InterruptedException, IOException, ClassNotFoundException  {
        JFrame frame = new JFrame();
        frame.add( new JLabel(" Output" ), BorderLayout.NORTH );

        CustomText ta = new CustomText();
        ta.redirectSystemStreams();

        Container content = frame.getContentPane();
        content.add( new JScrollPane( ta ),BorderLayout.CENTER );
        frame.setSize(new Dimension(620, 760));
        frame.setVisible( true );

        JPanel p = new JPanel();
        
		final JTextArea textArea = new JTextArea();
		p.add(textArea, BorderLayout.WEST);
        textArea.setColumns(10);
		
		JButton startButton = new JButton("Query");//The JButton name.
		p.add(startButton, BorderLayout.EAST);//Add the button to the JFrame.
		JButton close = new JButton("Close");
		p.add(close, BorderLayout.EAST);
		close.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
			    System.exit(0);
			}
		});
		
		content.add(p,BorderLayout.SOUTH);

		FileInputStream fileIn = new FileInputStream("cluster.ser");
		ObjectInputStream in = new ObjectInputStream(fileIn);
		cluster = new ClusterFinder((Dataset[]) in.readObject());
		in.close();
		fileIn.close();
		
    	System.out.print("Enter Word to Find Cluster: ");


		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
	        	String s = textArea.getText();

	        	Dataset result = ClusterFinder.findWord(cluster.getResults(),s);
	        	if (result == null) {
	            	System.out.print("Enter Word to Find Cluster: ");
	        	}
	        		
	        	System.out.println(result.classes());
	        	double[] tmp = ClusterFinder.calculateMean(result);
        		System.out.print("Average\t");
	        	for (double speed : tmp) {
	        		System.out.print(speed + "\t");
	        	}
	        	System.out.println();
				
			}
			
		});
        
        
    }
    
}
