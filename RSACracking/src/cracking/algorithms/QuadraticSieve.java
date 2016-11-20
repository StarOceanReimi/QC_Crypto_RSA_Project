/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import static cracking.algorithms.MathOp.legendreSymbol;
import static cracking.algorithms.MathOp.newtonSqrt;
import cracking.algorithms.Primes.PrimitiveEratosPrimeGenerator;
import static cracking.utils.Util.mustPositive;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.round;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
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
    private int threshold;
    private int domain;
    private int smoothApprox;
    private Set<Integer> smoothCandidates;
    
    private Set<BigInteger> bSmoothRef;
    
    int[] factorBase;
    int[] sieve;
    int[][] roots;
    int[][] pos;
    
    int counter = 0;
    
    private void buildFactorBase() {
        IntStream.Builder builder = IntStream.builder();
        Iterator<Integer> gen = new PrimitiveEratosPrimeGenerator().gen();
        while(true) {
            int p = gen.next();
            if(p > B) break;
            if(legendreSymbol(N, valueOf(p)) == 1)
                builder.accept(p);
        }
        factorBase = builder.build().toArray();
    }
    
    private void dataCollection() {
        roots = new int[B+1][];
        pos   = new int[B+1][];
        for(int p : factorBase) {
            BigInteger bp = valueOf(p);
            if(p == 2) {
                roots[p] = new int[]{1};
                pos[p] = new int[] { ONE.subtract(start).mod(bp).intValue() };
            } else {
                BigInteger[] sols = MathOp.shanksTonelli(N, valueOf(p));
                roots[p] = stream(sols).mapToInt(bi->bi.intValue()).toArray();
                pos[p] = stream(roots[p]).map(r->valueOf(r).subtract(start).mod(bp).intValue()).toArray();
            }
        }
    }

    private void buildSieve() {
        dataCollection();
        sieve = new int[domain];
        for(int p : factorBase) {
            int pl = (int)round(log(p));
            int[] rs = pos[p];
            if(p == 2) {
                saveLocation(rs[0], p, pl);
            } else {
                saveLocation(rs[0], p, pl);
                saveLocation(rs[1], p, pl);
            }
        }
    }

    //XXX: trying to find some efficient way to identify
    //     smooth candidates other than trail division
    private void verifySmooth() {
        
    }
    
    private void saveLocation(int r, int p, int pl) {
        int i=0;
        while(true) {
            int l = r+i*p;
            if(l >= domain) break;
            sieve[l] += pl;
            if(abs(sieve[l]-smoothApprox) < threshold) {
                smoothCandidates.add(l);
            }
            i++;
        }
    }

    private void init() {
        threshold = 7;
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

    public void setBSmoothRef(Set<BigInteger> bSmoothRef) {
        this.bSmoothRef = bSmoothRef;
    }
    
    
    private boolean isSmooth(BigInteger candidate) {
        
        for(int p : factorBase) {
            BigInteger bp = valueOf(p);
            while(candidate.mod(bp).equals(ZERO)) {
                candidate = candidate.divide(bp);
            }
        }
        return candidate.equals(ONE);
    }

    @Override
    public void run() {
        if(smoothCandidates == null)
            smoothCandidates = new HashSet<>();
        buildFactorBase();
        buildSieve();
        verifySmooth();
        if(bSmoothRef != null) {
            for(int loc : smoothCandidates) {
                bSmoothRef.add(start.add(valueOf(loc)));
            }
        }
    }

}
