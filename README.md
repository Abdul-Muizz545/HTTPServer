# HTTPServer
This is a server written in java that responds to HTTP/1.1 GET and HEAD requests from multiple clients at the same time. If the file requested from the GET or HEAD request is not available in the server, then the server will reply with a "404 Not Found" message. If any other request is sent to the server such as PUT or POST, then the server will reply with a "501 Not Implemented" message. The server can reply to requests from real web browsers such as Google Chrome, Firefox, etc. 

How to run the application?
After copying and pasting the HTTPServer.java file into a java IDE such as Eclipse or NetBeans, run the application by passing in a command line argument. The command line argument is the port number for which the server is listening to requests from. Set it to a number such as 9999. After running the application with the argument, the server will wait for a request on that port from the web browser.
Now open a web browser and type localhost:9999 (where localhost is referring to the IP address of the server which is your device and 9999 is the port number). You should see the HTML page displayed on the web browser. If you type something like localhost:9999/1233 in the web browser, then the web browser will display the Not Found page. 



