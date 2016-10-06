
import java.lang.System;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program always takes two arguments
//


public class CSftp
{
    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;
    
    static Socket socket = new Socket();
    
    static BufferedReader ftpInput;
    static PrintWriter printWriter;

    public static void main(String [] args)
    {
    	
	byte cmdString[] = new byte[MAX_LEN];
	String server = args[0];
	Integer portNumber;

	// Get command line arguments and connected to FTP
	// If the arguments are invalid or there aren't enough of them
        // then exit.

	if (args.length != ARG_CNT) {
	    System.out.print("Usage: cmd ServerAddress ServerPort\n");
	    return;
	}
	
	// If port number is not an integer then exit
	try {
		portNumber = Integer.parseInt(args[1]);
	} catch (NumberFormatException e) {
		// TODO Auto-generated catch block
		System.out.println("Usage: cmd ServerAddress ServerPort\n");
		return;
	}

	try {
		for (int len = 1; len > 0;) {
	    
			if (!socket.isConnected() || socket.isClosed()) {
	    	// Connect to FTP server
	    	try {
	    		socket = new Socket(server, portNumber);
	    	} catch (IOException e) {
	            System.out.println("920 Control connection to " + server + " on port " + portNumber + " failed to open.");
	            return;
	    	} catch (IllegalArgumentException e) {
	            System.out.println("920 Control connection to " + server + " on port " + portNumber + " failed to open.");
	            return;
	    	}
	    	
	        try {
	            ftpInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	            printWriter = new PrintWriter(socket.getOutputStream());
	        } catch (IOException e) {
	            System.out.println("925 Control connection I/O error, closing control connection.");
	    		socket.close();
	    		ftpInput.close();
	            printWriter.close();
	            return;
	        }
	        
	        parseMultipleLineResponse();
			}
	        
			System.out.print("csftp> ");
			len = System.in.read(cmdString);
			if (len <= 0) 
				break;
			// Start processing the command here.
			String[] userInput = parseInput(cmdString);
		
			// Ignore empty string and strings that start with "#"
			if ((userInput[0].equals("") || userInput[0].startsWith("#"))) {
				continue;
			}
			
			
			// quit command
            if (userInput[0].equals("quit")) {
            	
            	if (userInput.length == 1) {
            		
            		if(socket.isConnected()) {
            			socket.close();
            			printWriter.close();
            			ftpInput.close();
            		}
            		
            		System.out.println("Closing connection and client.");
            		break;
            		
            	}
            	
            	else {
            		
                    System.out.println("901 Incorrect number of arguments.");
                    continue;
                    
            	}
            	
            }
            
            // user command
            if (userInput[0].equals("user") && userInput.length == 2 && !socket.isClosed() && socket.isConnected()) {
            	
                String username = "USER " + userInput[1];
                sendCommand(username);
                parseMultipleLineResponse();
                cmdString = new byte[MAX_LEN];
                continue;
                
            } else if (userInput[0].equals("user")) {
            	
                if (userInput.length != 2) {
                    System.out.println("901 Incorrect number of arguments");
                    continue;
                }
                
                if (socket.isClosed() || !socket.isConnected()) {
                    System.out.println("903 Supplied command not expected at this time.");
                    continue;
                }
                

                
            }
            
            // password command only usable if the last response code was 331 specify password
            if (userInput[0].equals("pw") && userInput.length == 2 ) {
                String password = "PASS " + removeNewLine(new String(cmdString, "UTF-8"));
                sendCommand(password);
                parseMultipleLineResponse();
                cmdString = new byte[MAX_LEN];
                continue;
            	
            }
            
		
			System.out.println("900 Invalid command.");
		}
		
		} catch (IOException exception) {
	    	System.err.println("998 Input error while reading commands, terminating.");
		} catch (Exception e) {
       		System.err.println("999 Processing error. " + e.getMessage());
    	}
    }
    
    public static String removeNewLine(String input) {
        return input.split("(\\r)?\\n")[0];
    }
    
    public static void sendCommand(String command) {
        
        printWriter.print(command+"\r\n");
        printWriter.flush();
        System.out.println("--> " + command);
    }

	private static void parseMultipleLineResponse() throws IOException {
		try {
		    String result = ftpInput.readLine();
		    while (!(result.matches("\\d\\d\\d\\s.*"))) {
		        System.out.println(result);			   
		    }
		    System.out.println("<-- " + result);
		} catch (IOException e) {
		    System.out.println("925 Control connection I/O error, closing control connection.");
		        socket.close();
		        ftpInput.close();
		        printWriter.close();
		}
	}
    
    public static String[] parseInput(byte[] cmdString) {
    	String cmd = "";
    	try {
			cmd = new String(cmdString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	cmd = cmd.split("(\\r)?\\n")[0];
    	String[] parsedInput = cmd.split(" ");
    	return parsedInput;
    }
    
}
