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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import client.view.Panel;
import common.Tweet;

public class TweetListener implements MessageListener {

	private Panel panel;
	
	public TweetListener(Panel panel) {
		this.panel = panel;
	}
	
	@Override
	public void onMessage(Message msg) {
		try {
			Tweet tweet = msg.getBody(Tweet.class);
			String username = msg.getStringProperty("username");
			panel.showTweet(username, tweet);
		} catch (JMSException e) {
			e.printStackTrace();
		}	
	}

}
