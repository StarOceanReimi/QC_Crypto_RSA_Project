/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.cluster;

import static cracking.utils.Util.error;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

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

    public static SmoothInfo fromString(String info) {
        BigInteger x = null, l = null;
        IntStream.Builder fBuilder = IntStream.builder();
        StringBuilder temp = new StringBuilder();
        StringReader sr = new StringReader(info);
        char space = ' ';
        char comma = ',';
        char arrStart = '[';
        char arrEnd = ']';
        char c;
        try {
            char firstChar = (char) sr.read();
            if(firstChar == '0') sr.skip(1);
            else if(firstChar == '1') {
                while((c = (char)sr.read()) != space) {
                    temp.append(c);
                }
                l = new BigInteger(temp.toString());
                temp = new StringBuilder();
            } else error("error format");
            
            while((c=(char)sr.read()) != space) {
                temp.append(c);
            }
            x = new BigInteger(temp.toString());
            temp = new StringBuilder();
            if((c=(char)sr.read()) != arrStart) error("error format: missing [");
            while((c=(char)sr.read()) != arrEnd) {
                if((int)c == 65535) error("error format: missing ]");
                if(c == space) continue;
                if(c == comma) {
                    fBuilder.accept(Integer.parseInt(temp.toString()));
                    temp = new StringBuilder();
                } else
                    temp.append(c);
            }
        } catch (IOException ex) {
        }
        return new SmoothInfo(fBuilder.build().toArray(), x, l);
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
