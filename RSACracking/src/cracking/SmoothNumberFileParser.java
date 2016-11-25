/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking;

import cracking.algorithms.Factorization;
import cracking.algorithms.LargeGF2Matrix;
import cracking.algorithms.MathOp;
import static cracking.algorithms.MathOp.gcd;
import cracking.cluster.SmoothInfo;
import static cracking.utils.Util.error;
import static cracking.utils.Util.mustPositive;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import static java.util.stream.IntStream.concat;
import java.util.stream.Stream;

/**
 *
 * @author Li
 */
public class SmoothNumberFileParser {
    
    private static final BiFunction<BigInteger, BigInteger, BigInteger> Qx = (x, N)->x.pow(2).subtract(N);

    private RandomAccessFile file;
    private int[] fb;
    private BigInteger N;
    private List<BigInteger> relations;
    private List<int[]>      factors;
    private Map<BigInteger, SmoothInfo> leftOverStack;
    private LargeGF2Matrix expMatrix;
    private static final String DEFAULT_MATRIX_PATH = "./SmoothExpMatrix";

    public LargeGF2Matrix getExpMatrix() {
        return expMatrix;
    }
    
    public SmoothNumberFileParser(String filePath, int B, BigInteger N) throws FileNotFoundException, IOException {
        file = new RandomAccessFile(filePath, "r");
        relations = new ArrayList<>();
        factors   = new ArrayList<>();
        fb = Factorization.fastFactorBase(B, N);
        this.N = N;
        expMatrix = new LargeGF2Matrix(fb.length+1, fb.length+1, DEFAULT_MATRIX_PATH);
        leftOverStack = new HashMap<>();
        parse();
    }
    
    private int[] factorsToExpVector(BigInteger x, final int[] factors) {
        BigInteger qx = Qx.apply(x, N);
        IntStream result = qx.compareTo(ZERO) > 0 ? IntStream.of(0) : IntStream.of(1);
        return concat(result, stream(fb).map(p->binarySearch(factors, p) < 0 ? 0 : 1)).toArray();
    }
    
    private int[] expVectorToFactors(final int[] expVector) {
        IntStream.Builder result = IntStream.builder();
        for(int i=1; i<expVector.length; i++) {
            if(expVector[i] == 1) result.accept(fb[i-1]);
        }
        return result.build().toArray();
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
//                    factors.add(info.getFactors());
                    int[] expVec = factorsToExpVector(info.getX(), info.getFactors());
                    expMatrix.rowAdd(i++, expVec);
                } else {
                    //partial relation
                    if(leftOverStack.containsKey(info.getLeftover())) {
                        SmoothInfo saved = leftOverStack.get(info.getLeftover());
                        if(saved.getX().equals(info.getX())) continue;
                        saved = leftOverStack.remove(info.getLeftover());
                        int row = i++;
                        relations.add(info.getX().multiply(saved.getX()));
                        int[] savedVec = factorsToExpVector(saved.getX(), saved.getFactors());
                        int[] vec = factorsToExpVector(info.getX(), info.getFactors());
                        expMatrix.rowAdd(row, savedVec);
                        expMatrix.rowAdd(row, vec);
//                        factors.add(expVectorToFactors(expMatrix.getRow(row)));
                    } else {
                        leftOverStack.put(info.getLeftover(), info);
                    }
                }
            }
            System.out.println("relations: "+i);
        } catch (IOException ex) {
            error("smooth file io expection. %s", ex.getMessage());
        }
        
    }
    
    private Map<BigInteger, Integer> factorQx(BigInteger qx) {
        Map<BigInteger, Integer> fMap = new TreeMap<>();
        for(int p : fb) {
            BigInteger bp = valueOf(p);
            while(qx.mod(bp).equals(ZERO)) {
                fMap.put(bp, fMap.getOrDefault(bp, 0)+1);
                qx = qx.divide(bp);
            }
        }
        return fMap;
    }
    
    private int factorPower(BigInteger qx, int p) {
        int power = 0;
        BigInteger bp = valueOf(p);
        while(MathOp.divides(bp, qx)) {
            power++;
            qx = qx.divide(bp);
        }
        System.out.println(power);
        return power;
    }
    
    public List<BigInteger> calculateFactor(BigInteger N, int[][] nullSpace) {
        BigInteger factorOne = ONE;
        System.out.println(relations.size());
        System.out.println(nullSpace.length);
        LinkedList results = new LinkedList<>();
        for(int i=0, n=nullSpace.length; i<n; i++) {
            BigInteger prodX = ONE;
            BigInteger prodQx = ONE;
//            final TreeMap<BigInteger, Integer> fMap = new TreeMap<>();
            for(int j=0; j<nullSpace[i].length; j++) {
                if(nullSpace[i][j] == 1) {
                    BigInteger x = relations.get(j);
                    BigInteger qx = Qx.apply(x, N);
                    prodX = prodX.multiply(x);
                    prodQx = prodQx.multiply(qx);
                }
            }
            Map<BigInteger, Integer> prodFactors = factorQx(prodQx);
            System.out.println(prodFactors);
//            System.out.println(fMap);
            prodFactors.replaceAll((f,pow)->pow/2);
            Stream<BigInteger> fStream = prodFactors.keySet().stream();
            BigInteger sqrtQx = fStream.reduce(ONE, (v,f)->v.multiply(f.pow(prodFactors.get(f))));
            BigInteger candidate = gcd(N, sqrtQx.add(prodX).abs());
            if(!candidate.equals(ONE) && !candidate.equals(N)) {
                factorOne = candidate;
                break;
            }
        }
        if(factorOne.equals(ONE)) return results;
        results.add(factorOne);
        results.add(N.divide(factorOne));
        return results;
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
        SmoothNumberFileParser parser = new SmoothNumberFileParser("./SmoothNumber4", 1_000_000, Main.TARGET);
        System.out.println(parser.relations.size());
//        int[][] nullSpace = parser.expMatrix.nullSpace();
//        System.out.println(parser.calculateFactor(Main.TARGET, nullSpace));
    }
}
