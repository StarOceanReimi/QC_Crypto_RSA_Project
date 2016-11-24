package cracking.cluster;

import cracking.algorithms.QuadraticSieve;
import cracking.utils.Util;
import static cracking.utils.Util.error;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Slave {

    private int port;
    private String address;

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private final long freeMem;
    private final int  cpuCount;
    private final double mutiplier;
    private Socket socket;

    public Slave(String address, int port) {
        this.address = address;
        this.port    = port;
        mutiplier = 0.6;
        freeMem = Runtime.getRuntime().freeMemory();
        cpuCount = Runtime.getRuntime().availableProcessors();
    }

    private void connect() throws IOException {
        socket = new Socket(address, port);
        input = new ObjectInputStream(socket.getInputStream());
        output = new ObjectOutputStream(socket.getOutputStream());
    }

    private Result process(Job job) {
        System.out.println(job);
        BigInteger end = job.getEnd();
        BigInteger start = job.getStart();
        Set<SmoothInfo> smooth = ConcurrentHashMap.newKeySet();
        BigInteger diff = end.subtract(start);
        int times = (int)Math.ceil(diff.doubleValue()/(freeMem*mutiplier));
        BigInteger[][] ranges = Util.splitRange(times, start, end);
        int t=0;
        while(--times >= 0) {
            ExecutorService threadManager = Executors.newFixedThreadPool(cpuCount);
            BigInteger[] range = ranges[t++];
            BigInteger[][] threadRanges = Util.splitRange(cpuCount, range[0], range[1]);
            for(int i=0; i<cpuCount; i++) {
                BigInteger[] threadRange = threadRanges[i];
                QuadraticSieve sieve = new QuadraticSieve(job.getN(), 
                                job.getB(), threadRange[0], threadRange[1]);
                sieve.setBSmoothRef(smooth);
                threadManager.execute(sieve);
            }
            threadManager.shutdown();
            try {
                threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                System.out.println("Thread was interrupted.");
            }
        }
        
        try {
            String resultMsg = String.format("Result from %s, %d", Inet4Address.getLocalHost(), System.currentTimeMillis());
            Result ret = new Result(resultMsg);
            SmoothInfo[] bSmooth = smooth.toArray(new SmoothInfo[smooth.size()]);
            ret.setBSmooth(bSmooth);
            return ret;
        } catch (UnknownHostException ex) {
            error("Unable to find localhost. %s", ex.getMessage());
        }
        //unreachable
        return null;
    }

    public void work() {
        try {
            connect();
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
            System.err.printf("Can not connected to server[%s:%d] \n", address, port);
            System.err.printf("Network Connection failed. %s\n", ex.getMessage());
        } catch(ClassNotFoundException ex) {
            System.err.println("Job class not found.");
        }
    }

    public static void main(String[] args) {
        int port = 5506;
        String address = "localhost";
        if(args.length > 0)
            address = args[0];
        if(args.length > 1)
            port = Integer.parseInt(args[1]);
        
        Slave worker = new Slave(address, port);
        worker.work();
//        BigInteger N = Main.TARGET;
//        BigInteger sqrtN = MathOp.newtonSqrt(N).toBigInteger();
//        BigInteger M = valueOf(1_000_000_000);
//        Result ret = worker.process(new Job(N, 1_000_000, sqrtN.subtract(M), sqrtN.add(M)));
//        System.out.println(ret);
//        System.out.println(ret.getBSmooth().length);
        
    }

}
