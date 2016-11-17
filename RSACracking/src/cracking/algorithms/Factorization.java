/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import cracking.Main;
import static cracking.algorithms.MathOp.MINUS_ONE;
import static cracking.algorithms.MathOp.TWO;
import static cracking.algorithms.MathOp.expMod;
import static cracking.algorithms.MathOp.gcd;
import static cracking.algorithms.MathOp.modInverse;
import static cracking.algorithms.MathOp.newtonSqrt;
import cracking.algorithms.Primes.EratosthenesPrimeGenerator;
import static cracking.utils.Util.error;
import java.math.BigDecimal;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 *
 * @author Li
 */
public class Factorization {
    
    public static BigInteger pollardRho(BigInteger n) {
        BigInteger a = TWO, b = TWO, d = ONE;
        Function<BigInteger, BigInteger> f = x->x.pow(2).add(ONE).mod(n);
        while(d.equals(ONE)) {
            a = f.apply(a);
            b = f.apply(f.apply(b));
            if(a.equals(b) || !d.equals(ONE)) break;
            d = gcd(a.subtract(b).abs(), n);
        }
        return d;
    }
    
    public static boolean eulerCriterion(BigInteger a, BigInteger p) {
        return expMod(a, p.subtract(ONE).divide(TWO), p).equals(ONE);
    }
    
    public static LinkedList<BigInteger> factorBase(int piB, BigInteger N) {
        Iterator<BigInteger> gen = new EratosthenesPrimeGenerator().gen();
        LinkedList<BigInteger> factorBase = new LinkedList<>();
        while(piB-- > 0) {
            BigInteger p = gen.next();
            if(eulerCriterion(N, p)) factorBase.add(p);
        }
        return factorBase;
    }
    
    public static LinkedList<BigInteger> factorBase(BigInteger B, BigInteger N) {
        Iterator<BigInteger> gen = new EratosthenesPrimeGenerator().gen();
        LinkedList<BigInteger> factorBase = new LinkedList<>();
        while(true) {
            BigInteger p = gen.next();
            if(p.compareTo(B) > 0) break;
            if(eulerCriterion(N, p)) factorBase.add(p);
        }
        return factorBase;
    }
    
    private static BigInteger[] findRoot(BigInteger n, BigInteger p, int degree) {
        return findRoot(n, p, degree, false);
    }
    
    private static BigInteger[] findRoot(BigInteger n, BigInteger p, int degree, boolean check) {
        if(check)
            if(!eulerCriterion(n, p)) error("No solution.");
        BigInteger[] roots = new BigInteger[degree];
        BigInteger modular = n.mod(p);
        int i = 0;
        int cnt = p.equals(TWO) ? 1 : 0;
        while (true) {
            try {
                BigDecimal root = newtonSqrt(p.multiply(valueOf(i)).add(modular)).setScale(0);
                roots[cnt] = root.toBigInteger();
                if(++cnt == degree) break;
            } catch(ArithmeticException ae) {
                //round necessary. not a square root
            }
            i++;
        }
        
        return roots;
    }
    
    private static BigInteger smallestMatch(BigInteger start, BigInteger end, BigInteger p, BigInteger modular) {
        BigInteger diff = start.subtract(end).abs();
        while(diff.compareTo(ZERO) > 0) {
            if(start.mod(p).equals(modular)) {
                return start;
            }
            start = start.add(ONE);
            diff = diff.subtract(ONE);
        }
        error("No match with given interval.");
        return null;
    }
    
    private static BigInteger henselLifting(BigInteger x, BigInteger p, BigInteger r) {
        BigInteger inv = modInverse(TWO.multiply(x), p);
        BigInteger tmp = r.subtract(x.pow(2)).divide(p);
        BigInteger t = inv.multiply(tmp).mod(p);
        return t.multiply(p).add(x);
    }
    
    public static Map<BigInteger, List<BigInteger>> factorSieve(BigInteger N, BigInteger from, BigInteger to, List<BigInteger> factorBase) {
        Map<BigInteger, List<BigInteger>> sieve = new HashMap<>();
        
        for(BigInteger p : factorBase) {
            try {
                
                BigInteger[] roots = null;
                if(p.equals(TWO))
                    roots = new BigInteger[] { ONE };
                else 
                    roots = MathOp.shanksTonelli(N, p);
                
                for(BigInteger r : roots) {
                    try {
                        BigInteger s = smallestMatch(from, to, p, r);
                        while(s.compareTo(to) <= 0) {
                            List<BigInteger> pfs = sieve.get(s);
                            if(pfs == null) {
                                pfs = new LinkedList<>();
                                sieve.put(s, pfs);
                            }
                            pfs.add(p);
                            s = s.add(p);
                        }
                    } catch (RuntimeException ex) {
                        //no match continue
                    }
                }
                
            } catch (RuntimeException ex) {
                //no root solution continue
            }
        }
        
        return sieve;
    }
    
    public static int[] expVector(int index, TreeMap<BigInteger, Integer> factors, List<BigInteger> factorBase) {
        return expVector(index, factors, factorBase, 2);
    }
    
    public static int[] expVector(int index , TreeMap<BigInteger, Integer> factors, List<BigInteger> factorBase, int gf) {
        int[] vec = new int[factorBase.size()+1];
        Arrays.fill(vec, 0);
        vec[0] = index;
        for(int i=0; i<factorBase.size(); i++) {
            BigInteger p = factorBase.get(i);
            if(factors.containsKey(p)) {
                vec[i+1] = factors.get(p) % gf;
            }
        }
        return vec;
    }
    
