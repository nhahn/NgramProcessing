package edu.cmu.nhahn.ngramprocess;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class CustomText extends JTextArea {
		private static final long serialVersionUID = 1L;

		private void updateTextArea(final String text) {
 		final JTextArea me = this;
         SwingUtilities.invokeLater(new Runnable() {
           public void run() {
             me.append(text);
           }
         });
       }

       public void redirectSystemStreams() {
         OutputStream out = new OutputStream() {
           @Override
           public void write(int b) throws IOException {
             updateTextArea(String.valueOf((char) b));
           }

           @Override
           public void write(byte[] b, int off, int len) throws IOException {
             updateTextArea(new String(b, off, len));
           }

           @Override
           public void write(byte[] b) throws IOException {
             write(b, 0, b.length);
           }
         };

         System.setOut(new PrintStream(out, true));
         System.setErr(new PrintStream(out, true));
       }
 }
