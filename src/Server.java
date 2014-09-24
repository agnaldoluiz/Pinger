/**
 * Created by agnaldocunha on 9/19/14.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends JFrame {
    private JTextField enterField;
    private JTextArea displayArea;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;
    private int counter = 1;

    //Constructor: set up GUI
    public Server()
    {
        super("Server");
        Container container = getContentPane();
        //create enterField and register listener
        enterField = new JTextField();
        enterField.setEditable(false);
        enterField.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        sendData(event.getActionCommand());
                        enterField.setText("");
                    }
                }
        );
        container.add(enterField, BorderLayout.NORTH);

        //create displayArea
        displayArea = new JTextArea();
        container.add( new JScrollPane(displayArea), BorderLayout.CENTER);
        setSize(300,150);
        setVisible(true);
    }

    //Most important method: run the server
    public void runServer() {
        try {
            //Step 1: Create a ServerSocket
            server = new ServerSocket(12345, 100);
            while (true) {
                try {
                    waitForConnection(); //Step 2: Wait for Connection
                    getStreams(); //Step 3: Get input and output streams
                    processConnection(); //Step 4: Process connection
                }

                //Process EOFException when client closes connection
                catch (EOFException eofException) {
                    System.err.println("Server terminated connection");
                }

                finally {
                    closeConnection(); //Step 5: Close connection
                    ++counter;
                }
            }
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    //wait for connection to arrive. Then display connection info
    private void waitForConnection() throws IOException {
        displayMessage("Waiting for connection\n");
        connection = server.accept();
        displayMessage("Connection " + counter + " received from: " + connection.getInetAddress().getHostName());
    }

    private void getStreams() throws IOException {
        output = new ObjectOutputStream(connection.getOutputStream());
        output.flush();

        input = new ObjectInputStream(connection.getInputStream());

        displayMessage("\nGot I/O streams\n");
    }

    private void processConnection() throws IOException {
        String message = "Connection Successful";
        sendData(message);

        setTextFieldEditable(true);

        do {
            try {
                message = (String)input.readObject();
                displayMessage("\n" + message);
            }
            catch (ClassNotFoundException classNotFoundException) {
                displayMessage("\nUnknown object type received");
            }
        } while (!message.equals("CLIENT>>> TERMINATE"));
    }

    private void closeConnection() {
        displayMessage( "\nTerminating connection\n" );
        setTextFieldEditable( false ); // disable enterField
        try {
            output.close();
            input.close();
            connection.close();
        }

        catch( IOException ioException ) {
            ioException.printStackTrace();
        }
    }

    // send message to client
    private void sendData( String message ) {
        try {
            output.writeObject("SERVER>>> " + message);
            output.flush();
            displayMessage("\nSERVER>>> " + message);
        }

        catch (IOException ioException) {
            displayArea.append("\nError writing object");
        }
    }

    private void displayMessage (final String messageToDisplay) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        displayArea.append(messageToDisplay);
                        displayArea.setCaretPosition(displayArea.getText().length());
                    }
                }
        );
    }

    private void setTextFieldEditable(final boolean editable) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        enterField.setEditable(editable);
                    }
                }
        );
    }

    public static void main(String args[]) {
        Server application = new Server();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        application.runServer();
    }
}
