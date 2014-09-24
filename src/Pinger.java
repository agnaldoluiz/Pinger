/**
 * Created by agnaldocunha on 9/18/14.
 * @author agnaldocunha
 *
 * Params the program should receive
 *   -l <local port>
 *   -h <remote hostname>
 *   -r <remote port>
 *   -c <packet count>
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;


public class Pinger {

    public static void main(String [] args) throws Exception {
        Options options = new Options();
        Option optLocalPort = new Option("l", true, "Local Port");
        Option optHost = new Option("h", true, "Remote Hostname");
        Option optRemotePort = new Option("r", true, "Remote Port");
        Option optPacketCount = new Option("c", true, "Packet Count");

        options.addOption(optLocalPort);
        options.addOption(optHost);
        options.addOption(optRemotePort);
        options.addOption(optPacketCount);

        BasicParser bp = new BasicParser();

        try {
            CommandLine cmd = bp.parse(options, args);

            //CLIENT MODE
            if(cmd.hasOption("c") && cmd.hasOption("h") && cmd.hasOption("r") && cmd.hasOption("l")) {
                String serverName = cmd.getOptionValue("h");
                int port = Integer.parseInt(cmd.getOptionValue("r"));

                //UDP Socket
                DatagramSocket socket = new DatagramSocket();
                InetAddress ipAdress = InetAddress.getByName(serverName);

                //Loop
                for(int sequence = 1; sequence <= Integer.parseInt(cmd.getOptionValue("c")); sequence++) {
                    long sendTime = System.currentTimeMillis();
                    byte[] sequenceByteArray = ByteBuffer.allocate(4).putInt(sequence).array();
                    byte[] sendTimeByteArray = ByteBuffer.allocate(8).putLong(sendTime).array();
                    byte[] bytes = new byte[12];

                    for(int i = 0; i < 4; i++) {
                        bytes[i] = sequenceByteArray[i];
                    }
                    for(int i = 0, j = 4; i < 8; i++, j++) {
                        bytes[j] = sendTimeByteArray[i];
                    }

                    DatagramPacket request = new DatagramPacket(bytes, bytes.length, ipAdress,port);
                    socket.send(request);
                    DatagramPacket reply = new DatagramPacket(new byte[12], 12);

                    socket.setSoTimeout(1000);

                    try {
                        socket.receive(reply);
                        printData(reply);
                    }
                    catch (IOException E) {}
                }

            }

            //SERVER MODE
            else if(cmd.hasOption("l")) {
                int port = Integer.parseInt(cmd.getOptionValue("l"));

                DatagramSocket socket = new DatagramSocket(port);

                while(true) {
                    DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(request);

                    InetAddress clientHost = request.getAddress();
                    int clientPort = request.getPort();
                    byte[] buf = request.getData();
                    DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
                    socket.send(reply);

                    System.out.println("Reply Sent");
                }
            }

            else {
                System.err.println("Error: missing or additional arguments");
                return;
            }
        }
        catch (org.apache.commons.cli.ParseException e) {
            e.printStackTrace();
        }
    }

    private static void printData(DatagramPacket request) throws Exception {
        // Obtain references to the packet's array of bytes.
        byte[] buf = request.getData();

        byte[] counterByteArray = new byte[4];
        byte[] timeByteArray = new byte[8];

        for(int i = 0; i < 4; i ++) {
            counterByteArray[i] = buf[i];
        }
        for(int i = 0, j = 4; i < 8; i++, j++) {
            timeByteArray[i] = buf[j];
        }

        ByteBuffer counterBuffer = ByteBuffer.allocate(Integer.BYTES);
        counterBuffer.put(counterByteArray).flip();
        int count = counterBuffer.getInt();

        ByteBuffer timeBuffer = ByteBuffer.allocate(Long.BYTES);
        timeBuffer.put(timeByteArray).flip();
        long time = timeBuffer.getLong();

        // Print host address and data received from it.
        System.out.println(count + "" + time);
    }
}
