/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import static cracking.utils.Util.error;
import static cracking.utils.Util.mustPositive;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

/**
 *
 * @author Li
 */
public class MathOp {
    
    public static BigInteger TWO = new BigInteger("2");
    
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
        mustPositive(a, b);
        BigInteger[] triple = new BigInteger[3];
        if(a.equals(ZERO)) {
            triple[0] = b;
            triple[1] = ZERO;
            triple[2] = ONE;
            return triple;
        } else {
            triple = exGcd(a, b);
            BigInteger x = triple[1];
            BigInteger y = triple[2];
            triple[1] = y.subtract(b.mod(a).multiply(x));
            triple[2] = x;
            return triple;
        }
    }
    
    public static BigInteger modInverse(BigInteger m, BigInteger n) {
        mustPositive(m, n);
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
        mustPositive(a, e, n);
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
    
}
