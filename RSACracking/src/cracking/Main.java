/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 * @author Li
 */
public class Main {

    public static BigInteger TARGET = new BigInteger("495960937377360604920383605744987602701101399399359259262820733407167");
    
    
    public static void main(String[] args) {
        
        List<Integer> list = IntStream.range(0, 100).collect(ArrayList::new, List::add, List::addAll);
        System.out.println(list);
        
        
    }
}
