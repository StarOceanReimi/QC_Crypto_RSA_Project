/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking;

import cracking.algorithms.Factorization;
import cracking.algorithms.LargeGF2Matrix;
import cracking.cluster.SmoothInfo;
import static cracking.utils.Util.error;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.stream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Li
 */
public class SmoothNumberFileParser {

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
        return stream(fb).map(p->binarySearch(factors, p) == -1 ? 0 : 1).toArray();
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
    
    
    public static void main(String[] args) throws IOException {
        
        SmoothNumberFileParser parser = new SmoothNumberFileParser("./SmoothNumber4");
        long start = System.currentTimeMillis();
        System.out.println(parser.relations.size());
        System.out.printf("building matrix takes %.1fms\n", (System.currentTimeMillis()-start)/(double)1000);
        start = System.currentTimeMillis();
        parser.expMatrix.nullSpace();
        System.out.printf("calculating null space takes %.1fms\n", (System.currentTimeMillis()-start)/(double)1000);
    }

}
