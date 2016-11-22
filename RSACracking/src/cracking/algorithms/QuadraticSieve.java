/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import cracking.Main;
import static cracking.algorithms.Factorization.henselLifting;
import static cracking.algorithms.MathOp.EIGHT;
import static cracking.algorithms.MathOp.TWO;
import static cracking.algorithms.MathOp.gcd;
import static cracking.algorithms.MathOp.congruent;
import static cracking.algorithms.MathOp.divides;
import static cracking.algorithms.MathOp.legendre;
import static cracking.algorithms.MathOp.modInverse;
import static cracking.algorithms.MathOp.newtonSqrt;
import cracking.algorithms.Primes.PrimitiveEratosPrimeGenerator;
import static cracking.algorithms.Primes.RandomPrimeGenerator.ODD_FUNC;
import static cracking.algorithms.Primes.findClosePrime;
import static cracking.algorithms.Primes.millerRabinTest;
import static cracking.utils.Util.error;
import static cracking.utils.Util.mustPositive;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.round;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import java.nio.file.Paths;
import static java.util.Arrays.stream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import java.util.stream.IntStream;

/**
 *
 * @author Li
 */
public class QuadraticSieve implements Runnable {
    
    private final BigInteger N;
    private final BigInteger start;
    private final BigInteger end;
    private final int B;
    private final int M;
    private double R;
    private int threshold;
    private int domain;
    private int smoothApprox;
    private boolean multi = false;
    private Set<Integer> smoothCandidates;
    private Set<BigInteger> bSmoothRef;
    
    int[] factorBase;
    int[] sieve;
    int[][] roots;
    int[][] pos;
    int[]   logp;
    
    int counter = 0;
    
    private void buildFactorBase() {
        IntStream.Builder builder = IntStream.builder();
        Iterator<Integer> gen = new PrimitiveEratosPrimeGenerator().gen();
        while(true) {
            int p = gen.next();
            if(p > B) break;
            if(legendre(N, valueOf(p)) == 1)
                builder.accept(p);
        }
        factorBase = builder.build().toArray();
    }
    
    private void dataCollection() {
        pos   = new int[B+1][];
        roots = new int[B+1][];
        logp  = new int[B+1];
        for(int p : factorBase) {
            BigInteger bp = valueOf(p);
            logp[p] = (int)round(log(p));
            if(p == 2) {
                roots[p] = new int[] {1};
                pos[p] = new int[] { ONE.subtract(start).mod(bp).intValue() };
            } else {
                BigInteger[] sols = MathOp.shanksTonelli(N, valueOf(p));
                roots[p] = stream(sols).mapToInt(r->r.intValue()).toArray();
                pos[p] = stream(sols).mapToInt(r->r.subtract(start).mod(bp).intValue()).toArray();
            }
        }
    }
    
    private BigInteger q = null;
    
    private int multipoly(BigInteger a, BigInteger b, int root, int p) {
        BigInteger bp = valueOf(p);
        if(divides(bp, a)) return root;
        BigInteger r = valueOf(root);
        BigInteger t = r.subtract(b);
        BigInteger aI = modInverse(a, bp);
        return t.multiply(aI).subtract(start).mod(bp).intValue();
    }

    public void setQ(BigInteger q) {
        this.q = q;
    }

    private void buildSieveMutiPoly() {
        if(q == null) {
            q = newtonSqrt(N.multiply(TWO)).toBigInteger();
            q = newtonSqrt(q.divide(valueOf(M))).toBigInteger();
        }
        while(smoothCandidates.size() < factorBase.length) {
            
            q = findClosePrime(q, p->legendre(N, p)==1);
            BigInteger a = q.pow(2);
            BigInteger[] sols = henselLifting(N, q);
            BigInteger b = congruent(sols[0].pow(2), N, a) ? sols[0] : sols[1];
            sieve = new int[domain];
            for(int p : factorBase) {
                int[] rs = roots[p];
                saveLocation(multipoly(a, b,rs[0], p), p);
                if(p != 2) {
                    saveLocation(multipoly(a, b,rs[1], p), p);
                }
            }
            q = ODD_FUNC.apply(q);
        }
    }

    private void buildSieve() {
        sieve = new int[domain];
        for(int p : factorBase) {
            int[] rs = pos[p];
            saveLocation(rs[0], p);
            if(p != 2) {
                saveLocation(rs[1], p);
            }
        }
    }

    //XXX: trying to find some efficient way to identify
    //     smooth candidates other than trail division
    private void verifySmooth() {
        for(int loc : smoothCandidates) {
            BigInteger smooth = start.add(valueOf(loc));
            BigInteger qx = smooth.pow(2).subtract(N).abs();
            if(isSmooth(qx)) { 
                counter++;
                if(bSmoothRef != null) {
                    bSmoothRef.add(smooth);
                }
            }
        }
    }
    
