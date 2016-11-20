/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import static cracking.algorithms.MathOp.TWO;
import static cracking.algorithms.MathOp.expMod;
import static cracking.algorithms.MathOp.gcd;
import static cracking.utils.Util.intToBigInteger;
import static cracking.utils.Util.randomBigInteger;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author Li
 */
public class Primes {
    
    public static class RandomPrimeGenerator {

        private static final BigInteger PRIME_PRODUCT = BigInteger.valueOf((long)2*3*5*7*11*13*17*19*23*29*31*37*41*43*47);
        public static final Function<BigInteger, BigInteger> ODD_FUNC = b-> {
            if(b.and(ONE).equals(ONE)) b = b.add(TWO);
            else b = b.add(ONE);
            while(!gcd(b, PRIME_PRODUCT).equals(ONE)) {
                b = b.add(TWO);
            }
            return b;
        };
                
        private static Random RAND = new Random();

        public Iterator<BigInteger> gen() {
            return gen(RAND.nextInt());
        }
        
        public Iterator<BigInteger> gen(final int bits) {
            return gen(bits, ODD_FUNC);
        }
        
        public Iterator<BigInteger> gen(final int bits, Function<BigInteger, BigInteger> func) {
            return new Iterator<BigInteger>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public BigInteger next() {
                    BigInteger random = randomBigInteger(bits);
                    if(random.and(ONE).equals(ZERO)) random.add(ONE);
                    while(true) {
                        if(millerRabinTest(random)) return random;
                        random = func.apply(random);
                    }
                }
            };
        }
        
        public Iterator<BigInteger> gen(final BigInteger start) {
            return new Iterator<BigInteger>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public BigInteger next() {
                    BigInteger init = start;
                    if(init.and(ONE).equals(ZERO)) init.add(ONE);
                    while(true) {
                        if(millerRabinTest(init)) return init;
                        init = ODD_FUNC.apply(init);
                    }
                }
            };
        }
        
    }
    
    public static class PrimitiveEratosPrimeGenerator {
        
        
        public PrimitiveEratosPrimeGenerator() {
        }
        
        public Iterator<Integer> gen() {
            
            return new Iterator<Integer>() {
                int q = 2;
                HashMap<Integer, List<Integer>> dict = new HashMap<>();
                
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Integer next() {
                    int prime = 0;
                    while(prime == 0) {
                        if(!dict.containsKey(q)) {
                            prime = q;
                            List<Integer> factors = new LinkedList<>();
                            factors.add(q);
                            dict.put(q*q, factors);
                        } else {
                            for(int f : dict.get(q)) {
                                List<Integer> factors = dict.get(q+f);
                                if(factors == null) {
                                    factors = new LinkedList<>();
                                    dict.put(q+f, factors);
                                }
                                factors.add(f);
                            }
                            dict.remove(q);
                            
                        }
                        q += 1;
                    }
                    return prime;
                }
            };
     
        }
        
    }
    
    public static class EratosthenesPrimeGenerator {
        
        private BiPredicate<BigInteger, BigInteger> criteria;

        public EratosthenesPrimeGenerator(BiPredicate<BigInteger, BigInteger> criteria) {
            this.criteria = criteria;
            if(this.criteria == null) {
                this.criteria = (p, c)->true;
            }
        }

        public EratosthenesPrimeGenerator() {
            this(null);
        }
        
        public Iterator<BigInteger> gen() {
            return new Iterator<BigInteger>() {
                
                Map<BigInteger, List<BigInteger>> table = new HashMap<>();
                
                BigInteger q = TWO;
                BigInteger c = ONE;
                
                private BigInteger nextPrime() {
                    BigInteger t = q;
                    while(true) {
                        if(!table.containsKey(t)) return t;
                        t = t.add(ONE);
                    }
                }
                
                @Override
                public boolean hasNext() {
                    return criteria.test(nextPrime(), c);
                }

                @Override
                public BigInteger next() {
                    BigInteger ret = ZERO;
                    while(ret.equals(ZERO)) {
                        if(!table.containsKey(q)) {
                            ret = q;
                            c = c.add(ONE);
                            List<BigInteger> factors = new LinkedList<>();
                            factors.add(q);
                            table.put(q.pow(2), factors);
                        } else {
                            for(BigInteger f : table.get(q)) {
                                BigInteger composite = q.add(f);
                                if(!table.containsKey(composite)) {
                                    List<BigInteger> factors = new LinkedList<>();
                                    table.put(composite, factors);
                                } 
                                table.get(composite).add(f);
                            }
                            table.remove(q);
                        }
                        q = q.add(ONE);
                    }
                    return ret;
                }
            };
        }
    }
    
    public static int[] eratosthenesSieve(int n) {
        int[] sieve = new int[n+1];
        Arrays.fill(sieve, 0);
        for(int i=2; i<=n; i++) {
            if(sieve[i] == 0) {
                sieve[i] = i;
                for(int j=i; j<=n; j+=i) {
                    sieve[j] = sieve[i] != 0 ? sieve[i] : i;
                }
            }
        }
        return sieve;
    }
    
    public static TreeMap<BigInteger, Integer> findFactorsInSieve(int n, int[] sieve) {
        TreeMap<BigInteger, Integer> factors = new TreeMap<>();
        BigInteger N = intToBigInteger(n);
        while(N.compareTo(ONE) > 0) {
            BigInteger p = intToBigInteger(sieve[N.intValue()]);
            if(factors.containsKey(p)) factors.put(p, factors.get(p)+1);
            else factors.put(p, 1);
            if(p.equals(N)) break;
            N = N.divide(p);
        }
        return factors;
    }
    
    public static boolean millerRabinTest(BigInteger n) {
        return millerRabinTest(n, 50);
    }
    
    public static boolean millerRabinTest(BigInteger n, int k) {
        if(n.compareTo(TWO) < 0) return false;
        if(n.compareTo(TWO.pow(2)) < 0) return true;
        if(n.and(ONE).equals(ZERO)) return false;
        int s = 0;
        final BigInteger N_MINUS_ONE = n.subtract(ONE);
        BigInteger d = N_MINUS_ONE;
        while(d.and(ONE).equals(ZERO)) {
            s += 1;
            d = d.shiftRight(1);
        }
        final BigInteger exp = d;
        final int trial = s;
        Supplier<Boolean> test = ()-> { 
            BigInteger randomRoot = randomBigInteger(TWO, n.subtract(TWO));
            BigInteger x = expMod(randomRoot, exp, n);
            if(x.equals(ONE) || x.equals(N_MINUS_ONE)) return true;
            for(int i=0; i<trial; i++) {
                x = x.pow(2).mod(n);
                if(x.equals(ONE)) return false;
                if(x.equals(N_MINUS_ONE)) return true;
            }
            return false;
        };
        for(int i=0; i<k; i++)
            if(!test.get()) return false;
        return true;
    }
    
    
    public static void main(String[] args) {
        RandomPrimeGenerator randomGen = new RandomPrimeGenerator();
        BigInteger b1 = randomGen.gen(20).next();
        BigInteger b2 = randomGen.gen(30).next();
        System.out.println(b1);
        System.out.println(b2);
        BigInteger N = b1.multiply(b2);
        System.out.println(N);
        System.out.println(N.bitLength());

    }
}
