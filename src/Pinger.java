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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;


public class Pinger {

    public static void main(String [] args) throws Exception {
        //Options for the command line
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
                InetAddress ipAddress = InetAddress.getByName(serverName);

                //Counter for received packets
                int received = 0;
                //Array for rrt
                long[] rrtArray = new long[Integer.parseInt(cmd.getOptionValue("c"))];

                //Loop
                for(int sequence = 1; sequence <= Integer.parseInt(cmd.getOptionValue("c")); sequence++) {
                    //Make the packages
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

                    //Sending the packets
                    DatagramPacket request = new DatagramPacket(bytes, bytes.length, ipAddress,port);
                    socket.send(request);
                    DatagramPacket reply = new DatagramPacket(new byte[12], 12);
                    //Wait up to a second to get an answer
                    socket.setSoTimeout(1000);

                    //Receive the packets from the server and print the data
                    try {
                        socket.receive(reply);

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
                        long rrt = System.currentTimeMillis() - time;
                        rrtArray[sequence-1] = rrt;
                        // Print host address and data received from it.
                        System.out.println("size=" + buf.length + " from=" + serverName + " seq=" + count + " rrt=" + rrt + " ms");
                        received++;
                    }
                    catch (IOException E) {}
                    //Wait one second to send another one
                    Thread.sleep(1000);
                }

                double lost = (1 - received/Double.parseDouble(cmd.getOptionValue("c")))*100;
                long rrtMin = rrtMinimum(rrtArray);
                double rrtAvg = rrtAverage(rrtArray);
                long rrtMax = rrtMaximum(rrtArray);
                System.out.println("*************************************************");
                System.out.println("sent=" + cmd.getOptionValue("c") + " received=" + received + " lost=" + lost + "% rrt min/avg/max=" + rrtMin+"/"+rrtAvg+"/"+rrtMax);

            }

            //SERVER MODE
            else if(cmd.hasOption("l")) {
                int port = Integer.parseInt(cmd.getOptionValue("l"));
                //Create socket and set counter
                DatagramSocket socket = new DatagramSocket(port);
                int counter = 0;

                while(true) {
                    DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(request);
                    long time = System.currentTimeMillis();
                    counter++;
                    InetAddress clientHost = request.getAddress();
                    printDataServer(time, counter, clientHost);
                    int clientPort = request.getPort();
                    byte[] buf = request.getData();
                    DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
                    socket.send(reply);
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

    private static void printDataServer(long time, int counter, InetAddress clientHost) throws Exception {
        // Print host address and data received from it.
        System.out.println("time=" + time + " from=" + clientHost + " seq=" + counter );
    }

    //Useful methods
    private static long rrtMinimum(long[] rrtArray) {
        long minValue = rrtArray[0];
        for(int i=1;i<rrtArray.length;i++){
            if(rrtArray[i] < minValue){
                minValue = rrtArray[i];
            }
        }
        return minValue;
    }

    private static long rrtMaximum(long[] rrtArray){
        long maxValue = rrtArray[0];
        for(int i=1;i < rrtArray.length;i++){
            if(rrtArray[i] > maxValue){
                maxValue = rrtArray[i];
            }
        }
        return maxValue;
    }

    private static double rrtAverage(long[] rrtArray) {
        long sum = 0;

        for(long rrt : rrtArray) {
            sum = sum + rrt;
        }
        return (double)sum / rrtArray.length;
    }
}
