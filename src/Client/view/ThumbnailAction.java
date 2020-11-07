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

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class ThumbnailAction extends AbstractAction {

	JFrame frame;
	URL imgURL;
	
	public ThumbnailAction(JFrame frame, URL imgURL) {
		this.frame = frame;
		this.imgURL = imgURL;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		BufferedImage image = null;
		
		try {  
			image = ImageIO.read(imgURL);
			
			JDialog dialog = new JDialog(frame, "Show image", true);
			JLabel label = new JLabel(new ImageIcon(image));
			dialog.add(label);
			dialog.pack();
			dialog.setVisible(true);
			
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(frame,
					"Cannot download this full image.",
					"Full image error",
					JOptionPane.ERROR_MESSAGE);
		}
		
	}


}
