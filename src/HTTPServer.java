import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

public class HTTPServer{

	static int buff_size = 4096*1000;

	public static void main(String[] args) throws Exception {
		int port; //port number
		SocketChannel client = null;
		String fileContents = ""; //this will store the index.html file contents in a string
		String requestMsg = ""; //this will be the request message contents
		String responseMsg = ""; //this will be the response message contents
		RandomAccessFile f1 = null; //this will store the index.html file 

		if (args.length == 1) {
			ByteBuffer buffer = ByteBuffer.allocate(buff_size); //creating empty byte buffer with buff_size capacity
			try {  
				port =  Integer.parseInt(args[0]); //this will be the port number (which you get from command line (in this case 9999))
				
				f1 = new RandomAccessFile("index.html", "r");
				FileChannel fileChannel1 = f1.getChannel(); //getting the file channel
				
				//-------------------------------------------------------
				//WRITING index.html contents to server 
				buffer.clear();
				//writing to buffer from file channel
				while (fileChannel1.read(buffer) > 0) {
					buffer.flip(); //change buffer from write mode to read from mode
					
					//reading from buffer now
					while (buffer.hasRemaining()) { //as long as buffer is not empty
						fileContents = fileContents + (char)buffer.get(); //add byte at buffer's current position
					}
				
					buffer.clear(); //change back to read mode					
				} 
				int fileSize = (int)f1.length();
				//System.out.println(fileContents);
				
				
				//-----------------------------------------------------------
				Selector selector = Selector.open(); //opens the selector

				ServerSocketChannel serverSocket = ServerSocketChannel.open(); //create server socket channel 
				InetSocketAddress socketAddr = new InetSocketAddress("localhost", port); //create socket address with address (localhost means our machine) and port #

				serverSocket.bind(socketAddr); //bind the socket of the server to the specific address
				serverSocket.configureBlocking(false); //puts channel in non-blocking mode

				int ops = serverSocket.validOps(); //same as SelectionKey.OP_ACCEPT
				SelectionKey selectKey = serverSocket.register(selector, ops, null); 

				System.out.println("----- Server -----");

				while (true) {
					System.out.println("Waiting for select...");
					selector.select(); //selects a set of keys from channels read to do I/O operations
					Set<SelectionKey> selectedKeys = selector.selectedKeys(); //the set of keys selected 
					Iterator<SelectionKey> iter = selectedKeys.iterator(); //making an iterator for the set of keys
					
					//loop through all the elements in the iterator 
					while (iter.hasNext()) {
						SelectionKey key = iter.next(); //return the next selection key in iterator

						if (key.isAcceptable()) { 
							//if new client connection is accepted
							client = serverSocket.accept(); //server channel's socket accepts connection made to it. 
							client.configureBlocking(false);

							System.out.println("Accepted new connection from client: " + client);
							// Add the new connection to the selector
							client.register(selector, SelectionKey.OP_READ);
						} //end of if acceptable

						if (key.isReadable()) { 
							//if key's channel supports read operations
							// Read the data from client channel
							client = (SocketChannel)key.channel();

							if (client != null) {
								//writing to buffer 
								while (client.read(buffer) > 0) {
									buffer.flip(); //change buffer from write mode to read from mode
									requestMsg = new String(); 
									//reading from buffer now
									while (buffer.hasRemaining()) { //as long as buffer is not empty
										requestMsg = requestMsg + (char)buffer.get(); //add byte at buffer's current position
									}

									System.out.println("client sent: " + requestMsg); 	
											
									buffer.clear();
								}	
								
								//System.out.println(requestMsg);
								
								//PARSING THE REQUEST MESSAGE 
								String[] requestLines = requestMsg.split(System.lineSeparator());
								String[] requestLine = requestLines[0].split(" "); //the first line of the message is the request line which we split across the space
								
								String method = requestLine[0]; //this is the method (i.e: GET, HEAD, etc)
								String fileRequested = requestLine[1]; //this will be the path to the index.html file
								String httpVersion = requestLine[2]; //this will be the HTTP version (HTTP/1.1 or HTTP/1.0)
								
								responseMsg = getResponseMsg(method, fileRequested, httpVersion, fileSize, fileContents);
								
								buffer.flip();
								buffer.clear();
								buffer.put(responseMsg.getBytes());
								buffer.flip();
								
								while (buffer.hasRemaining()) 
									client.write(buffer);
								buffer.clear();
								
							} ///end of if (client != null)
							
							//client.close();
						} //end of if key.isReadable()
						
						iter.remove(); 
					} //end of while iter.hasNext
					
				}//end of outer while 
				
			}

			catch(Exception e) {
				System.out.println(e);
			}  		

		} //end of if (args.length == 1) 

		//args.length != 1 (more than 1 argument)
		else {
			System.out.println("Usage: java server.java <port number>");
		}
		
		f1.close();

	}//end of main
	
	
	//method to take the method, filePath and httpVersion of the request and accordingly return a response message
	public static String getResponseMsg(String method, String filePath, String httpVersion, int fileSize, String fileContents) {
		String responseMsg = ""; //this will store the response message
		
		if (method.equals("GET")) {
			if (filePath.equals("/") || filePath.equals("/index.html")) {
				String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
				responseMsg += "HTTP/1.1 200 OK\r\n";
    			//add necessary response headers
    			responseMsg += "Date: " + date + "\r\n";
    			responseMsg += "Server: Myserver/2.3.1 (Win64)\r\n";
				responseMsg += "Accept-Ranges: bytes\r\n";
				
    			//add necessary entity headers
    			responseMsg += "Content-type: text/html; charset=UTF-8\r\n";
    			responseMsg += "Content-length: " + fileSize + "\r\n\r\n";				
    			responseMsg += fileContents;
			}
			else { //resource for GET was not found 
				String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
				responseMsg += "HTTP/1.1 404 Not Found\r\n";
    			//add necessary response headers
    			responseMsg += "Date: " + date + "\r\n";
    			responseMsg += "Server: Myserver/2.3.1 (Win64)\r\n";
				responseMsg += "Accept-Ranges: bytes\r\n";

				//add necessary entity headers
    			responseMsg += "Content-type: text/html; charset=iso-8859-1\r\n";
    			responseMsg += "Content-length: 230\r\n\r\n";				
    			responseMsg += "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + "<html>\n" + "<head>\n"
    					+ "   <title>404 Not Found</title>\n" + "</head>\n" + "<body>\n" + "   <h1>Not Found</h1>\n"
    					+ "   <p>The requested URL was not found on this server.</p>\n" + "</body>\n" + "</html>\r\n";
				
			}
		} //end of if GET 
		else if (method.equals("HEAD")) {
			if (filePath.equals("/") || filePath.equals("/index.html")) {
				String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
				responseMsg += "HTTP/1.1 200 OK\r\n";
    			//add necessary response headers
    			responseMsg += "Date: " + date + "\r\n";
    			responseMsg += "Server: Myserver/2.3.1 (Win64)\r\n";
				responseMsg += "Accept-Ranges: bytes\r\n";
					
    			//add necessary entity headers
    			responseMsg += "Content-type: text/html; charset=UTF-8\r\n";
    			responseMsg += "Content-length: " + fileSize + "\r\n\r\n";	
			} 
			else {
				String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
				responseMsg += "HTTP/1.1 404 Not Found\r\n";
    			//add necessary response headers
    			responseMsg += "Date: " + date + "\r\n";
    			responseMsg += "Server: Myserver/2.3.1 (Win64)\r\n";
				responseMsg += "Accept-Ranges: bytes\r\n";

				//add necessary entity headers
    			responseMsg += "Content-type: text/html; charset=iso-8859-1\r\n";
    			responseMsg += "Content-length: 230\r\n\r\n";				
				
				responseMsg += "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + "<html>\n" + "<head>\n"
    					+ "   <title>404 Not Found</title>\n" + "</head>\n" + "<body>\n" + "   <h1>Not Found</h1>\n"
    					+ "   <p>The requested URL was not found on this server.</p>\n" + "</body>\n" + "</html>\r\n";
			}
		} //end of if HEAD
		

		else { //method is not HEAD and not GET (not implemented)
			String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
			responseMsg += "HTTP/1.1 501 Not Implemented\r\n";
			//add necessary response headers
			responseMsg += "Date: " + date + "\r\n";
			responseMsg += "Server: Myserver/2.3.1 (Win64)\r\n";
			responseMsg += "Accept-Ranges: bytes\r\n";

			//add necessary entity headers
			responseMsg += "Content-type: text/html; charset=iso-8859-1\r\n";
			responseMsg += "Content-length: 230\r\n\r\n";				
			
			responseMsg += "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + "<html>\n" + "<head>\n"
					+ "   <title>404 Not Implemented</title>\n" + "</head>\n" + "<body>\n" + "   <h1>Not Implemented</h1>\n"
					+ "   <p>Method was not implemented.</p>\n" + "</body>\n" + "</html>\r\n";
			
		}
		
		return responseMsg;
	} //end of method
	
}//end of class

