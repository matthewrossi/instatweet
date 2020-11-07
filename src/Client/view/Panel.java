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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.naming.NamingException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import client.controller.MainLogic;
import common.Tweet;

public class Panel extends JPanel implements ActionListener{
	
	private String serverIP;
	
	private JFrame frame;
	private JLabel tweetImgs;
	private JPanel tweetsViewer;
	private JTextPane tweetText;
	private JFileChooser fileChooser;
	private JButton attachImg, sendTweet;
	
	public Panel(JFrame frame, String serverIP, String username, ArrayList<String> followed) {

		this.frame = frame;
		this.serverIP = serverIP;
		
		if (username == null) {
			setBorder(new EmptyBorder(5, 5, 5, 5));
			setLayout(null);
	
			JLabel lblUsage = new JLabel("usage: username usernames-followed");
			lblUsage.setBounds(10, 11, 380, 25);
			add(lblUsage);
			
		} else {
		
			setBorder(new EmptyBorder(5, 5, 5, 5));
			setLayout(null);
			
			JLabel lblUsername = new JLabel("Username: "+username);
			lblUsername.setBounds(10, 16, 172, 25);
			add(lblUsername);
			
			JLabel label = new JLabel("Follows:");
			label.setBounds(195, 16, 48, 25);
			add(label);			
			JLabel followedUsers = new JLabel();			
			JScrollPane followedUsersPane = new JScrollPane(followedUsers, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			followedUsersPane.setBounds(249, 11, 425, 40);
			add(followedUsersPane);
			String followed_users_username = new String();
			for (int i = 0; i < followed.size(); i++) {
				followed_users_username += followed.get(i) + ", ";
			}
			followedUsers.setText(followed_users_username);
			
			tweetsViewer = new JPanel();
			tweetsViewer.setLayout(new BoxLayout(tweetsViewer, BoxLayout.Y_AXIS));
			JScrollPane tweetsScroll = new JScrollPane(tweetsViewer);
			tweetsScroll.setBounds(10, 57, 664, 282);
			add(tweetsScroll);
			
			tweetText = new JTextPane();
			JScrollPane textScroll = new JScrollPane(tweetText);
			textScroll.setBounds(10, 348, 524, 56);
			add(textScroll);
			
			tweetImgs = new JLabel();			
			JScrollPane imagesMsgPane = new JScrollPane(tweetImgs, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			imagesMsgPane.setBounds(10, 412, 415, 40);
			add(imagesMsgPane);
			
			fileChooser = new JFileChooser();
			FileFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "png", "gif", "jpeg");
			fileChooser.setFileFilter(filter);
			
			attachImg = new JButton("Add image");
			attachImg.addActionListener(this);			
			attachImg.setBounds(435, 412, 99, 40);
			add(attachImg);
			
			sendTweet = new JButton("Tweet");
			sendTweet.addActionListener(this);
			sendTweet.setFont(new Font("Tahoma", Font.PLAIN, 18));
			sendTweet.setActionCommand("New Button");
			sendTweet.setBounds(544, 350, 130, 100);
			add(sendTweet);
			
			try {
				MainLogic.init(this, serverIP, username, followed);
			} catch (NamingException e) {				
				JOptionPane.showMessageDialog(Panel.this,
						"A connection with server reasources cannot be enstablished.",
					    "Init error",
					    JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
				
		}
	}
	
	public void showTweet(String username, Tweet tweet) {

		String text = tweet.getText();
		ArrayList<URL> imgsURL = tweet.getImages();
		ArrayList<URL> thumbnailsURL = tweet.getThumbnails();
		
		ArrayList<BufferedImage> thumbnails = null;
		
		try {
			
			if (thumbnailsURL != null && !thumbnailsURL.isEmpty()) {
				
				thumbnails = new ArrayList<BufferedImage>();
				
				// download thumbnails
				for (URL thumbnailURL : thumbnailsURL) {			
					BufferedImage thumb = ImageIO.read(thumbnailURL);
					thumbnails.add(thumb);
				}
				
			}
			
			// create a new tweet panel
			JPanel tweetPanel = new TweetPanel(frame, username, text, thumbnails, imgsURL);
			tweetsViewer.add(tweetPanel);
			tweetPanel.revalidate();
			tweetPanel.repaint();
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(Panel.this,
					"Cannot download thmbnails attached to this tweet.",
				    "Thumbnails error",
				    JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}		
		
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		
		//handle addImage button action
        if (ae.getSource() == attachImg) {
            int returnVal = fileChooser.showOpenDialog(Panel.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	File file = fileChooser.getSelectedFile();  
            	try {
            		MainLogic.attachImage(file);
            		tweetImgs.setText(tweetImgs.getText() + file.getName() + ", ");
            	} catch (IOException e) {
            		String errorMsg = e.getMessage();
    				if (errorMsg.lastIndexOf("(") > 0 && errorMsg.lastIndexOf(")") > 0){
    					errorMsg = errorMsg.substring(errorMsg.lastIndexOf("(") + 1, errorMsg.lastIndexOf(")"));
    				}
            		JOptionPane.showMessageDialog(Panel.this,
	    					errorMsg,
	    				    "File error",
	    				    JOptionPane.ERROR_MESSAGE);
            	}
            }

        //handle sendMsg button action
        } else if (ae.getSource() == sendTweet) {
        	String text = tweetText.getText();
        	if (text.equals("")) {
        		JOptionPane.showMessageDialog(Panel.this,
						"A tweet should contain some text.",
					    "Tweet error",
					    JOptionPane.ERROR_MESSAGE);
        	} else {
	        	try {
	        		MainLogic.sendTweet(text);
	        		tweetText.setText("");
	        		tweetImgs.setText("");
	        		JOptionPane.showMessageDialog(Panel.this,
	        				"Tweet sent.");
	        	} catch (InterruptedException e) {
	        		JOptionPane.showMessageDialog(Panel.this,
							"The loading of the attached image encountered a problem.",
						    "Loading error",
						    JOptionPane.ERROR_MESSAGE);
	        		System.exit(0);
	        	}
        	}
        }  
		
	}

}
