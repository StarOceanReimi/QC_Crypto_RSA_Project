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
    
    
    final static BigInteger three = BigInteger.valueOf(3);
    final static BigInteger five = BigInteger.valueOf(5);
    final static BigInteger four = BigInteger.valueOf(4);
    final static BigInteger seven = BigInteger.valueOf(7);
    
    public static int legendreSymbol(BigInteger a, BigInteger p) {
        
        if(p.and(ONE).equals(ZERO))
            error("p has to be odd, but %s", p);
        if(a.mod(p).equals(ZERO))
            error("a cant congruent to 0 mod p");
        
        if(a.equals(p.subtract(ONE)) || a.equals(ZERO.subtract(ONE))) {
            return p.subtract(ONE).divide(TWO).mod(TWO).equals(ZERO) ? 1 : -1;
        }
        
        if(a.equals(three) && !p.equals(three)) {
            BigInteger qr = p.mod(BigInteger.valueOf(12));
            if(qr.equals(ONE) || qr.equals(BigInteger.valueOf(11))) return 1;
            if(qr.equals(five) || qr.equals(seven)) return -1;
        }
        
        if(a.equals(five) && !p.equals(five)) {
            BigInteger qr = p.mod(BigInteger.valueOf(5));
            if(qr.equals(ONE) || qr.equals(four)) return 1;
            if(qr.equals(TWO) || qr.equals(three)) return -1;
        }
        
        if(a.equals(TWO)) {
            BigInteger qr = p.mod(BigInteger.valueOf(8));
            if(qr.equals(ONE) || qr.equals(seven)) return 1;
            if(qr.equals(three) || qr.equals(five)) return -1;
        }

        if(a.and(ONE).equals(ZERO)) {
            return legendreSymbol(TWO, p) * legendreSymbol(a.divide(TWO), p);
        }
        
        if(a.compareTo(p) < 0) {
            int sign = 1;
            if(a.mod(four).equals(three) && p.mod(four).equals(three))
                sign = -1;
            return sign*legendreSymbol(p, a);
        } else {

            return legendreSymbol(a.mod(p), p);
        }
    }
}
