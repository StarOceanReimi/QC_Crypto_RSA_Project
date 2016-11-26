package cracking.cluster;
import cracking.Main;
import static cracking.algorithms.MathOp.TWO;
import static cracking.algorithms.MathOp.legendre;
import static cracking.algorithms.MathOp.newtonSqrt;
import cracking.algorithms.Primes;
import static cracking.algorithms.Primes.findClosePrime;
import static cracking.utils.Util.error;
import java.io.File;
import java.math.BigInteger;
import java.util.LinkedList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import static java.math.BigInteger.valueOf;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.util.Arrays.stream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class Master {
    
    private BigInteger start;
    private BigInteger end;
    private BigInteger N;
    private int B;
    private ThreadGroup clientGroup;

    private int port = 5506;

    private final LinkedList<Job> taskQueue;
    
    private final PrintStream smoothResult;
    private final Object printLocker = new Object();
    private AtomicInteger smoothCounter;
    private Thread multipolyProducer;
    
    private volatile boolean serverDown = false;

    private File findSuitableFile() throws IOException {
        long filesNum = Files.list(Paths.get(".")).filter(p->p.toString().matches(".*SmoothNumber\\d+")).count();
        return Files.createFile(Paths.get(".", "SmoothNumber"+filesNum)).toFile();
    }
    
    public Master(BigInteger N, int B, BigInteger start, BigInteger end) {
        this.start = start;
        this.end   = end;
        this.N     = N;
        this.B     = B;
        this.taskQueue = new LinkedList<>();
        this.clientGroup = new ThreadGroup("ClinetThreads");
        File smoothNumberFile;
        try {
            smoothNumberFile = findSuitableFile();
            smoothResult = new PrintStream(smoothNumberFile);
        } catch (IOException ex) {
            error("can not access file");
        }
        smoothCounter = new AtomicInteger();
    }

    public void startMakingPolyPrime() {
        multipolyProducer = new Thread(new PrimeProducer());
        multipolyProducer.start();
    }
    
    public void makeAssignment(int pieces) {
        BigInteger portion = BigInteger.valueOf(pieces);
        BigInteger chunk = end.subtract(start).divide(portion);
        BigInteger init  = start;
        for(int i=0; i<pieces; i++) {
            BigInteger temp = init.add(chunk);
            taskQueue.add(new Job(N, B, init, temp));
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
            System.err.println("Server cant listening on port:" + port);
            System.err.println(ex.getMessage());
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
    
    class PrimeProducer implements Runnable {

        private int fbSize = 0;

        private final double R = 0.95;
        
        public PrimeProducer() {
            Iterator<Integer> gen = new Primes.PrimitiveEratosPrimeGenerator().gen();
            while(true) {
                int p = gen.next();
                if(p > B) break;
                if(legendre(N, valueOf(p)) == 1) fbSize++;
            }
            fbSize = (int)(fbSize*R);
        }
        
        @Override
        public void run() {
            BigInteger q = newtonSqrt(N.multiply(TWO)).toBigInteger();
            q = newtonSqrt(q.divide(end.subtract(start))).toBigInteger();
            while(fbSize > smoothCounter.get()) {
                q = findClosePrime(q, p->legendre(N, p)==1);
                try {
                    synchronized(taskQueue) {
                        while(taskQueue.size() > 100) {
                            taskQueue.wait();
                        }
                        taskQueue.add(new Job(N, B, q, start, end));
                    }
                } catch (InterruptedException ex) {
                    System.err.printf("producer thread was interrupted: %s\n", ex.getMessage());
                }
            }
        }
        
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
                taskQueue.notify();
            }
            return job;
        }

        void assignJob(Job job) {
            try {
                output.writeObject(job);
                output.flush();
            
            } catch(IOException ex) {
                synchronized(taskQueue) {
                    taskQueue.offer(job);
                }
                error("Error occur in assigning job.");
            }
        }

        void collectResult(Job job) {
            try {
                Result result = (Result)input.readObject();
                System.out.printf("total relation in %s is %d.\n", result, 
                                    result.getBSmooth().length);
                smoothCounter.addAndGet(result.getBSmooth().length);
                synchronized(printLocker) {
                    for(SmoothInfo info : result.getBSmooth()) {
                        smoothResult.println(info);
                    }
                }
            } catch(IOException | ClassNotFoundException ex) {
                synchronized(taskQueue) {
                    taskQueue.offer(job);
                }
                error("Error occur in collectResult.");
            }
        }

        void shutdownServer() {
            if(serverDown) return;
            try { new Socket(InetAddress.getLocalHost(), port); }
            catch (IOException ex) {  }
        }

        void done() {
            try {
                input.close();
                output.close();
                clientSock.close();
            } catch(IOException ex) {
                //sliently close
            } 
        }

        @Override
        public void run() {
            try {
                while(true) {
                    Job job = take();
                    if(job == null) break;
                    assignJob(job);
                    collectResult(job);
                }
                output.writeObject(Command.NoMoreJob);
                shutdownServer();
            } catch(Exception ex) {
                System.err.println(ex.getMessage());
                System.err.println("Client Handler closed.");
            } finally {
                done();    
            }
            
            
        }
    }

    public static void main(String[] args) {
        BigInteger M = BigInteger.valueOf(1_000_000_000);
        Integer B = 1_000_000;
        Integer J = 8;
        if(args.length > 0)
            B = Integer.parseInt(args[0]);
        if(args.length > 1)
            M = new BigInteger(args[1]);
        if(args.length > 2)
            J = Integer.parseInt(args[2]);
        
        BigInteger N = Main.TARGET;
        BigInteger sqrtN = newtonSqrt(N).toBigInteger();
        
        Master master = new Master(Main.TARGET, B, sqrtN.subtract(M), sqrtN.add(M));
        master.makeAssignment(J);
        master.listening();
    }
}