    private void saveLocation(int r, int p) {
        int i=0;
        while(true) {
            int l = r+i*p;
            if(l >= domain) break;
            sieve[l] += logp[p];
            if(abs(sieve[l]-smoothApprox) < threshold) {
                smoothCandidates.add(l);
            }
            i++;
        }
    }
    
    public QuadraticSieve mulitPoly() { 
        multi = true;
        return this;
    }

    private void init() {
        multi = false;
        threshold = 5;
        domain = 2*M+1;
        R = .95;
        smoothApprox = (int)(log(N.doubleValue())/2+log(M));
        smoothCandidates = new HashSet<>();
    }
    
    public QuadraticSieve(BigInteger N, int B, BigInteger start, BigInteger end) {
        this.N = N;
        this.B = B;
        this.start = start;
        this.end = end;
        this.M = end.subtract(start).intValue();
        mustPositive(M);
        init();
    }
    
    
    public QuadraticSieve(BigInteger N, int B, int M) {
        this.N = N;
        this.B = B;
        this.M = M;
        mustPositive(M);
        BigInteger sqrtN = newtonSqrt(N).toBigInteger();
        this.start = sqrtN.subtract(valueOf(M));
        this.end = sqrtN.add(valueOf(M));
        init();
    }
    
    public Set<BigInteger> getBSmooth() {
        return smoothCandidates.stream().map(loc->start.add(valueOf(loc))).collect(toSet());
    }

    public void setBSmoothRef(Set<BigInteger> bSmoothRef) {
        this.bSmoothRef = bSmoothRef;
    }
    
    
    private BigInteger getK() {
        if(!gcd(EIGHT, N).equals(ONE)) 
            error("N has a factor of 8");
        return modInverse(N, EIGHT);
    }
    
    private boolean isSmooth(BigInteger candidate) {
        for(int p : factorBase) {
            BigInteger bp = valueOf(p);
            while(candidate.mod(bp).equals(ZERO)) {
                candidate = candidate.divide(bp);
            }
        }
        if(candidate.equals(ONE)) return true;
        //if(millerRabinTest(candidate)) return true;
        return false;
    }

    
    
    @Override
    public void run() {
        buildFactorBase();
        dataCollection();
        if(!multi) buildSieve();
        else       buildSieveMutiPoly();
        verifySmooth();
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        BigInteger N = Main.TARGET; //new BigInteger("6275815110957813119593022531213");
//        BigInteger N1 = new BigInteger("6275815110957813119593022531213");
        QuadraticSieve sieve = new QuadraticSieve(N, 1_000_000, 200_000_000);
        sieve.buildFactorBase();
//        
//        System.out.println(sieve.isSmooth(new BigInteger("782977848170708394135694655741222").pow(2).subtract(N)));

        int check;
        IntStream.Builder builder;
        try (RandomAccessFile raf = new RandomAccessFile(Paths.get(".", "SmoothNumber3").toFile(), "r")) {
            check = 1000;
            int cnt = 0;
            builder = IntStream.builder();
            if(raf.getFilePointer() < raf.length()) {
                while (cnt++ < check) {
                    BigInteger x = new BigInteger(raf.readLine());
                    BigInteger qx = x.pow(2).subtract(N).abs();
                    if(sieve.isSmooth(qx)) {
                        builder.accept(1);
                    } else {
                        builder.accept(0);
                    }
                }
            }
        }
        int partial = (int)builder.build().filter(i->i==0).count();
        System.out.println((double)partial/check);
        
        
//        BigInteger sqrtN = newtonSqrt(N).toBigInteger();
//        BigInteger M = valueOf(8_000_000);
//        BigInteger S = sqrtN.subtract(M);
//        BigInteger E = sqrtN.add(M);
//        QuadraticSieve sieve = new QuadraticSieve(N, 1_000_000, S, E);
//        sieve.buildFactorBase();
//        BigInteger x = new BigInteger("782977848170708394133697102616780");
//        BigInteger qx = x.pow(2).subtract(N);
//        System.out.println(sieve.isSmooth(qx));
        
//        sieve.run();
//        System.out.println(sieve.factorBase.length);
//        System.out.println(sieve.smoothCandidates.size());
//        System.out.println(sieve.counter);
//        sieve.buildFactorBase();
//        System.out.println(sieve.getK());
//        BigInteger kN = new BigDecimal(sieve.getK())
//                            .multiply(new BigDecimal(Main.TARGET))
//                            .toBigInteger();
//        BigInteger maxP = valueOf(sieve.factorBase[sieve.factorBase.length-1]);
//        double tester = log(M.multiply(newtonSqrt(kN.divide(TWO)).toBigInteger()).divide(maxP.pow(3)).doubleValue());
//        System.out.println(tester);
        
        

    }


}
