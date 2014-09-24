import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/*
* Server to process ping requests over UDP. java PingClient host port
*/
public class PingClient {

    public static void main(String[] args) throws Exception {
    // Get command line argument.
        if (args.length != 2) {
            System.out.println("Required arguments: host port");
            return;
        }

        String ServerName = args[0];
        int port = Integer.parseInt(args[1]);


        // Create a datagram socket for receiving and sending UDP packets
        // through the port specified on the command line.
        DatagramSocket socket = new DatagramSocket();
        InetAddress IPAddress =InetAddress.getByName(ServerName);
        // Processing loop.

        // You should write the client so that it sends
        // 10 ping requests to the server, separated by
        // approximately one second.

        for(int i=0;i<10;i++) {

            // Create a datagram packet to hold outgoing UDP packet.

            //Each message contains
            // a payload of data that includes the keyword PING,
            // a sequence number, and a timestamp.
            long SendTime = System.currentTimeMillis();
            String Message = i + " " + SendTime;
            System.out.println(Message.length());
            DatagramPacket request = new DatagramPacket(Message.getBytes(), Message.length(), IPAddress, port);
            socket.send(request);
            DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);

            socket.setSoTimeout(1000);

            try {
                socket.receive(reply);
                printData(reply);
            } catch(IOException E) {}

            Thread.sleep(1000);

        }

        // After sending each packet, the client waits up
        // to one second to receive a reply.
        // If one seconds goes by without a reply from the server,
        // then the client assumes that its packet or the
        // server's reply packet has been lost in the network.

    }

    /*
    * Print ping data to the standard output stream.
    */
    private static void printData(DatagramPacket request) throws Exception {
        // Obtain references to the packet's array of bytes.
        byte[] buf = request.getData();

        // Wrap the bytes in a byte array input stream,
        // so that you can read the data as a stream of bytes.
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        // Wrap the byte array output stream in an input stream reader,
        // so you can read the data as a stream of characters.
        InputStreamReader isr = new InputStreamReader(bais);

        // Wrap the input stream reader in a bufferred reader,
        // so you can read the character data a line at a time.
        // (A line is a sequence of chars terminated by any combination of \r and \n.)
        BufferedReader br = new BufferedReader(isr);

        // The message data is contained in a single line, so read this line.
        String line = br.readLine();

        // Print host address and data received from it.
        System.out.println(
                "Received from " +
                        request.getAddress().getHostAddress() +
                        ": " +
                        new String(line) );
    }
}