/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import static cracking.algorithms.MathOp.TWO;
import static cracking.algorithms.MathOp.shanksTonelli;
import static cracking.utils.Util.mustPositive;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static java.lang.Math.log;
import static java.math.BigInteger.valueOf;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Li
 */
public class QuadraticSieveFactorization {
    
    private List<BigInteger> factors;
    private BigInteger B;
    private int        M;
    
    private final BigInteger startPosition;
    private final BigInteger endPosition;
    
    private LinkedList<BigInteger> factorbase;
    
    private BigInteger[]  sieve;
    private List[]        sieveFactors;
    private BigInteger[]  sieveRemaing;
    private Set<Integer> smoothIndices;
    
    private int[][]      expVectors;

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
    }

    private void pickingUpperBoundOfFactorbase(double N) {
        //B = valueOf((long)exp(0.5*sqrt(log(N)*log(log(N))))).divide(valueOf(8));
        B = valueOf(15000);
    }

    public static void main(String[] args) throws InterruptedException {
        BigInteger N = new BigInteger("6275815110957813119593022531213");
        BigInteger SQRT_N = MathOp.newtonSqrt(N).toBigInteger();
        BigInteger M = valueOf(100_000);
        QuadraticSieveFactorization qsf = new QuadraticSieveFactorization(SQRT_N.subtract(M), SQRT_N.add(M));
        qsf.factorize(N);
        System.out.println(qsf.smoothIndices.size()+"/"+qsf.factorbase.size());
        qsf.smoothIndices.forEach(index->System.out.println(qsf.sieve[index]+","+qsf.sieveFactors[index]));
        
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
        sieve = new BigInteger[M];
        sieveRemaing = new BigInteger[M];
        sieveFactors = new List[M];
        smoothIndices = new HashSet<>();
        //new SievingStrategy(N, startPosition, endPosition).run();
        multiThreadSieving(N, numberOfProcessor);
    }
    
    private void multiThreadSieving(BigInteger N, int numberOfProcessor) {
        BigInteger[][] ranges = splitRangeInto(numberOfProcessor);
        ThreadGroup group = new ThreadGroup("SievingGroup");
        for(int i=0; i<ranges.length; i++) {
            SievingStrategy s = new SievingStrategy(N, ranges[i][0], ranges[i][1]);
            new Thread(group, s).start();
        }
        waitingGroupFinish(group);
    }

    private void waitingGroupFinish(ThreadGroup group) {
        Thread[] remainingThreads = new Thread[group.activeCount()];
        group.enumerate(remainingThreads);
        for(Thread t : remainingThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
        }
        group.destroy();
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

        private void populateAllCandidates(BigInteger p) {
            BigInteger startValue = start.add(valueOf(pos).subtract(start.subtract(startPosition)));
            
            while (startValue.compareTo(end) < 0) {
                if(pos >= M) break;
                sieve[pos] = startValue;
                if(sieveFactors[pos] == null) {
                    sieveFactors[pos] = new LinkedList();
                }
                sieveFactors[pos].add(p);
                BigInteger qx = sieveRemaing[pos];
                if(qx == null) {
                    qx = startValue.pow(2).subtract(N);
                }
                while (qx.mod(p).equals(ZERO)) {
                    qx = qx.divide(p);
                }
                if(qx.equals(ONE) || qx.equals(ZERO.subtract(ONE))) {
                    smoothIndices.add(pos);
                }
                sieveRemaing[pos] = qx;
                startValue = startValue.add(p);
                pos += p.intValue();
            }
        }
    }
}
