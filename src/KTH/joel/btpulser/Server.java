package KTH.joel.btpulser;

import java.io.*;
import java.net.*;

/**
 * @description Run a simple server socket
 *
 * @author Borrowed from Anders Lindström at https://www.kth.se/social/upload/50ab584ff276540cb1eaa971/NoninServer.java
 *
 */
public class Server
{
    public static void main(String[] args) throws Exception
    {
        ServerSocket server = null;
        boolean running = true;

        try {
            server = new ServerSocket(6667);
            System.out.println(String.format("Server started on %s:%d", server.getLocalSocketAddress(), 6667));

            while(running) {
                Socket socket = null;
                PrintWriter writer = null;
                BufferedReader reader = null;
                try {
                    socket = server.accept();
                    System.out.println(socket.getInetAddress());

                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer = new PrintWriter(new FileWriter("nonin.txt", false));

                    String line = reader.readLine();
                    while(line != null) {
                        System.out.println(line);
                        writer.println(line);
                        line = reader.readLine();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                finally {
                    writer.close();
                    socket.close();
                    System.out.println("Client socket closed");
                }
            }
        }
        finally {
            if (server != null) server.close();
            System.out.println("Server stopped");
        }
    }

}
