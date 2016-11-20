/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import java.util.Random;

/**
 *
 * @author Li
 */
public class Util {
    
    private final static Random RAND = new Random();
    
    public static void error(String msg, Object... args) {
        throw new RuntimeException(String.format(msg, args));
    }
    
    public static void mustPositive(int... integers) {
        for(int integer : integers)
            if(integer <= 0)
                error("number must be positive, but %d", integer);
    }
    
    public static void mustNonNegative(BigInteger... integers) {
        for(BigInteger integer : integers)
            if(integer.compareTo(ZERO) < 0)
                error("number must be non-negative, but %s", integer.toString());
    }
    
    public static void mustPositive(BigInteger... integers) {
        for(BigInteger integer : integers)
            if(integer.compareTo(ZERO) <= 0)
                error("number must be positive, but %s", integer.toString());
    }
    
    public static BigInteger randomBigInteger(BigInteger from, BigInteger to) {
        BigDecimal diff = new BigDecimal(to.subtract(from));
        BigDecimal min  = new BigDecimal(from);
        return new BigDecimal(RAND.nextDouble()).multiply(diff).add(min).toBigInteger();
    }
    
    public static BigInteger randomBigInteger(int bits) {
        mustPositive(bits);
        if(bits == 1) return BigInteger.valueOf(RAND.nextInt(2));
        BigInteger ret = ONE;
        for(int i=1; i<bits; i++) {
            ret = ret.shiftLeft(1);
            ret = ret.or(intToBigInteger(RAND.nextInt(2)));
        }
        return ret;
    }
    
    public static BigInteger intToBigInteger(int n) {
        return BigInteger.valueOf(n);
    }
    
    public static BigInteger[][] splitRange(int pieces, BigInteger from, BigInteger to) {
        BigInteger chunk = to.subtract(from);
        mustPositive(chunk);
        BigInteger portion = chunk.divide(BigInteger.valueOf(pieces));
        BigInteger[][] ranges = new BigInteger[pieces][];
        for(int i=0; i<pieces; i++) {
            BigInteger[] range = new BigInteger[2];
            range[0] = from;
            from = from.add(portion);
            if(i == pieces-1) {
                from = from.max(to);
            }
            range[1] = from;
            ranges[i] = range;
        }
        return ranges;
    }
}
