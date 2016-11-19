/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import cracking.Main;
import static cracking.algorithms.MathOp.TWO;
import static cracking.algorithms.MathOp.gcd;
import static cracking.algorithms.MathOp.shanksTonelli;
import static cracking.utils.Util.mustPositive;
import java.io.FileNotFoundException;
import java.io.IOException;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static java.lang.String.format;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 *
 * @author Li
 */
public class QuadraticSieveFactorization {
    
    private static final BiFunction<BigInteger, BigInteger, BigInteger> Qx = (x, N)->x.pow(2).subtract(N);
    
    private List<BigInteger> factors;
    private BigInteger B;
    private int        M;
    
    private final BigInteger startPosition;
    private final BigInteger endPosition;
    
    private List<BigInteger> factorbase;
    
    private Double[]  sieveRemaing;

    private Set<Integer> smoothIndices;
    
    private int[][]      expMatrix;
    
    private int[][]      nullSpace;
    
    public QuadraticSieveFactorization(BigInteger xStart, BigInteger xEnd) {
        startPosition = xStart.min(xEnd);
        endPosition = xStart.max(xEnd);
        BigInteger limit = valueOf(Integer.MAX_VALUE);
        BigInteger M = endPosition.subtract(startPosition);
        mustPositive(limit.subtract(M));
        this.M = M.intValue();
        
    }

    public List<BigInteger> factorize(BigInteger N) {
        process(N);
        return factors;
    }

    private void process(BigInteger N) {
        pickingUpperBoundOfFactorbase(N.doubleValue());
        buildFactorbase(N);
        buildSieve(N);
        buildExpVectors(N);
        findNullSpace();
        calculateFactor(N);
    }

    private void buildExpVectors(BigInteger N) {
        int row = smoothIndices.size();
        mustPositive(row, row-factorbase.size());
        expMatrix = new int[row][];
        int cnt = 0;
        for(int index : smoothIndices) {
            BigInteger x = startPosition.add(valueOf(index));
            BigInteger smoothQx = Qx.apply(x, N);
            int[] expVector = new int[factorbase.size()+1];
            expMatrix[cnt++] = expVector;
            expVector[0] = smoothQx.compareTo(ZERO) > 0 ? 0 : 1;
            for(int i=0, n=factorbase.size(); i<n; i++) {
                BigInteger p = factorbase.get(i);
                while(smoothQx.mod(p).equals(ZERO)) {
                    expVector[i+1] ^= 1;
                    smoothQx = smoothQx.divide(p);
                }
            }
        }
        
    }
    
    private void pickingUpperBoundOfFactorbase(double N) {
        //System.out.println(valueOf((long)exp(0.5*sqrt(log(N)*log(log(N))))));
//        B = valueOf((long)Math.pow(exp(sqrt(log(N)*log(log(N)))), sqrt(2)/4));
        
        B = valueOf(15_000);
    }
    
    
    
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
//        BigInteger N = Main.TARGET;
//        System.out.println(shanksTonelli(N, valueOf(8)));
//        BigInteger N = new BigInteger("30940085241669403305401996108077375566839");