    public static TreeMap<BigInteger, Integer> getPrimeFactors(BigInteger x, BigInteger N, Map<BigInteger, List<BigInteger>> sieve) {
        TreeMap<BigInteger, Integer> factors = new TreeMap<>();
        List<BigInteger> primes = sieve.get(x);
        if(primes == null) error("x[%s] is not in sieves.", x);
        BigInteger qx = x.pow(2).subtract(N);
        if(qx.compareTo(ZERO) < 0) {
            factors.put(MINUS_ONE, 1);
            qx = qx.abs();
        }
        
        for(BigInteger p : primes) {
            while(qx.mod(p).equals(ZERO)) {
                if(factors.containsKey(p)) factors.put(p, factors.get(p)+1);
                else factors.put(p, 1);
                qx = qx.divide(p);
            }
        }
        if(!qx.equals(ONE)) {
            factors.put(qx, 1);
        }
        return factors;
    }
    
    private static BigInteger bitsTransform(int[] vector) {
        BigInteger bits = ZERO;
        for(int i=vector.length-1; i>=0; i--) {
            bits = bits.or(BigInteger.valueOf(vector[i]));
            if(i != 0) bits = bits.shiftLeft(1);
        }
        return bits;
    }
    
    public static Map<BigInteger, TreeMap<BigInteger, Integer>> largePrimeVarious(
            BigInteger N, BigInteger primeUpperBound, 
            Map<BigInteger, List<BigInteger>> sieve)  {
        Map<BigInteger, TreeMap<BigInteger, Integer>> mathched = new TreeMap<>();
        for(BigInteger k : sieve.keySet()) {
            TreeMap<BigInteger, Integer> factors = getPrimeFactors(k, N, sieve);
            if(factors.lastKey().compareTo(primeUpperBound) <= 0 &&
               factors.firstKey().compareTo(MINUS_ONE) >= 0) {
                mathched.put(k, factors);
            }
        }
        return mathched;
    }
    
    
    
    public static void main(String[] args) throws InterruptedException {
        BigInteger B = valueOf(15000);
//        int piOfB = 4000;
//        BigInteger N = Main.TARGET;
        BigInteger N = new BigInteger("6275815110957813119593022531213");
//        final BigInteger N = new BigInteger("19438380807575007722167");
//        
//        long b1 = 17900041427L;
//        long b2 = 1085940548621L;
        
        LinkedList<BigInteger> fb = factorBase(B, N);
        BigInteger M = valueOf(8_000_000);
        BigInteger N_SQRT = newtonSqrt(N).setScale(0, 2).toBigInteger();
        BigInteger BEGIN = N_SQRT.subtract(M);
        BigInteger END  = N_SQRT.add(M);
//        
//        
        Map<BigInteger, List<BigInteger>> sieve = factorSieve(N, BEGIN, END, fb);
        Map<BigInteger, TreeMap<BigInteger,Integer>> factorsList = largePrimeVarious(N, fb.getLast(), sieve);
        System.out.println(fb.size());
        System.out.println(factorsList.size());
//        
//        factorsList.forEach((k, v)->{ System.out.println(k+","+v);});

//        for(int i=0; i<times; i++)
//            aInt.incrementAndGet();
        
//        for(int i=0; i<4; i++)
//            new Thread(()->{
//                for(int j=0; j<smallTimes; j++)
//                    aInt.incrementAndGet();
//            }).start();
                
//        final Iterator<BigInteger> xIter = factorsList.keySet().iterator();
//        BigInteger[] xIndecies = range(0, factorsList.size()).mapToObj(i->xIter.next()).toArray(BigInteger[]::new);
//        
//        int[][] matrix = new int[fb.size()][];
//        for(int i=0; i<matrix.length; i++) {
//            matrix[i] = expVector(i, factorsList.get(xIndecies[i]), fb);
//        }
//        Function<BigInteger, BigInteger> Qx = (x)->x.pow(2).subtract(N);
//        
//        int[][] result = gaussianElimilationF2(matrix, 1);
//        for(int[] row : result) {
//            System.out.println(Arrays.toString(row));
//        }
        
//        List<Integer> result = Matrix.searchZeroVector(matrix);
//        BigInteger xResult = ONE, qxResult = ONE;
//        for(int index : result) {
//            BigInteger x = xIndecies[index];
//            xResult = xResult.multiply(x);
//            BigInteger qx = Qx.apply(x);
//            qxResult = qxResult.multiply(qx);
//        }
        
        
//        int[][] matrix = buildMatrix(N, sieve, fb);
//        Iterator<BigInteger> iter = sieve.keySet().iterator();
//        for(int[] vec : matrix) {
//            BigInteger x = iter.next();
//            BigInteger qx = x.pow(2).subtract(N);
//            System.out.println(String.format("x=%s, qx=%s, vecNum=%s ", x, qx, bitsTransform(vec)));
//        }
        
//          BigInteger qx1 = new BigInteger("9957466568168883057453647528079435857");
//          BigInteger x1 = new BigInteger("22270180452285531602308142748790532");
//
//          BigInteger qx2 = new BigInteger("10135628011787167310272112670069760129");
//          BigInteger x2 = new BigInteger("22270180452285531602308142748790536");
//
//          System.out.println(qx1.multiply(qx2).subtract(x1.multiply(x2)).gcd(N));
  
        
//        BigInteger r = valueOf(37987);
//        BigInteger p = valueOf(239);
//        System.out.println(r.mod(p));
//        BigInteger x = newtonSqrt(r.mod(p)).setScale(0).toBigInteger();
//        System.out.println(Arrays.toString(findRoot(N, valueOf(13), 2)));
//        
//        System.out.println(henselLifting(x, p, r));
    }
}
