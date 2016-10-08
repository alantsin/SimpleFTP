
import java.lang.System;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
			    
			    
			    parseResponse();
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
            
            // user command
            if (userInput[0].equals("user")) {
            	
            	if (userInput.length == 2) {
            		
                    String username = "USER " + userInput[1];
                    sendCommand(username);
                    parseResponse();
                    cmdString = new byte[MAX_LEN];
                    continue;
            	}
            	
            	else {
            		
                    System.out.println("901 Incorrect number of arguments");
                    continue;
            		
            	}
                
            } 
            
            // password command
            if (userInput[0].equals("pw")) {
            	
            	if (userInput.length == 2) {
            		
                String password = "PASS " + userInput[1];
                sendCommand(password);
                parseResponse();
                cmdString = new byte[MAX_LEN];
                continue;
                
            	}
            	
            	else {
            		
                    System.out.println("901 Incorrect number of arguments");
                    continue;
            		
            	}
            	
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
            
            // get command
            if (userInput[0].equals("get")) {
            	
            	if (userInput.length == 2) {
            		
            		sendCommand("TYPE I");
            		String response = parseResponse();
            		
            		if (response.startsWith("200")) {
            			
            			sendCommand("SIZE " + userInput[1]);
            			response = parseResponse();
            			
            			if (response.startsWith("550")) {
            				
                            System.out.println("<-- 910 Access to local file " + userInput[1] + " denied.");
                            continue;
                            
            			}
            			
            			String split[] = response.split(" ");
            			int size = Integer.parseInt(split[1]);
            			
            			sendCommand("PASV");
                		response = parseResponse();
                		
                		if (response.startsWith("227")) {

                		String[] ipAndPort = parseIPAndPort(response);
                		System.out.println("IP: " + ipAndPort[0] + " on Port " + ipAndPort[1]);
                		
                		Socket dataConnection = new Socket(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                		
                		try {
                			
    						if (ftpInput.ready()) {
    							response = ftpInput.readLine();
    							
    							if (response.startsWith("425")) {
    								
    								System.out.println("930 Data transfer connection to " + ipAndPort[0] + 
    												   " on port " + ipAndPort[1] + " failed to open.");
    								continue;
    								
    							}
    							
    						}
    						
    						BufferedInputStream dataInput = new BufferedInputStream(dataConnection.getInputStream());
    						
    						sendCommand("RETR " + userInput[1]);
    						response = parseResponse();
    						
                            if (response.startsWith("450 ")) {
                            	
                                System.out.println("<-- 910 Access to local file " + userInput[1] + " denied.");
                                continue;
                                
                            }
    						
                            byte readInput[] = new byte[size];
                            int read;
                            int offset = 0;
                            
                            while ((read = dataInput.read(readInput, offset, readInput.length - offset)) != -1) {
                            	
                                offset += read;
                                
                                if (readInput.length - offset == 0) {
                                    break;
                                }
                                
                            }
                            
                            try {
                            	
                                File file = new File(userInput[1]);   
                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(readInput);
                                fos.close();
                                
                            } catch (IOException io) {
                                System.out.println("Unable to write into file");
                            }
                            
                            dataConnection.close();
                            dataInput.close();
                            parseResponse();
    						
    					} catch (IOException io) {
    						
    						System.out.println("935 Data transfer connection I/O error, closing data connection.");
                            dataConnection.close();
                            
    	                } catch (IllegalArgumentException i) {
    	                	
    						System.out.println("930 Data transfer connection to " + ipAndPort[0] + 
    								   " on port " + ipAndPort[1] + " failed to open.");

    	                }
                		
                        cmdString = new byte[MAX_LEN];
                        continue;
            			
                		}
            		
            		}
            		
            	}
            	
            	else {
            		
                    System.out.println("901 Incorrect number of arguments.");
                    continue;
            		
            	}
            	
            }
            
            // cd command
            if (userInput[0].equals("cd")) {
            	
            	if (userInput.length == 2) {
            		
            		sendCommand("CWD " + userInput[1]);
            		parseResponse();
                    
                    cmdString = new byte[MAX_LEN];
                    continue;
            	}
            	
            	else {
            		
                    System.out.println("901 Incorrect number of arguments.");
                    continue;
            		
            	}
            	
            }
            
            // dir command
            if (userInput[0].equals("dir")) {
            	
            	if (userInput.length == 1) {
            		
            		sendCommand("PASV");
            		String response = parseResponse();
            		if (response.startsWith("227")) {

            		String[] ipAndPort = parseIPAndPort(response);
            		System.out.println("IP: " + ipAndPort[0] + " on Port " + ipAndPort[1]);
            		
            		Socket dataConnection = new Socket(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            		
            		try {
						
            			if (ftpInput.ready()) {
							response = ftpInput.readLine();
							
							if (response.startsWith("425")) {
								
								System.out.println("930 Data transfer connection to " + ipAndPort[0] + 
												   " on port " + ipAndPort[1] + " failed to open.");
								continue;
								
							}
							
						}
						
						sendCommand("LIST");
						parseResponse();
						
                        BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
                        String line;
                        while((line = dataIn.readLine()) != null){
                            System.out.println(line);
                        }
                        dataConnection.close();
                        dataIn.close();
                        
                        parseResponse();
						
					} catch (IOException io) {
						
						System.out.println("935 Data transfer connection I/O error, closing data connection.");
                        dataConnection.close();
                        
	                } catch (IllegalArgumentException i) {
	                	
						System.out.println("930 Data transfer connection to " + ipAndPort[0] + 
								   " on port " + ipAndPort[1] + " failed to open.");

	                }
            		
                    cmdString = new byte[MAX_LEN];
                    continue;
            		
            		}
            		
            	}
            	
            	else {
            		
                    System.out.println("901 Incorrect number of arguments");
                    continue;
            		
            	}
            
            }
            
            System.out.println("900 Invalid command.");
            
		}
		
		} catch (IOException exception) {
	    	System.err.println("998 Input error while reading commands, terminating.");
	    	return;
		} catch (Exception e) {
       		System.err.println("999 Processing error. " + e.getMessage());
    	}
    }
    
    // Parses the IP address and Port number received when entering passive mode
    private static String[] parseIPAndPort(String response) {
    	
    	String split[] = response.split("\\(");
    	String numbers[] = split[1].split(",");
    	numbers[5] = numbers[5].replaceAll("[^0-9]", "");
    	String ip = numbers[0] + "." + numbers[1] + "." + numbers[2] + "." + numbers[3];
    	String port = Integer.toString(((Integer.parseInt(numbers[4]) * 256) + Integer.parseInt(numbers[5])));
    	String parsedResult[] = { ip, port };
    	return parsedResult;
    }

    
    // Sends the user command to the server and prints what command was sent onto the console
    public static void sendCommand(String command) {
        
        printWriter.print(command+"\r\n");
        printWriter.flush();
        System.out.println("--> " + command);
    }

    // Parses the response from the server
	private static String parseResponse() throws IOException {
		try {
		    String result;
		    while (!(result = ftpInput.readLine()).matches("\\d\\d\\d\\s.*")) {
		        System.out.println(result);	
		    }
		    System.out.println("<-- " + result);
		    return result;
		} catch (IOException e) {
		    System.out.println("925 Control connection I/O error, closing control connection.");
		        try {
					socket.close();
					ftpInput.close();
					printWriter.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		}
		return null;
	}
    
	// Parses the user input. Takes the user input and returns a string array of the inputs
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