        BigInteger N = new BigInteger("6275815110957813119593022531213");        
        BigInteger SQRT_N = MathOp.newtonSqrt(N).toBigInteger();
        BigInteger M = valueOf(8_000_000);
        BigInteger begin = SQRT_N.subtract(M);
        QuadraticSieveFactorization qsf = new QuadraticSieveFactorization(begin, SQRT_N.add(M));
        List<BigInteger> factors = qsf.factorize(N);
        System.out.println(format("%d/%d", qsf.smoothIndices.size(), qsf.factorbase.size()));
        System.out.println(factors.get(0).multiply(factors.get(1)));
        for(BigInteger p : factors) {
            System.out.println(Primes.millerRabinTest(p));
        }
    }

    private void buildFactorbase(BigInteger N) {
        factorbase = Factorization.factorBase(B, N);
    }

    private BigInteger[][] splitRangeInto(int pieces) {
        BigInteger[][] pairs = new BigInteger[pieces][2];
        BigInteger chunk = valueOf(M/pieces);
        BigInteger startPos = startPosition;
        for(int i=0; i<pieces; i++) {
            pairs[i] = new BigInteger[2];
            pairs[i][0] = startPos;
            pairs[i][1] = startPos.add(chunk);
            startPos = pairs[i][1];
        }
        return pairs;
    }
    
    private void buildSieve(BigInteger N) {
        int numberOfProcessor = 8;
        sieveRemaing = new Double[M];
        smoothIndices = ConcurrentHashMap.newKeySet(factorbase.size()+1);
        multiThreadSieving(N, numberOfProcessor);
    }
    
    private void multiThreadSieving(BigInteger N, int numberOfProcessor) {
        BigInteger[][] ranges = splitRangeInto(numberOfProcessor);
        List<Thread> threadHolder = new LinkedList<>();
        for(int i=0; i<ranges.length; i++) {
            SievingStrategy s = new SievingStrategy(N, ranges[i][0], ranges[i][1]);
            Thread task = new Thread(s);
            threadHolder.add(task);
            task.start();
        }
        waitingThreadsFinish(threadHolder);
    }

    private void waitingThreadsFinish(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
        }
    }

    private void findNullSpace() {
        nullSpace = Matrix.nullspace(expMatrix);
    }

    private void calculateFactor(BigInteger N) {
        Integer[] idX = smoothIndices.toArray(new Integer[0]);
        BigInteger factorOne = ONE;
        factors = new LinkedList<>();
        for(int i=0, n=nullSpace.length; i<n; i++) {
            BigInteger prodX = ONE;
            BigInteger prodQx = ONE;
            
            for(int j=0; j<nullSpace[i].length; j++) {
                if(nullSpace[i][j] == 1) {
                    BigInteger x = startPosition.add(valueOf(idX[j]));
                    prodX = prodX.multiply(x);
                    BigInteger qx = Qx.apply(x, N);
                    prodQx = prodQx.multiply(qx);
                }
            }
            Map<BigInteger, Integer> prodFactors = factorQx(prodQx);
            prodFactors.replaceAll((f,pow)->pow/2);
            Stream<BigInteger> fStream = prodFactors.keySet().stream();
            BigInteger sqrtQx = fStream.reduce(ONE, (v,f)->v.multiply(f.pow(prodFactors.get(f))));
            BigInteger candidate = gcd(N, sqrtQx.add(prodX).abs());
            if(!candidate.equals(ONE) && !candidate.equals(N)) {
                factorOne = candidate;
                break;
            }
        }
        if(factorOne.equals(ONE)) return;
        factors.add(factorOne);
        factors.add(N.divide(factorOne));
    }

    private boolean enoughSmoothFactors() {
        return false;
    }
    
    private Map<BigInteger, Integer> factorQx(BigInteger qx) {
        Map<BigInteger, Integer> fMap = new HashMap<>();
        for(BigInteger p : factorbase) {
            while(qx.mod(p).equals(ZERO)) {
                fMap.put(p, fMap.getOrDefault(p, 0)+1);
                qx = qx.divide(p);
            }
        }
        return fMap;
    }

    class SievingStrategy implements Runnable {

        private final BigInteger start;
        private final BigInteger end;
        private BigInteger N;
        private int        pos;
        
        public SievingStrategy(BigInteger N, BigInteger start, BigInteger end) {
            this.start = start;
            this.end = end;
            this.N   = N;
            resetPos();
        }
        
        final void resetPos() {
            this.pos = start.subtract(startPosition).intValue();
        }

        void sieving() {
            final Iterator<BigInteger> iter = factorbase.iterator();
            while(iter.hasNext()) {
                BigInteger p = iter.next();
                BigInteger[] sols;
                if(p.equals(TWO)) {
                    sols = new BigInteger[] { ONE };
                } else {
                    sols = shanksTonelli(N, p);
                }
                for(BigInteger sol : sols) {
                    resetPos();
                    findInitialCandidate(sol, p);
                    populateAllCandidates(p);
                    if(enoughSmoothFactors()) return;
                }
            }
        }
        
        @Override
        public void run() {
            sieving();
        }

        private void findInitialCandidate(BigInteger sol, BigInteger p) {
            for(BigInteger x=start; x.compareTo(end) < 0; x=x.add(ONE)) {
                if(x.mod(p).equals(sol)) {
                    return;
                }
                pos++;
            }
        }

        /*
        
                BigInteger qx = sieveRemaing[pos];
                if(qx == null) {
                    qx = startValue.pow(2).subtract(N);
                }
                while (qx.mod(p).equals(ZERO)) {
                    qx = qx.divide(p);
                }
                sieveRemaing[pos] = qx;
                if(qx.equals(ONE) || qx.equals(MINUS_ONE)) {
                    smoothIndices.add(pos);
                }
        */
            
        private void populateAllCandidates(BigInteger p) {
            BigInteger startValue = start.add(valueOf(pos).subtract(start.subtract(startPosition)));

            while (startValue.compareTo(end) < 0) {
                if(pos >= M) break;
                Double qxBits = sieveRemaing[pos];
                if(qxBits == null) {
                    BigInteger qx = startValue.pow(2).subtract(N).abs();
                    qxBits = log(qx.doubleValue());
                }
                qxBits -= log(p.doubleValue());
                sieveRemaing[pos] = qxBits;
                double t = Math.round(qxBits);
                if(t < 2 && t > -2) {
                    smoothIndices.add(pos);
                    if(enoughSmoothFactors()) return;
                }
                startValue = startValue.add(p);
                pos += p.intValue();
            }
        }
    }
}
