/**
 * Created by agnaldocunha on 9/26/14.
 */

import java.io.*;
import java.net.*;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
public class Iperfer {

    Socket client;
    ServerSocket server;
    Socket connection;
    DataOutputStream outToServer;
    DataInputStream intoServer;
    long counterBytes;

    public static void main(String [] args) throws Exception {
        //Options for the command line
        Options options = new Options();
        Option optClientMode = new Option("c", false, "Client Mode");
        Option optHost = new Option("h", true, "Server Hostname");
        Option optServerPort = new Option("p", true, "Server Port");
        Option time = new Option("t", true, "Time");
        Option optServerMode = new Option("s", false, "Server Mode");

        options.addOption(optServerPort);
        options.addOption(optHost);
        options.addOption(time);
        options.addOption(optClientMode);
        options.addOption(optServerMode);

        BasicParser bp = new BasicParser();

        try {
            CommandLine cmd = bp.parse(options, args);
            Iperfer iperf = new Iperfer();

            //CLIENT MODE
            if (cmd.hasOption("c") && cmd.hasOption("h") && cmd.hasOption("p") && cmd.hasOption("t")) {
                iperf.runClient(cmd);
            }
            //SERVER MODE
            else if (cmd.hasOption("s") && cmd.hasOption("p")) {
                iperf.runServer(cmd);
            }

            else {
                System.err.println("Error: missing or additional arguments");
            }
        }
        catch (org.apache.commons.cli.ParseException e) {
            e.printStackTrace();
        }
    }

    private void runClient(CommandLine cmd) {
        // connect to server, get streams, process connection
        try {
            connectToServer(cmd); // Step 1: Create a Socket to make connection
            sendData(cmd); // Step 3: Process connection
        }

        // server closed connection
        catch ( EOFException eofException ) {
            System.err.println( "Client terminated connection" );
        }

        // process problems communicating with server
        catch ( IOException ioException ) {
            ioException.printStackTrace();
        }

        finally {
            closeClientConnection(); // Step 4: Close connection
        }

    } // end method runClient

    private void connectToServer(CommandLine cmd) throws IOException {
        try {
            String serverName = cmd.getOptionValue("h");
            InetAddress ipAddress = InetAddress.getByName(serverName);
            int serverPort = Integer.parseInt(cmd.getOptionValue("p"));
            client = new Socket(ipAddress, serverPort);
            System.out.println("Connected to server");
        }
        catch (ConnectException e) {
            System.err.println("Server not set");
        }
    }

    private void sendData(CommandLine cmd) throws IOException {
        byte[] dataToSend = new byte[1024];
        for(int i = 0; i < 1024; i++) {
            dataToSend[i] = 0;
        }
        counterBytes = 0;
        Long timeout = Long.parseLong(cmd.getOptionValue("t"));
        Long starTime = System.currentTimeMillis();
        System.out.println("Start: " + starTime + "   " + timeout);
        while(System.currentTimeMillis() - starTime < timeout) {
            try {
                outToServer = new DataOutputStream(client.getOutputStream());
                outToServer.writeInt(dataToSend.length);
                outToServer.write(dataToSend);
                outToServer.flush();
                counterBytes++;
            }
            catch (NullPointerException e) {
                System.out.println("Connection failed");
                break;
            }

            System.out.println(counterBytes + " " + (System.currentTimeMillis() - starTime));
        }
        System.out.println("Finished: " + (System.currentTimeMillis()-starTime));
        double sent = counterBytes*1025/100;
        System.out.println("sent=" + sent + "kB rate=" + sent/timeout*0.008 + " Mbps");
    }
    private void closeClientConnection ()  {
        try {
            outToServer.close();
            client.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            System.err.println("Server is not set");
        }
    }


    private void runServer(CommandLine cmd) throws InterruptedException {
        try {
            //Step 1: Create a ServerSocket
            int port = Integer.parseInt(cmd.getOptionValue("p"));
            server = new ServerSocket(port);
            while (true) {
                try {
                    waitForConnection(); //Step 2: Wait for Connection
                    processConnection(); //Step 4: Process connection
                }

                //Process EOFException when client closes connection
                catch (EOFException eofException) {
                    System.err.println("Server terminated connection");
                }
            }
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void processConnection() throws IOException {
        int length = intoServer.readInt();
        if(length > 0) {
            byte[] message = new byte[length];
            intoServer.readFully(message, 0, message.length);
            for(int i = 0; i < length; i++) {
                //System.out.println(message[i] + " YO");
            }
        }
    }
    private void waitForConnection () throws IOException {
        connection = server.accept();
        intoServer = new DataInputStream(connection.getInputStream());
    }
}
