/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cracking.algorithms;

import cracking.utils.Util;
import static cracking.utils.Util.error;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;
import static java.util.stream.Collectors.toList;
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
    
    public static int[][] transpose(int[][] matrix) {
        int[][] newMatrix = new int[matrix.length][];
        for(int r=0; r<matrix[0].length; r++) {
            newMatrix[r] = new int[matrix.length];
            Arrays.fill(newMatrix[r], 0);
        }
        for(int r=0; r<matrix.length; r++) {
            for(int c=0; c<matrix[r].length; c++) {
                newMatrix[c][r] = matrix[r][c];
            }
        }
        return newMatrix;
    }
    
    public static int[] cloneCol(int[][] matrix, int col) {
        int[] column = new int[matrix.length];
        for(int r=0; r<matrix.length; r++)
            column[r] = matrix[r][col];
        return column;
    }
    
    public static int[][] identity(int N) {
        int[][] identityM = new int[N][N];
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++)
                if(i==j) identityM[i][j] = 1;
                else identityM[i][j] = 0;
        return identityM;
    }
    
    public static int[][] nullspace(int[][] matrix) {
        return nullspace(matrix, 0);
    }
    
    public static int[][] nullspace(int[][] matrix, int startColumn) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] elimilated = new int[matrix.length][];
        int[] marker = range(0, rows).map(i->0).toArray();
        int[][] identity = identity(rows);
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
                for(int j=startColumn; j<cols; j++) {
                    matrix[restRow][j] ^= matrix[pivot][j];
                }
                for(int j=0; j<identity[restRow].length; j++) {
                    identity[restRow][j] ^= identity[pivot][j];
                }
            }
        }
        List<int[]> nullSpace = new LinkedList<>();
        for(int r=elimilated.length-1; r>=0; r--) {
            if(elimilated[r] == null)
                nullSpace.add(identity[r].clone());
        }
        return (int[][]) nullSpace.toArray(new int[0][0]);
    }
    
    
    
    public static void main(String[] args) {
//        int N = 5;
//        Random rand = new Random();
//        IntFunction<int[]> bitsGen = i->range(0, i).map(j->rand.nextInt(2)).toArray();
//        int[][] bitsMatrix = range(0, N).mapToObj(i->bitsGen.apply(N)).toArray(int[][]::new);

        BigInteger[][] ranges = Util.splitRange(8, BigInteger.valueOf(100_000_000), BigInteger.valueOf(500_000_009));
        Arrays.stream(ranges).forEach(row->System.out.println(Arrays.toString(row)));
        
        
//        int[][] bitsMatrix = {{15,1,0,1,0}, {16,1,1,1,0}, {17,0,1,0,1}, {18,1,1,1,1}, {19,0,0,0,1}};
//        
//        Arrays.stream(bitsMatrix).forEach((row)->System.out.println(Arrays.toString(row)));
//        System.out.println("-----");
//        Arrays.stream(nullspace(bitsMatrix,1)).forEach((row)->System.out.println(Arrays.toString(row)));
//        System.out.println("-----");
//        Arrays.stream(bitsMatrix).forEach((row)->System.out.println(Arrays.toString(row)));
    }
}
