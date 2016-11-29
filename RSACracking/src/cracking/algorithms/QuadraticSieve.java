/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import cracking.Main;
import cracking.SmoothNumberFileParser;
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
import cracking.cluster.SmoothInfo;
import cracking.utils.Util;
import static cracking.utils.Util.error;
import static cracking.utils.Util.mustPositive;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.round;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import java.util.Arrays;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
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
    private int threshold;
    private int domain;
    private int smoothApprox;
    private boolean multi = false;
    private Set<Integer> smoothCandidates;
    private Set<SmoothInfo> bSmoothRef;
    
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
    private void saveSmooth() {
        for(int loc : smoothCandidates) {
            BigInteger x = start.add(valueOf(loc));
            BigInteger qx = x.pow(2).subtract(N).abs();
            IntStream.Builder factors = IntStream.builder();
            SmoothInfo info = null;
            for(int p : factorBase) {
                BigInteger bp = valueOf(p);
                boolean oddPower = false;
                while(divides(bp, qx)) {
                    qx = qx.divide(bp);
                    oddPower = !oddPower;
                }
                if(oddPower) factors.accept(p);
            }
            
            if(qx.equals(ONE)) {
                info = new SmoothInfo(factors.build().toArray(), x, null);
            } else if(millerRabinTest(qx)) {
                info = new SmoothInfo(factors.build().toArray(), x, qx);
            }
            
            if(info != null) {
                if(bSmoothRef != null) bSmoothRef.add(info);
                counter++;
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

    public void setBSmoothRef(Set<SmoothInfo> bSmoothRef) {
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
        if(millerRabinTest(candidate)) return true;
        return false;
    }

    
    
    @Override
    public void run() {
        buildFactorBase();
        dataCollection();
        if(!multi) buildSieve();
        else       buildSieveMutiPoly();
        saveSmooth();
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
//        BigInteger N1 = Main.TARGET; //new BigInteger("6275815110957813119593022531213");
//        BigInteger N1 = new BigInteger("489293762578746481445307920902864380533");
        
        Primes.RandomPrimeGenerator randomGen = new Primes.RandomPrimeGenerator();
        BigInteger b1 = randomGen.gen(6).next();
        BigInteger b2 = randomGen.gen(7).next();
        BigInteger N1 = b1.multiply(b2);
        System.out.println(N1);
        int B = 100;
        int M = 500;
        QuadraticSieve sieve = new QuadraticSieve(N1, B, M);
//        System.out.println(Arrays.toString(Factorization.fastFactorBase(B, N1)));
        Set<SmoothInfo> relations = new HashSet<>();
        sieve.setBSmoothRef(relations);
        sieve.run();
        
        File file = new File("./testRelation");
        if(file.exists()) file.delete();
        file.createNewFile();
        PrintStream ps = new PrintStream(file);
        relations.forEach(ps::println);
        System.out.printf("%d/%d\n", relations.size(), sieve.factorBase.length);
        
        if(relations.size() < sieve.factorBase.length) {
            System.err.printf("%d/%d, not enough smooth!\n", relations.size(), sieve.factorBase.length);
            System.exit(1);
        }
        SmoothNumberFileParser parser = new SmoothNumberFileParser(file.getAbsolutePath(), B, N1);
        parser.getExpMatrix().gf2Print();
//        parser.getExpMatrix().nullSpace();
        System.out.println(parser.calculateFactor());
        
//        System.out.println("---expMatrix");
//        parser.getExpMatrix().gf2Print();
        
//        System.out.println("---identity");
//        new LargeGF2Matrix("./temp1").gf2Print();
//        System.out.println("---gaussian");
//        new LargeGF2Matrix("./temp2").gf2Print();
        
//        
//        System.out.println(sieve.isSmooth(new BigInteger("782977848170708394135694655741222").pow(2).subtract(N)));

        
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
