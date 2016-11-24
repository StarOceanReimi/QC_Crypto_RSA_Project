/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.cluster;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 *
 * @author Li
 */
public class SmoothInfo implements Serializable {

    private final int[] factors;
    
    private final BigInteger x;
    
    private final BigInteger leftover;

    public SmoothInfo(int[] factors, BigInteger x, BigInteger leftover) {
        this.factors = factors;
        this.x = x;
        this.leftover = leftover;
    }

    public int[] getFactors() {
        return factors;
    }

    public BigInteger getLeftover() {
        return leftover;
    }

    public BigInteger getX() {
        return x;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(leftover == null) builder.append("0");
        else builder.append("1").append(leftover);
        builder.append(" ");
        builder.append(x);
        builder.append(" ");
        builder.append(Arrays.toString(factors));
        return builder.toString();
    }
}
