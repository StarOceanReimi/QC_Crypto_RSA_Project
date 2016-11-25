/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking;

import cracking.algorithms.Factorization;
import cracking.algorithms.LargeGF2Matrix;
import static cracking.algorithms.MathOp.gcd;
import cracking.cluster.SmoothInfo;
import static cracking.utils.Util.error;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 *
 * @author Li
 */
public class SmoothNumberFileParser {
    
    private static final BiFunction<BigInteger, BigInteger, BigInteger> Qx = (x, N)->x.pow(2).subtract(N);

    private RandomAccessFile file;
    private int[] fb;
    private List<BigInteger> relations;
    private Map<BigInteger, SmoothInfo> leftOverStack;
    private LargeGF2Matrix expMatrix;
    private static final String DEFAULT_MATRIX_PATH = "./SmoothExpMatrix";

    public SmoothNumberFileParser(String filePath) throws FileNotFoundException, IOException {
        file = new RandomAccessFile(filePath, "r");
        relations = new ArrayList<>();
        fb = Factorization.fastFactorBase(1_000_000, Main.TARGET);
        expMatrix = new LargeGF2Matrix(fb.length+1, fb.length, DEFAULT_MATRIX_PATH);
        leftOverStack = new HashMap<>();
        parse();
    }
    
    private int[] factorsToExpVector(final int[] factors) {
        return stream(fb).map(p->binarySearch(factors, p) < 0 ? 0 : 1).toArray();
    }
    
    private void parse() throws IOException {
        long len;
        try {
            len = file.length();
            int i = 0;
            int rows = expMatrix.getRows();
            while(file.getFilePointer() < len) {
                if(i >= rows) break;
                SmoothInfo info = SmoothInfo.fromString(file.readLine());
                if(info.getLeftover() == null) { 
                    //full relation
                    relations.add(info.getX());
                    expMatrix.rowAdd(i++, factorsToExpVector(info.getFactors()));
                } else {
                    //partial relation
                    if(leftOverStack.containsKey(info.getLeftover())) {
                        SmoothInfo saved = leftOverStack.remove(info.getLeftover());
                        int row = i++;
                        relations.add(info.getX().multiply(saved.getX()));
                        expMatrix.rowAdd(row, factorsToExpVector(saved.getFactors()));
                        expMatrix.rowAdd(row, factorsToExpVector(info.getFactors()));
                    } else {
                        leftOverStack.put(info.getLeftover(), info);
                    }
                }
            }
        } catch (IOException ex) {
            error("smooth file io expection. %s", ex.getMessage());
        }
        
    }
    
    private Map<BigInteger, Integer> factorQx(BigInteger qx) {
        Map<BigInteger, Integer> fMap = new HashMap<>();
        for(int p : fb) {
            BigInteger bp = valueOf(p);
            while(qx.mod(bp).equals(ZERO)) {
                fMap.put(bp, fMap.getOrDefault(p, 0)+1);
                qx = qx.divide(bp);
            }
        }
        return fMap;
    }
    
    public List<BigInteger> calculateFactor(BigInteger N, int[][] nullSpace) {
        BigInteger factorOne = ONE;
        LinkedList factors = new LinkedList<>();
        for(int i=0, n=nullSpace.length; i<n; i++) {
            BigInteger prodX = ONE;
            BigInteger prodQx = ONE;
            
            for(int j=0; j<nullSpace[i].length; j++) {
                if(nullSpace[i][j] == 1) {
                    BigInteger x = relations.get(j);
                    prodX = prodX.multiply(x);
                    BigInteger qx = Qx.apply(x, N);
                    prodQx = prodQx.multiply(qx);
                }
            }
            Map<BigInteger, Integer> prodFactors = factorQx(prodQx);
            prodFactors.replaceAll((f,pow)->pow/2);
            Stream<BigInteger> fStream = prodFactors.keySet().stream();
            BigInteger sqrtQx = fStream.reduce(ONE, (v,f)->v.multiply(f.pow(prodFactors.get(f))));
            BigInteger candidate = gcd(N, sqrtQx.add(prodX).abs());
            if(!candidate.equals(ONE) && !candidate.equals(N)) {
                factorOne = candidate;
                break;
            }
        }
        if(factorOne.equals(ONE)) return factors;
        factors.add(factorOne);
        factors.add(N.divide(factorOne));
        return factors;
    }
    
    
    public static void printArray(int[] arr, int lineNum) {
        int comma = 0;
        for(int i=0; i<arr.length; i++) {
            if(comma++ != 0) System.out.print(",");
            if(i!=0 && i % lineNum == 0) System.out.println();
            System.out.print(arr[i]);
        }
        System.out.println();
    }
    
    public static void main(String[] args) throws IOException {
        
        SmoothNumberFileParser parser = new SmoothNumberFileParser("./SmoothNumber4");
        File matrixPresentation = new File("./matrixGF2");
        if(matrixPresentation.exists()) {
            matrixPresentation.delete();
        }
        matrixPresentation.createNewFile();
        
        System.out.println(parser.relations.size());
        parser.expMatrix.gf2Print(new PrintStream(matrixPresentation));
        
//        int[][] nullSpace = parser.expMatrix.nullSpace();
//        System.out.println(parser.calculateFactor(Main.TARGET, nullSpace));
    }
}
