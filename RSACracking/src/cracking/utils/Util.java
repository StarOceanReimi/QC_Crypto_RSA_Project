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
    
    private static Random RAND = new Random();
    
    public static void error(String msg, Object... args) {
        throw new RuntimeException(String.format(msg, args));
    }
    
    public static void mustPositive(int... integers) {
        for(int integer : integers)
            if(integer <= 0)
                error("number must be positive, but %d", integer);
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
        if(bits == 1) RAND.nextInt(2);
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
}
