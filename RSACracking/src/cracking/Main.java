/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking;

import cracking.algorithms.Factorization;
import java.math.BigInteger;

/**
 *
 * @author Li
 */
public class Main {

    public static BigInteger TARGET = new BigInteger("613054310726032886180943888436325837702226698886723435429939101863");
    
    
    public static void main(String[] args) {
        int[] fb = Factorization.fastFactorBase(1_000_000, TARGET);
        
    }
}
