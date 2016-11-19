package cracking;
import java.math.BigInteger;
import java.util.LinkedList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

public class Master {
    
    private BigInteger start;
    private BigInteger end;

    private ThreadGroup clientGroup;

    private int port = 5506;

    private final LinkedList<Job> taskQueue;
    
    private volatile boolean serverDown = false;

    public Master(BigInteger start, BigInteger end) {
        this.start = start;
        this.end   = end;
        this.taskQueue = new LinkedList<>();
        this.clientGroup = new ThreadGroup("ClinetThreads");
    }

    public void makeAssignment(int pieces) {
        BigInteger portion = BigInteger.valueOf(pieces);
        BigInteger chunk = end.subtract(start).divide(portion);
        BigInteger init  = start;
        for(int i=0; i<pieces; i++) {
            BigInteger temp = init.add(chunk);
            taskQueue.add(new Job(start, init, temp));
            init = temp;
        }
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void listening() {

        ServerSocket serverSock = null;
        try {
            serverSock = new ServerSocket(port);
            System.out.println("Server is listening on port:" + port);
        } catch(IOException ex) {
            System.out.println("Server cant listening on port:" + port);
            return;
        }
        
        while(true) {
            try {
                Socket clientSock = serverSock.accept();
                if(taskQueue.isEmpty()) { serverDown = true; break; }
                new Thread(clientGroup, new ClientHandler(clientSock)).start();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("connection error");
            }
        }
        System.out.println("Done");

    }

    class ClientHandler implements Runnable {

        private Socket clientSock;
        
        private ObjectInputStream input;
        private ObjectOutputStream output;

        ClientHandler(Socket sock) throws IOException {
            clientSock = sock;
            //must initialize ObjectOutputStream before ObjecctInputStream
            //Otherwise, deadlock..
            output = new ObjectOutputStream(clientSock.getOutputStream());
            input  = new ObjectInputStream(clientSock.getInputStream());
        }

        Job take() {
            Job job = null;
            synchronized(taskQueue) {
                if(taskQueue.isEmpty()) 
                    return null;
                job = taskQueue.poll();
            }
            return job;
        }

        boolean assignJob(Job job) {
            try {
                output.writeObject(job);
                output.flush();
                return true;
            } catch(IOException ex) {
                ex.printStackTrace();
                System.out.println("Error occur in assigning job.");
                synchronized(taskQueue) {
                    taskQueue.offer(job);
                }
                return false;
            }
        }

        void collectResult(Job job) {
            try {
                Result result = (Result)input.readObject();
                System.out.println(result);
            } catch(IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                System.out.println("Error occur in collectResult job.");
                synchronized(taskQueue) {
                    taskQueue.offer(job);
                }
            }
        }

        void shutdownServer() {
            if(serverDown) return;
            try { new Socket(InetAddress.getLocalHost(), port); }
            catch (IOException ex) {  }
        }

        void done() {
            try {
                output.writeObject(Command.NoMoreJob);
                input.close();
                output.close();
                clientSock.close();
                shutdownServer();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
            
        }

        @Override
        public void run() {
            while(true) {
                Job job = take();
                if(job == null) break;
                if(!assignJob(job)) continue;
                collectResult(job);
            }
            done();
        }
    }

    public static void main(String[] args) {
        BigInteger S = BigInteger.valueOf(100_000_000);
        BigInteger E = BigInteger.valueOf(200_000_000);
        Master master = new Master(S, E);
        master.makeAssignment(8);
        master.listening();
    }
}
