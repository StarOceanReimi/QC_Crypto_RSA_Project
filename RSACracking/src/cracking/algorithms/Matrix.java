/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cracking.algorithms;

import static cracking.utils.Util.error;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import static java.util.stream.IntStream.range;

/**
 *
 * @author Li
 */
public class Matrix {

    public static double[][] gaussianElimilation(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        if(cols < rows) 
            error("cant do gaussian elimilation on matrix %dx%d", rows, cols);
        double[][] eliminated = new double[rows][];
        List<Integer> rowCalculated = range(0, rows).boxed().collect(toList());
        int newRow = 0;
        for(int c=0; c<cols; c++) {
            final int col = c;
            BiConsumer<LinkedList, Integer> lead1Test = (L, row)-> { if(matrix[row][col]!= 0) L.add(row); };
            LinkedList<Integer> leftMostNonZeroRows = rowCalculated.stream().collect(LinkedList::new, lead1Test, List::addAll);
            if(leftMostNonZeroRows.isEmpty()) continue;
            int pivot = leftMostNonZeroRows.pollFirst();
            Array.set(eliminated, newRow++, matrix[pivot]);
            rowCalculated.remove(0);
            for(int rest : leftMostNonZeroRows) {
                double multiplyer = matrix[rest][c]/matrix[pivot][c];
                for(int i=c; i<cols; i++) {
                    matrix[rest][i] = matrix[rest][i] - matrix[pivot][i]*multiplyer;
                }
            }
        }
        return eliminated;
    }
    
    public static int[][] gaussianElimilationF2(int[][] matrix) {
        return gaussianElimilationF2(matrix, 0);
    }
    
    public static List<Integer> searchZeroVector(int[][] expMatrix) {
        int rows = expMatrix.length;
        int cols = expMatrix[0].length;
        if(cols < rows) 
            error("cant do gaussian elimilation on matrix %dx%d", rows, cols);
        int[][] matrix = expMatrix.clone();
        
        int[] marker = range(0, rows).map(i->0).toArray();
        Map<Integer, List<Integer>> track = new HashMap<>();
        for(int c=1; c<cols; c++) {
            final int col = c;
            ObjIntConsumer<LinkedList> lead1Test = (L, row)-> { if(matrix[row][col] != 0 && marker[row]==0) L.add(row); };
            LinkedList<Integer> leftMost1Rows = range(0, rows).collect(LinkedList::new, lead1Test, List::addAll);
            if(leftMost1Rows.isEmpty()) continue;
            int pivot = leftMost1Rows.pollFirst();
            marker[pivot] = 1;
            while(!leftMost1Rows.isEmpty()) {
                int restRow = leftMost1Rows.pollFirst();

                List<Integer> stack = track.get(restRow);
                if(stack == null) {
                    stack = new LinkedList<>();
                    track.put(restRow, stack);
                }
                for(int j=1; j<cols; j++) {
                    matrix[restRow][j] ^= matrix[pivot][j];
                }
                    
                stack.add(matrix[pivot][0]);
                if(Arrays.stream(matrix[restRow]).skip(1).allMatch(x->x==0)) {
                    stack.add(restRow);
                    return stack;
                }
            }
            
        }
        return null;
    }
    
    public static int[][] gaussianElimilationF2(int[][] matrix, int startColumn) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        if(cols < rows) 
            error("cant do gaussian elimilation on matrix %dx%d", rows, cols);
        int[][] elimilated = new int[matrix.length][];
        int[] marker = range(0, rows).map(i->0).toArray();
        int newRow = 0;
        for(int c=startColumn; c<cols; c++) {
            final int col = c;
            ObjIntConsumer<LinkedList> lead1Test = (L, row)-> { if(matrix[row][col] != 0 && marker[row]==0) L.add(row); };
            LinkedList<Integer> leftMost1Rows = range(0, rows).collect(LinkedList::new, lead1Test, List::addAll);
            if(leftMost1Rows.isEmpty()) continue;
            int pivot = leftMost1Rows.pollFirst();
            marker[pivot] = 1;
            elimilated[newRow++] = matrix[pivot].clone();
            while(!leftMost1Rows.isEmpty()) {
                int restRow = leftMost1Rows.pollFirst();
                for(int j=startColumn; j<cols; j++)
                    matrix[restRow][j] ^= matrix[pivot][j];
            }
        }

        return elimilated;
    }
    
    public static void main(String[] args) {
        int N = 4000;
        Random rand = new Random();
        IntFunction<int[]> bitsGen = i->range(0, i).map(j->rand.nextInt(2)).toArray();
        int[][] bitsMatrix = range(0, N).mapToObj(i->bitsGen.apply(N)).toArray(int[][]::new);
        gaussianElimilationF2(bitsMatrix);
        
    }
}
