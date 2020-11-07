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
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class LoadBalancer {
	
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
			
		if (args.length <  8) {
			System.err.println("usage: listener queue lower_threshold higher_threshold start_timeout check_timeout instance_creation_timeout instance_termination_timeout");
			System.exit(0);
		} 
		
		String listener = args[0];
		String queueName = args[1];
		
		int lower_threshold = Integer.parseInt(args[2]);
		int higher_threshold = Integer.parseInt(args[3]);
		
		int start_timeout = Integer.parseInt(args[4]);
		int check_timeout = Integer.parseInt(args[5]);
		int instance_creation_timeout = Integer.parseInt(args[6]);
		int instance_termination_timeout = Integer.parseInt(args[7]);	
		
		// init
		System.out.println("Initializing load balancer on queue " + queueName);
		Context initialContext = LoadBalancer.getContext();	
		Connection sharedConnection = ((ConnectionFactory) initialContext.lookup("java:comp/DefaultJMSConnectionFactory")).createConnection();
		Session lbSession = sharedConnection.createSession();
		Queue queue = (Queue) initialContext.lookup(queueName);
		QueueBrowser browser = lbSession.createBrowser(queue);
		
		ArrayList<Session> sessions = new ArrayList<Session>();
			
		Session session = sharedConnection.createSession();
		sessions.add(session);
		MessageListener msgListener = null;
		if (listener.equals("ImageStore")) {
			msgListener = new ImageStore(sessions.size() - 1, session);
		} else if (listener.equals("ThumbnailStore")) {
			msgListener = new ThumbnailStore(sessions.size() - 1, session);
		} else {
			System.err.println("usage: listener should be either 'ImageStore' or 'ThumbnailStore'");
			System.exit(0);
		}
		session.createConsumer(queue).setMessageListener(msgListener);
		
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
				if (listener.equals("ImageStore")) {
					msgListener = new ImageStore(sessions.size() - 1, session);
				} else {
					msgListener = new ThumbnailStore(sessions.size() - 1, session);
				}
				session.createConsumer(queue).setMessageListener(msgListener);
				System.out.println("New instance launched");
				Thread.sleep(instance_creation_timeout);
				
			} else if (sessions.size() > 1 && msgCounter <= lower_threshold) {  // it should also be based on the time the threshold is not respected
				// terminate a session with ImageStore as a listener
				System.out.println("Terminating an instance...");
				session = sessions.remove(sessions.size() - 1);
				session.close();  // close the session only after the listener terminates
				System.out.println("Instance terminated");
				Thread.sleep(instance_termination_timeout);
			}
			
			Thread.sleep(check_timeout);
		}
	}
		
}

