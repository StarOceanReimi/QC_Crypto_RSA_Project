package cracking;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Slave {

    private int port;
    private String address;

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Socket socket;

    public Slave(String address, int port) {
        this.address = address;
        this.port    = port;
    }

    private void connect() {
        try {
            socket = new Socket(address, port);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
        } catch(IOException ex) {
            System.out.printf("can not connect to %s:%d\n", address, port);
            throw new RuntimeException(ex);
        }
    }

    private Result process(Job job) {
        System.out.println(job);
        return new Result("test");
    }

    public void work() {
        connect();
        try {
            while(true) {
                Object cmd = input.readObject();
                if(cmd instanceof Job) {
                    Result result = process((Job)cmd);
                    output.writeObject(result);
                }
                if(cmd instanceof Command) {
                    if(cmd.equals(Command.NoMoreJob)) {
                        break;
                    }
                }
            }
            input.close();
            output.close();
            socket.close();
        } catch(IOException ex) {
            ex.printStackTrace();
            System.out.println("Network Connection failed.");
        } catch(ClassNotFoundException ex) {
            System.out.println("Job class not found.");
        }
    }

    public static void main(String[] args) {
        int port = 5506;
        String address = "localhost";
        Slave worker = new Slave(address, port);
        worker.work();

    }

}
