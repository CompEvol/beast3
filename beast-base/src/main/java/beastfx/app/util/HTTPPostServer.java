package beastfx.app.util;

/*
 * HTTPPOSTServer.java
 * Author: S.Prasanna
 * @version 1.00 
 */


import java.io.*;
import java.net.*;
import java.util.*;

import beast.base.core.Log;
import beast.pkgmgmt.PackageManager;
import beastfx.app.beastfx.BeastFX;




public class HTTPPostServer extends Thread {

	static final String HTML_START = "<html>" + "<title>HTTP POST Server</title>" + "<body id='body'>";

	static final String HTML_END = "</body>" + "</html>";

	String DEV_DIR = "html/";
	String PKG_DIR = "/";

	Socket connectedClient = null;
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;

	HTTPRequestHandler handler = new BeastFX();
	
	public void setHandler(HTTPRequestHandler handler) {
		this.handler = handler;
	}
	

	public HTTPPostServer(Socket client) {
		connectedClient = client;
		PKG_DIR = PackageManager.getPackageUserDir() + "/BeastFX/html/";
	}

	public void run() {

		String currentLine = null, filename = null;

		try {

			Log.trace.println("Client " + connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");

			inFromClient = new BufferedReader(new InputStreamReader(connectedClient.getInputStream()));
			outToClient = new DataOutputStream(connectedClient.getOutputStream());

			currentLine = inFromClient.readLine();
			String headerLine = currentLine;
			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();
			if (httpQueryString.startsWith("/")) {
				httpQueryString = httpQueryString.substring(1);
			}

			Log.trace.println("REQUESTED: " + currentLine);
			Log.trace.println("REQUESTED: " + httpQueryString);
				
			Log.trace.println(currentLine);

			
			if (httpMethod.equals("GET")) {
				Log.trace.println("GET request");

				String client = connectedClient.getInetAddress().getHostAddress();
				// TODO: make sure only access from localhost is processed
				if (!client.equals("127.0.0.1") && !client.equals("localhost")) {
					// The default home page
					String responseString = HTTPPostServer.HTML_START + "<form action=\"http://127.0.0.1:5000\" enctype=\"multipart/form-data\" "
							+ "method=\"post\"> " + "Enter the name of a File <input name=\"file\" type=\"file\"><br> "
							+ "<input value=\"Upload\" type=\"submit\"></form> " + "Upload only text files." + HTTPPostServer.HTML_END;
					sendResponse(200, responseString, false, null);
				} else {
					
					System.err.println("QUERY\n" + httpQueryString);

					InputStream in = null;
					File file = new File(DEV_DIR + httpQueryString);
					if (file == null || !file.exists()) {
						file = new File(PKG_DIR + httpQueryString);
					}
					if (file == null || !file.exists()) {
						if (handler != null) {
							String response = handler.handleRequest(httpQueryString, null);
							sendResponse(200, response, false, in);
						} else {
							sendResponse(404, "<b>Requested resource was not found ...." + "Usage: http://127.0.0.1:5000</b>", false, in);
						}
					} else {
						in = new FileInputStream(file);
					}

					if (in != null) {
						sendResponse(200, httpQueryString, true, in);
						in.close();
					}

				}
			} else { // POST request
				Log.trace.println("POST request");
				do {
					currentLine = inFromClient.readLine();
					Log.trace.println(currentLine);

					//if (currentLine.indexOf("Content-Type: multipart/form-data") != -1) {
					StringBuffer buf = new StringBuffer();
					if (currentLine.length() == 0) {
						currentLine = inFromClient.readLine();
					//}
					//if (currentLine.indexOf("----------------------------")  != -1) {
						String boundary = currentLine;//currentLine.split("boundary=")[1];
						// The POST boundary
						filename = inFromClient.readLine().split("filename=")[1].replaceAll("\"", "");

						String fileContentType = inFromClient.readLine().split(" ")[1];
						Log.trace.println("File content type = " + fileContentType);

						inFromClient.readLine(); // assert(inFromClient.readLine().equals(""))
													// :
													// "Expected line in POST request is "" ";

						// fout = new PrintWriter(filename);
						String prevLine = inFromClient.readLine();
						currentLine = inFromClient.readLine();

						// Here we upload the actual file contents
						while (true) {
							if (currentLine.equals(boundary + "--")) {
								buf.append(prevLine).append('\n');
								break;
							} else {
								buf.append(prevLine).append('\n');
							}
							prevLine = currentLine;
							currentLine = inFromClient.readLine();
						}

						String response = handler.handleRequest(httpQueryString, buf);
						sendResponse(200, response, false, null);
						return;
						// fout.close();
					} // if
				} while (inFromClient.ready()); // End of do-while
				sendResponse(200, "File " + filename + " Uploaded..", false, null);
			}// else
		} catch (Exception e) {
			e.printStackTrace();
			try {
				sendResponse(404, "Error: " + e.getMessage() + "<br>BEASTFX_is_done", false, null);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public void sendResponse(int statusCode, String responseString, boolean isFile, InputStream in) throws Exception {

		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer";
		//String contentLengthLine = null;
		//String fileName = null;
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		//FileInputStream fin = null;

		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

		if (isFile) {
			//fileName = responseString;
			//fin = new FileInputStream(fileName);
			//contentLengthLine = "Content-Length: " + Integer.toString(in.available()) + "\r\n";
			String type_is = "text/html";
			// find out what the filename ends with,
			// so you can construct a the right content type
			if (responseString.endsWith(".zip")) {
				type_is = "application/x-zip-compressed";
			}
			if (responseString.endsWith(".jpg") || responseString.endsWith(".jpeg")) {
				type_is = "image/jpeg";
			}
			if (responseString.endsWith(".gif")) {
				type_is = "image/gif";
			}
			if (responseString.endsWith(".png")) {
				type_is = "image/png";
			}
			if (responseString.endsWith(".ico")) {
				type_is = "image/x-icon";
			}
			if (responseString.endsWith(".css")) {
				type_is = "text/css";
			}
			if (responseString.endsWith(".js")) {
				type_is = "text/javascript";
			}
			contentTypeLine = "Content-Type: " + type_is + "\r\n";
		} else {
			responseString = HTTPPostServer.HTML_START + responseString + HTTPPostServer.HTML_END;
			//contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
		}

		outToClient.writeBytes(statusLine);
		outToClient.writeBytes(serverdetails);
		outToClient.writeBytes(contentTypeLine);
		//outToClient.writeBytes(contentLengthLine);
		//outToClient.writeBytes("Connection: close\r\n");
		outToClient.writeBytes("\r\n");

		if (isFile)
			sendFile(in, outToClient);
		else
			outToClient.writeBytes(responseString);

		outToClient.close();
	}

	public void sendFile(InputStream fin, DataOutputStream out) throws Exception {
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = fin.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		fin.close();
	}

	static int port = 5000;

	public static int startServer(final HTTPRequestHandler handler) throws Exception {

		new Thread() {
			@SuppressWarnings("resource")
			@Override
			public void run() {
				try {
					ServerSocket Server = null;
					for (int i = 0; i < 100; i++) {
						try {
							Server = new ServerSocket(port + i, 10, InetAddress.getByName("127.0.0.1"));
							port += i;
							Log.trace.println("HTTP Server Waiting for client on port " + port);
							break;
						} catch (Throwable t) {
							
						}
					}
	
					while (true) {
						Socket connected = Server.accept();
						HTTPPostServer server = new HTTPPostServer(connected);
						server.handler = handler;
						server.start();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		Thread.sleep(500);
		return port;
	}

	public static void main(String args[]) throws Exception {
		
		startServer(new BeastFX());
	}
}