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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

public class TweetPanel extends JPanel{

	JTextPane tweetText;
	JPanel tweetThumbnails;
	
	public TweetPanel(JFrame frame, String username, String text, ArrayList<BufferedImage> thumbnails, ArrayList<URL> imgsURL) {
	
		Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		setBorder(BorderFactory.createTitledBorder(loweredetched, username));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		tweetText = new JTextPane();
		tweetText.setText(text);
		tweetText.setEditable(false);
		tweetText.setOpaque(false);
		JScrollPane textScroll = new JScrollPane(tweetText);
		textScroll.setMaximumSize(new Dimension(660, 42));
		//textScroll.setBorder(BorderFactory.createEmptyBorder());
		add(textScroll);
		
		if (thumbnails != null && !thumbnails.isEmpty()) {
			tweetThumbnails = new JPanel();
			tweetThumbnails.setLayout(new BoxLayout(tweetThumbnails, BoxLayout.X_AXIS));
			JScrollPane thumnailsScroll = new JScrollPane(tweetThumbnails);
			thumnailsScroll.setMaximumSize(new Dimension(660, 130));
			thumnailsScroll.setMinimumSize(new Dimension(660, 130));
			
			for (int i = 0; i < thumbnails.size(); i++) {
				ThumbnailAction thumbAction = new ThumbnailAction(frame, imgsURL.get(i));
				JButton thumbButton = new JButton(thumbAction);
				ImageIcon thumbIcon = new ImageIcon(thumbnails.get(i));
				thumbButton.setIcon(thumbIcon);
				thumbButton.setBorder(new EmptyBorder(5, 5, 5, 5));
				thumbButton.setContentAreaFilled(false);
				tweetThumbnails.add(thumbButton);
			}
			
			add(thumnailsScroll);
		}
		
	}
	
}
