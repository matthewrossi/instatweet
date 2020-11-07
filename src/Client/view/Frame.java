// Copyright (c) 2020 Matthew Rossi
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package client.view;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.ArrayList;

public class Frame extends JFrame{

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Frame frame = new Frame(args);
					frame.setVisible(true);					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Frame(String [] args) {	
		String serverIP = null;
		String username = null;
		ArrayList<String> followed = null;
		setTitle("Instatweet");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		if (args.length <  2) {			
			setBounds(100, 100, 290, 90);			
		} else {		
			serverIP = args[0];  // the server IP should be given by a DNS server
			username = args[1];
			followed = new ArrayList<String>();
			for (int i = 2; i < args.length; i++) {
				followed.add(args[i]);
			}
			setBounds(100, 100, 700, 500);
		}
		contentPane = new Panel(this, serverIP, username, followed);
		setContentPane(contentPane);
	}

}