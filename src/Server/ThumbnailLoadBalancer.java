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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ThumbnailLoadBalancer {
	
	private final static String queueName = "jms/instatweet.thumbnails";	
	
	private final static int lower_threshold = 0;
	private final static int higher_threshold = 10;
	
	private final static int start_timeout = 1000;
	private final static int check_timeout = 100;
	private final static int instance_creation_timeout = 0;
	private final static int instance_termination_timeout = 10000;	
	
	private static Context getContext() throws NamingException {
		Properties props = new Properties();
		props.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
		props.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
		props.setProperty("java.naming.provider.url", "iiop://localhost:3700");
		return new InitialContext(props);
	}
	
	/* 
	 * ConncectionFactory.getContext() creates both a connection and a session,
	 * this code instead does it separately.
	 * 
	 * Actually more session can be created on the same connection even with context:
	 * 1. Create a context with ConncectionFactory.getContext()
	 * 2. Create new context that share the same connection with JMSContext.getContext()
	 * 
	 * Still seems to only work on new msgs, but this is far the most optimized way
	 * of doing it.
	 * 
	 * Creating multiple consumers each one with an handler on the same queue would 
	 * not work as we wished.
	 * 
	 * The session used to create the message consumer serializes the execution of all
	 * message listeners registered with the session. At any time, only one of the
	 * session’s message listeners is running. 
	 * 
	*/

	public static void main(String[] args) throws NamingException, InterruptedException, JMSException, UnknownHostException {
		
		// init
		System.out.println("Initializing load balancer on queue " + queueName);
		Context initialContext = ThumbnailLoadBalancer.getContext();	
		Connection sharedConnection = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createConnection();
		Session lbSession = sharedConnection.createSession();
		Queue queue = (Queue) initialContext.lookup(queueName);
		QueueBrowser browser = lbSession.createBrowser(queue);
		
		ArrayList<Session> sessions = new ArrayList<Session>();
		int idSession = 0;  // TODO: use sessions size instead
			
		Session session = sharedConnection.createSession();
		sessions.add(session);
		ThumbnailStore thumbnailStore = new ThumbnailStore(idSession, session);
		session.createConsumer(queue).setMessageListener(thumbnailStore);
		idSession++;
		
		System.out.println("Starting connection...");
		sharedConnection.start();
		System.out.println("Connection started");
		
		Thread.sleep(start_timeout);
		
		System.out.println("Checking for load issue...");
		
		// handle load balancing
		Enumeration<?> enumeration = null;
		int msgCounter;
		
		while (true) {		
			enumeration = browser.getEnumeration();
			msgCounter = 0;
			while (enumeration.hasMoreElements()) {
				enumeration.nextElement();
				msgCounter++;
			}
			
			if (msgCounter >= higher_threshold) {  // it should also be based on the time the threshold is not respected
				// create a new session with ImageStore as a listener
				System.out.println("Launching new instance...");
				session = sharedConnection.createSession();
				sessions.add(session);
				thumbnailStore = new ThumbnailStore(idSession, session);
				session.createConsumer(queue).setMessageListener(thumbnailStore);
				idSession++;
				System.out.println("New instance launched");
				Thread.sleep(instance_creation_timeout);
				
			} else if (sessions.size() > 1 && msgCounter <= lower_threshold) {  // it should also be based on the time the threshold is not respected
				// terminate a session with ImageStore as a listener
				System.out.println("Terminating an instance...");
				session = sessions.remove(idSession - 1);
				session.close();  // close the session only after the listener terminates
				idSession--;
				System.out.println("Instance terminated");
				Thread.sleep(instance_termination_timeout);
			}
			
			Thread.sleep(check_timeout);
			
		}

	}

}
