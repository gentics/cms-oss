package com.gentics.node.tests.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import com.gentics.testutils.http.IHttpServerConnectionHandler;

public class BlockingHttpConnectionHandler implements IHttpServerConnectionHandler {

	private long nMilisecondsToWait = 0;

	/**
	 * Create a new blocking http connection handler that will block the connection for the given amount of time.
	 * 
	 * @param nMilisecondsToWait
	 */
	public BlockingHttpConnectionHandler(long nMilisecondsToWait) {
		this.nMilisecondsToWait = nMilisecondsToWait;
	}

	/**
	 * Set the amount of miliseconds which the connection should be stalled
	 * 
	 * @param time
	 */
	public void setBlockingTime(long nMilisecondsToWait) {
		this.nMilisecondsToWait = nMilisecondsToWait;
	}

	public void handleConnection(Socket client) throws IOException {

		InputStream ins = client.getInputStream();
		OutputStream out = client.getOutputStream();
		
		System.out.println("Handling Connection - Starting Block");
		try {
			Thread.sleep(nMilisecondsToWait);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
		System.out.println("Handling Connection - Blocking finished");

		Scanner in = new Scanner(ins);
		PrintWriter output = new PrintWriter(out, true);

		String cmd = in.nextLine();

		output.println("HTTP/1.0 200 OK");
		output.println("");
		output.println("Here you go: " + cmd);
		
		out.close();
		ins.close();
	}

}

