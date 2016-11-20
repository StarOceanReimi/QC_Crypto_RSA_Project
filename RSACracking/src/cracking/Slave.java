package cracking;

import cracking.algorithms.MathOp;
import cracking.algorithms.QuadraticSieve;
import cracking.utils.Util;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import static java.math.BigInteger.valueOf;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
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
        BigInteger end = job.getEnd();
        BigInteger start = job.getStart();
        Set<BigInteger> smooth = ConcurrentHashMap.newKeySet();
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
            BigInteger[] bSmooth = smooth.toArray(new BigInteger[smooth.size()]);
            ret.setBSmooth(bSmooth);
            return ret;
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Unable to find localhost..");
        }
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
//        BigInteger N = Main.TARGET;
//        BigInteger sqrtN = MathOp.newtonSqrt(N).toBigInteger();
//        BigInteger M = valueOf(1_000_000_000);
//        Result ret = worker.process(new Job(N, 1_000_000, sqrtN.subtract(M), sqrtN.add(M)));
//        System.out.println(ret);
//        System.out.println(ret.getBSmooth().length);

        worker.work();
        
        
        
    }

}
