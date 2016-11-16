/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import static cracking.utils.Util.error;
import static cracking.utils.Util.mustNonNegative;
import static cracking.utils.Util.mustPositive;
import java.math.BigDecimal;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import java.util.Arrays;
import java.util.function.Function;

/**
 *
 * @author Li
 */
public class MathOp {
    
    public static final BigInteger TWO = BigInteger.valueOf(2);
    
    public static BigInteger gcd(BigInteger a, BigInteger b) {
        mustPositive(a, b);
        BigInteger r = b;
        while(!a.equals(ZERO)) {
            b = a;
            a = r.mod(a);
            r = b;
        }
        return b;
    }
    
    public static BigInteger[] exGcd(BigInteger a, BigInteger b) {
        mustNonNegative(a, b);
        BigInteger[] triple = new BigInteger[3];
        if(a.equals(ZERO)) {
            triple[0] = b;
            triple[1] = ZERO;
            triple[2] = ONE;
            return triple;
        } else {
            triple = exGcd(b.mod(a), a);
            BigInteger x = triple[1];
            BigInteger y = triple[2];
            triple[1] = y.subtract(b.divide(a).multiply(x));
            triple[2] = x;
            return triple;
        }
    }
    
    public static BigInteger modInverse(BigInteger m, BigInteger n) {
        mustNonNegative(m, n);
        BigInteger[] triple = exGcd(m, n);
        BigInteger gcd = triple[0];
        if(!gcd.equals(ONE)) {
            error("%s do not have inverse over %s", m.toString(), m.toString());
        }
        BigInteger inv = triple[1];
        if(inv.compareTo(ZERO) < 0) {
            return n.subtract(inv);
        }
        return inv;
    }
    
    public static BigInteger expMod(BigInteger a, BigInteger e, BigInteger n) {
        mustPositive(a, n);
        if(e.equals(ZERO)) return ONE;
        else mustPositive(e);
        BigInteger ret = ONE;
        BigInteger base = a;
        while(e.compareTo(ZERO) > 0) {
            if(e.mod(TWO).equals(ONE)) {
                ret = ret.multiply(base).mod(n);
            }
            base = base.pow(2).mod(n);
            e = e.shiftRight(1);
        }
        return ret;
    }
    
    public static BigDecimal newtonSqrt(BigInteger n) {
        return newtonSqrt(n, 10, 20.0D, 150);
    }
    
    public static BigDecimal newtonSqrt(BigInteger n, int percision, double initGuess, int acc) {
        BigDecimal dn = new BigDecimal(n);
        BigDecimal x0 = new BigDecimal(initGuess);
        Function<BigDecimal, BigDecimal> f = x->x.pow(2).subtract(dn);
        Function<BigDecimal, BigDecimal> f_p = x->x.multiply(new BigDecimal(2));
        while(acc-- > 0) {
            x0 = x0.subtract(f.apply(x0).divide(f_p.apply(x0), percision, 5));
        }
        return x0;
    }
    
    
    public static BigInteger[] shanksTonelli(BigInteger a, BigInteger p) {
        if(p.equals(TWO)) { return new BigInteger[] { ONE }; }
        if(legendreSymbol(a, p) == -1) return null; //No solution
        int numOfSolution = p.equals(TWO) ? 1 : 2;
        BigInteger[] solutions = new BigInteger[numOfSolution];
        BigInteger P_MINUS_ONE = p.subtract(ONE);
        BigInteger e = ZERO, s = P_MINUS_ONE;
        while(s.and(ONE).equals(ZERO)) {
            s = s.shiftRight(1);
            e = e.add(ONE);
        }
        BigInteger n = THREE;
        while(true) {
            if(legendreSymbol(n, p) == -1) break;
            n = n.add(ONE);
        }
        BigInteger x = expMod(a, s.add(ONE).divide(TWO), p),
                   b = expMod(a, s, p),
                   g = expMod(n, s, p),
                   r = e.mod(p);
        
        while(true) {
            BigInteger m = ZERO;
            while(true) {
                if(b.equals(ONE)) {
                    m = ZERO;
                    break;
                }
                if(expMod(b, TWO.pow(m.intValue()), p).equals(ONE)) break;
                m = m.add(ONE);
            }
            
            if(m.equals(ZERO)) {
                solutions[0] = x;
                if(solutions.length > 1) {
                    solutions[1] = p.subtract(x);
                }
                return solutions;
            }
            BigInteger gPow = TWO.pow(r.subtract(m).intValue());
            BigInteger gPow_1 = TWO.pow(r.subtract(m).subtract(ONE).intValue());
            BigInteger newG = expMod(g, gPow, p);
            x = expMod(g, gPow_1, p).multiply(x).mod(p);
            b = expMod(g, gPow, p).multiply(b).mod(p);
            g = newG;
            r = m;
        }
    }
    
    final static BigInteger MINUS_ONE = ZERO.subtract(ONE);
    final static BigInteger THREE = BigInteger.valueOf(3);
    final static BigInteger FIVE = BigInteger.valueOf(5);
    final static BigInteger FOUR = BigInteger.valueOf(4);
    final static BigInteger SEVEN = BigInteger.valueOf(7);
    
    
    public static int legendreSymbol(BigInteger a, BigInteger p) {
        
        if(p.and(ONE).equals(ZERO))
            error("p has to be odd, but %s", p);
        
        if(a.mod(p).equals(ZERO)) return 0;
        
        if(a.equals(ONE)) return 1;
        
        if(a.equals(p.subtract(ONE)) || a.equals(ZERO.subtract(ONE))) {
            return p.subtract(ONE).divide(TWO).mod(TWO).equals(ZERO) ? 1 : -1;
        }
        
        if(a.equals(THREE) && !p.equals(THREE)) {
            BigInteger qr = p.mod(BigInteger.valueOf(12));
            if(qr.equals(ONE) || qr.equals(BigInteger.valueOf(11))) return 1;
            if(qr.equals(FIVE) || qr.equals(SEVEN)) return -1;
        }
        
        if(a.equals(FIVE) && !p.equals(FIVE)) {
            BigInteger qr = p.mod(FIVE);
            if(qr.equals(ONE) || qr.equals(FOUR)) return 1;
            if(qr.equals(TWO) || qr.equals(THREE)) return -1;
        }
        
        if(a.equals(TWO)) {
            BigInteger qr = p.mod(BigInteger.valueOf(8));
            if(qr.equals(ONE) || qr.equals(SEVEN)) return 1;
            if(qr.equals(THREE) || qr.equals(FIVE)) return -1;
        }

        if(a.and(ONE).equals(ZERO)) {
            return legendreSymbol(TWO, p) * legendreSymbol(a.divide(TWO), p);
        }
        
        if(a.compareTo(p) < 0) {
            int sign = 1;
            if(a.mod(FOUR).equals(THREE) && p.mod(FOUR).equals(THREE))
                sign = -1;
            return sign*legendreSymbol(p, a);
        } else {

            return legendreSymbol(a.mod(p), p);
        }
    }
}
