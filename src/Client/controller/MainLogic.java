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

package client.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import client.view.Panel;
import common.ImageToLoad;
import common.LoadedImage;
import common.Tweet;

public class MainLogic {
	
	private static final String tweetsTopic = "jms/instatweet.tweets";  // jms/mygroup.myproject.version.resource.queue
	private static final String fullImagesQueue = "jms/instatweet.full-images";
	private static final String thumbnailsQueue = "jms/instatweet.thumbnails";
	
	private static Queue fullImgs;
	private static Queue thumbnails;
	private static Queue imgsLoaded;
	private static Topic tweets;
	
	private static JMSProducer jmsProducer;
	
	private static String serverIP;
	private static String username;
	private static ArrayList<ImageToLoad> tweetImgs;
	private static ArrayList<LoadedImage> loadedImgs;
	
	private static Context getContext() throws NamingException {
		Properties props = new Properties();
		props.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
		props.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
		props.setProperty("java.naming.provider.url", "iiop://" + serverIP + ":3700");
		return new InitialContext(props);
	}
	
	public static void init(Panel panel, String serverIP, String username, ArrayList<String> followed) throws NamingException {
		
		MainLogic.serverIP = serverIP;
		MainLogic.username = username;
		
		Context initialContext = MainLogic.getContext();			
		JMSContext jmsContext = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createContext();
		jmsContext.setClientID(username);
		
		fullImgs = (Queue) initialContext.lookup(fullImagesQueue);
		thumbnails = (Queue) initialContext.lookup(thumbnailsQueue);
		imgsLoaded = jmsContext.createTemporaryQueue();
		tweets = (Topic) initialContext.lookup(tweetsTopic);
		
		jmsProducer = jmsContext.createProducer();

		tweetImgs = new ArrayList<ImageToLoad>();
		loadedImgs = new ArrayList<LoadedImage>();
		LoadedListener loadedListener = new LoadedListener(loadedImgs);			
		jmsContext.createConsumer(imgsLoaded).setMessageListener(loadedListener);
		
		String msgSelector = new String("username IN (");		
		if (!followed.isEmpty()) {
			for (int i = 0; i < followed.size(); i++) {
				msgSelector = msgSelector.concat("'" + followed.get(i) + "',");
			}
		}
		msgSelector = msgSelector.concat(")");
		TweetListener tweetListener = new TweetListener(panel);
		jmsContext.createDurableConsumer(tweets, username, msgSelector, true).setMessageListener(tweetListener);
		
	}
	
	public static void attachImage(File imgFile) throws IOException {
		
		String filename = imgFile.getName();
		String[] tokens = filename.split("\\.");
		String extension = tokens[tokens.length - 1];
		
		// read the image
        BufferedImage imgBuf = ImageIO.read(imgFile);  // throws IOException
        
        // serialize the image
        ByteArrayOutputStream imgStream = new ByteArrayOutputStream();
        ImageIO.write(imgBuf, extension, imgStream);  // throws IOException
        imgStream.flush();
        byte[] imgMsg = imgStream.toByteArray();
        imgStream.close();
        
	    tweetImgs.add(new ImageToLoad(username, tweetImgs.size(), extension, imgMsg));
	    
	}
	
	public static void sendTweet(String tweetText) throws InterruptedException {
		
		ArrayList<URL> imgsURL = null;
		ArrayList<URL> thumbnailsURL = null;
		
		if (!tweetImgs.isEmpty()) {			
			imgsURL = new ArrayList<URL>();
			thumbnailsURL = new ArrayList<URL>();						
			for (ImageToLoad imgToLoad : tweetImgs) {
				jmsProducer.setJMSReplyTo(imgsLoaded).send(fullImgs, imgToLoad);
				jmsProducer.setJMSReplyTo(imgsLoaded).send(thumbnails, imgToLoad);
			}
			
			// wait for the images to be loaded
			/*
			 * alternatively don't wait and spawn a thread to handle the
			 * sending of the tweet as soon as the last response is returned
			 * 
			 * in this case it might happen that more tweets are pending at 
			 * the same time
			 * 
			 * obviously this one is more complex
			 */
			int nResponses = 2 * tweetImgs.size();
			tweetImgs = new ArrayList<ImageToLoad>();
			synchronized (loadedImgs) {
				while(loadedImgs.size() < nResponses) { 
					loadedImgs.wait();
				}
			}
						
			// sort the images as they were selected by the user
			Collections.sort(loadedImgs, new Comparator<LoadedImage>() {
				@Override
				public int compare(LoadedImage li1, LoadedImage li2) {
					return li1.getId() - li2.getId();
				}
			});
			
			for (int i = 0; i < loadedImgs.size(); i++) {
				LoadedImage loadedImg = loadedImgs.get(i);
				if (!loadedImg.isThumbnail()) {
					imgsURL.add(loadedImg.getURL());
				} else {
					thumbnailsURL.add(loadedImg.getURL());							
				}
			}
			
			loadedImgs.removeAll(loadedImgs);
		}
			
		jmsProducer.setProperty("username", username).send(tweets, new Tweet(tweetText, imgsURL, thumbnailsURL));
	}

}
