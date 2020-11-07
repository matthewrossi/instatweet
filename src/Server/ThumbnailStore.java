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

package server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import common.ImageToLoad;
import common.LoadedImage;
import net.coobird.thumbnailator.Thumbnails;

public class ThumbnailStore implements MessageListener{
	
	private final String storage = "/glassfish-4.1.1/glassfish4/glassfish/domains/domain1/applications/instatweet/thumbnails/";
	private final String baseURL;
	
	private Session session;
	private int idSession;
	
	public ThumbnailStore(int idSession, Session session) throws UnknownHostException {
		this.idSession = idSession;
		this.session = session;
		this.baseURL = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080/instatweet/thumbnails/";
	}
	
	@Override
	public void onMessage(Message msg) {	
		try {			
			
			// read the message		
			ImageToLoad imgToLoad = msg.getBody(ImageToLoad.class);
			Queue replyToQueue = (Queue) msg.getJMSReplyTo();
			String username = imgToLoad.getUsername();
			int id = imgToLoad.getId();
			String extension = imgToLoad.getExtension();
			byte[] imgMsg = imgToLoad.getImage();
			System.out.println("Listener" + idSession + " received: [username: " + username + " | id: " + id + " | extension: " + extension + "]");
			
			// compute the image hash
			//MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			//String hash = new String(messageDigest.digest(imgMsg));
			
			// username has to be forced "safe" at creation
			//String filename = URLEncoder.encode(hash, "UTF-8");
			String filename = "thumb";//
			
			// generate the thumbnail
			File imgFile = new File(storage + username + "/" + filename + ".png");
			imgFile.getParentFile().mkdirs();
			//imgFile.createNewFile();		
			imgFile = File.createTempFile(filename, ".png", imgFile.getParentFile());//
			FileOutputStream out = new FileOutputStream(imgFile);
			InputStream imgStream = new ByteArrayInputStream(imgMsg);
			Thumbnails.of(imgStream)
		        .size(100, 100)
		        .outputFormat("png")//
		        //.toFile(imgFile);
				.toOutputStream(out);
			imgStream.close();
			
			// compute the thumbnail URL
			URL url = new URL(baseURL + username + "/" + imgFile.getName());
			
			LoadedImage loadedImage = new LoadedImage(id, url, true);
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(loadedImage);
			MessageProducer producer = session.createProducer(replyToQueue);
			producer.send(objectMessage);
			producer.close();  // Limit the #producer open simultaneously (100 max) 
			
		} catch (JMSException jms) {
			jms.printStackTrace();  // ReplyTo queue does not exists anymore
		} catch (IOException io) {
			io.printStackTrace();  // Concurrent access to stdout or same file (on test)
		} /*catch (NoSuchAlgorithmException alg) {
			alg.printStackTrace();
		}*/
		
	}
	
}
