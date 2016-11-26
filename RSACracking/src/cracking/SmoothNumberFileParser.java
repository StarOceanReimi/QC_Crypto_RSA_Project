/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking;

import cracking.algorithms.Factorization;
import cracking.algorithms.LargeGF2Matrix;
import cracking.algorithms.MathOp;
import static cracking.algorithms.MathOp.MINUS_ONE;
import static cracking.algorithms.MathOp.gcd;
import cracking.cluster.SmoothInfo;
import static cracking.utils.Util.error;
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
import java.util.Map.Entry;
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
    private List<TreeMap<BigInteger, Integer>> factors;
    private Map<BigInteger, SmoothInfo> leftOverStack;
    private LargeGF2Matrix expMatrix;
    private static final String DEFAULT_MATRIX_PATH = "./SmoothExpMatrix";
    private boolean useExistingMatrix = false;
    
    public LargeGF2Matrix getExpMatrix() {
        return expMatrix;
    }
    
    public SmoothNumberFileParser(String filePath, int B, BigInteger N) throws FileNotFoundException, IOException {
        file = new RandomAccessFile(filePath, "r");
        relations = new ArrayList<>();
        factors = new ArrayList<>();
        fb = Factorization.fastFactorBase(B, N);
        this.N = N;
        expMatrix = new LargeGF2Matrix(fb.length+226, fb.length+1, DEFAULT_MATRIX_PATH);
        leftOverStack = new HashMap<>();
        parse();
    }
    
    public SmoothNumberFileParser ignoreCollectingMatrix() throws IOException {
        useExistingMatrix = true;
        return this;
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
    
    
    private TreeMap<BigInteger, Integer> addPower(TreeMap<BigInteger, Integer> f1, TreeMap<BigInteger, Integer> f2) {
        for(Entry<BigInteger, Integer> entry : f2.entrySet()) {
            if(f1.containsKey(entry.getKey())) 
                f1.put(entry.getKey(), f1.get(entry.getKey())+entry.getValue());
            else
                f1.put(entry.getKey(), entry.getValue());
        }
        return f1;
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
                    factors.add(primeFactors(info));
//                    System.out.println("x1="+info.getX()+primeFactors(info));
                    if(!useExistingMatrix) {
                        int[] expVec = factorsToExpVector(info.getX(), info.getFactors());
//                        System.out.println(Arrays.toString(fb));
//                        System.out.println(Arrays.toString(expVec));
                        expMatrix.rowAdd(i, expVec);
//                        System.out.println(Arrays.toString(expMatrix.getRow(i)));
                    }
                    i++;
                } else {
                    //partial relation
                    if(leftOverStack.containsKey(info.getLeftover())) {
                        SmoothInfo saved = leftOverStack.get(info.getLeftover());
                        if(saved.getX().equals(info.getX()) ||
                           Arrays.equals(saved.getFactors(), info.getFactors())) continue;
                        saved = leftOverStack.remove(info.getLeftover());
                        int row = i++;
                        relations.add(info.getX().multiply(saved.getX()));
                        final TreeMap<BigInteger, Integer> infoFactors = primeFactors(info);
                        TreeMap<BigInteger, Integer> savedFactors = primeFactors(saved);
                        addPower(savedFactors, infoFactors);
                        factors.add(savedFactors);
//                        System.out.println("x1="+saved.getX()+primeFactors(saved)+", x2="+info.getX()+primeFactors(info));
                        if(!useExistingMatrix) {
                            int[] savedVec = factorsToExpVector(saved.getX(), saved.getFactors());
                            int[] vec = factorsToExpVector(info.getX(), info.getFactors());
//                            System.out.println(Arrays.toString(fb));
//                            System.out.println(Arrays.toString(savedVec));
//                            System.out.println(Arrays.toString(vec));
                            expMatrix.rowAdd(row, savedVec);
                            expMatrix.rowAdd(row, vec);
//                            System.out.println(Arrays.toString(expMatrix.getRow(row)));
                        }
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
    
    private TreeMap<BigInteger, Integer> primeFactors(SmoothInfo info) {
        TreeMap<BigInteger, Integer> fMap = new TreeMap<>();
        BigInteger qx = Qx.apply(info.getX(), N).abs();
        for(int p : fb) {
            BigInteger bp = valueOf(p);
            while(MathOp.divides(bp, qx)) {
                fMap.put(bp, fMap.getOrDefault(bp, 0)+1);
                qx = qx.divide(bp);
            }
        }
        if(info.getLeftover() != null) { 
            fMap.put(info.getLeftover(), 1);
            qx = qx.divide(info.getLeftover());
        }
        if(!qx.equals(ONE)) {
            error("Impossible smooth info: %s", info);
        }
        return fMap;
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
        if(qx.equals(MINUS_ONE)) fMap.put(MINUS_ONE, 1);
        else if(!qx.equals(ONE)) fMap.put(qx, 1);
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
        LinkedList results = new LinkedList<>();
//        stream(nullSpace).forEach(row->System.out.println(Arrays.toString(row)));
        for(int i=0, n=nullSpace.length; i<n; i++) {
            BigInteger prodX = ONE;
            final TreeMap<BigInteger, Integer> fMap = new TreeMap<>();
            for(int j=0; j<relations.size(); j++) {
                if(nullSpace[i][j] == 1) {
                    BigInteger x = relations.get(j);
                    addPower(fMap, factors.get(j));
                    prodX = prodX.multiply(x);
                }
            }
            if(fMap.isEmpty()) continue;
            System.out.println("-----"+fMap);
            fMap.replaceAll((f,pow)->pow/2);
            System.out.println("*****"+fMap);
            Stream<BigInteger> fStream = fMap.keySet().stream();
            BigInteger sqrtQx = fStream.reduce(ONE, (v,f)->v.multiply(f.pow(fMap.get(f))));
            BigInteger candidate = gcd(N, sqrtQx.add(prodX).abs());
            System.out.println(candidate);
            if(!candidate.equals(ONE) && !candidate.equals(N)) {
                factorOne = candidate;
                break;
            }
            System.out.println("-----");
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
    
    private static void saveMatrix(int[][] data, String file) throws IOException {
        LargeGF2Matrix matrix = new LargeGF2Matrix(data.length, data[0].length, file);
        for(int i=0; i<matrix.getRows(); i++)
            matrix.rowAdd(i, data[i]);
        matrix.close();
        
    }
    
    public static void main(String[] args) throws IOException {
        
//        System.out.println(parser.expMatrix.getRows());
//        int[][] nullSpace = LargeGF2Matrix.nullSpaceBy(new LargeGF2Matrix("ExpMatrix"), new LargeGF2Matrix("identity"));
//        System.out.println(nullSpace.length);
        System.out.println("data collection..");
        SmoothNumberFileParser parser = new SmoothNumberFileParser("./SmoothNumber0", 1_000_000, Main.TARGET);
        int[][] nullSpace = parser.expMatrix.nullSpace();
        System.out.println("saving null space..");
        saveMatrix(nullSpace, "./nullSpace");
        System.out.println("calculating...");
        System.out.println(parser.calculateFactor(Main.TARGET, nullSpace));
        
//        System.out.println("saving null space...");
//        for(int i=0; i<nullSpace.length; i++)
//            nullSpaceMatrix.rowAdd(i, nullSpace[i]);
//        System.out.println("Done");
//        System.out.println(nullSpace.length);
//        System.out.println(parser.calculateFactor(Main.TARGET, nullSpace));

        
        
    }
}
