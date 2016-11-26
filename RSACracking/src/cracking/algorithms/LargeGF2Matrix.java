/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cracking.algorithms;

import static cracking.utils.Util.error;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import static java.util.Arrays.stream;
import java.util.LinkedList;
import java.util.stream.Stream;

/**
 *
 * @author Li
 */
public class LargeGF2Matrix implements AutoCloseable {

    private static final int INTEGER_BITS = 8;
    
    private final int R;
    private final int C;
    private final int colInBytes;
    private final int pad;
    private final long start;
    private final RandomAccessFile file;
    
    private static int getPad(int row) {
        return INTEGER_BITS-row%INTEGER_BITS;
    }
    
    public static byte[] intArrayToRow(int[] array) {
        int[] gf2 = stream(array).map(i->i%2).toArray();
        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        for(int i=0, l=gf2.length; i<l; i+=INTEGER_BITS) {
            byte compress = (byte)0;
            int limit = INTEGER_BITS;
            int j = i;
            while(--limit >= 0) {
                if(j >= l) break;
                compress |= gf2[j++] << limit;
            }
            bytesStream.write(compress);
        }
        return bytesStream.toByteArray();
    }
    
    public static int[] rowToIntArray(byte[] row, int colSize) {
        int[] intArray = new int[colSize];
        for(int i=0; i<row.length; i++) {
            byte compressed = row[i];
            int limit = INTEGER_BITS;
            int j = i*INTEGER_BITS;
            while(--limit >= 0) {
                if(j >= colSize) break;
                intArray[j++] = compressed >> limit & 1;
            }
        }
        return intArray;
    }
    
    public LargeGF2Matrix(int R, int C, String filePath) throws FileNotFoundException, IOException {
        this.R = R;
        this.C = C;
        pad = getPad(C);
        File matraixFile = new File(filePath);
        if(matraixFile.exists()) matraixFile.delete();
        matraixFile.createNewFile();
        file = new RandomAccessFile(matraixFile, "rw");
        file.writeInt(R);
        file.writeInt(C);
        start = file.getFilePointer();
        colInBytes = (C+pad)/INTEGER_BITS;
        initializeZero();
    }

    public LargeGF2Matrix(String filePath) throws FileNotFoundException, IOException {
        file = new RandomAccessFile(filePath, "rw");
        R = file.readInt();
        C = file.readInt();
        pad = getPad(C);
        colInBytes = (C+pad)/INTEGER_BITS;
        start = file.getFilePointer();
    }
    
    public void add(int v, int r, int c) throws IOException {
        if(r >= R || c >= C) error("index out of bound %s,%s", r, c);
        long pos = gotoRow(r);
        int numByte = c/INTEGER_BITS;
        pos += numByte;
        byte b = file.readByte();
        b |= (v << (INTEGER_BITS-c%INTEGER_BITS-1));
        file.seek(pos);
        file.write(b);
        backToStart();
    }

    private long gotoRow(int r) throws IOException {
        long pos = colInBytes*r+start;
        file.seek(pos);
        return pos;
    }
    
    private void backToStart() throws IOException {
        file.seek(start);
    }
    
    public int[] getColumn(int c) throws IOException {
        if(c>=C) error("column should be less than %d", C);
        int[] col = new int[R];
        long startByte = start+c/INTEGER_BITS;
        file.seek(startByte);
        int i=0;
        while(i<R) {
            byte b = file.readByte();
            int v = b >> (INTEGER_BITS-c%INTEGER_BITS-1) & 0x1;
            col[i++] = v;
            startByte += colInBytes;
            file.seek(startByte);
        }
        backToStart();
        return col;
    }
    
    public byte[] getRawRow(int r) throws IOException {
        byte[] row = new byte[colInBytes];
        gotoRow(r);
        file.read(row);
        backToStart();
        return row;
    }
    
    public int[] getRow(int r) throws IOException {
        return rowToIntArray(getRawRow(r), C);
    }
    
    public void rowAdd(int r, byte[] data) throws IOException {
        if(r>=R) error("row should be less than %d", R);
        long pos = gotoRow(r);
        byte[] row = new byte[colInBytes];
        file.read(row);
        for(int i=0; i<row.length; i++) {
            row[i] ^= data[i];
        }
        file.seek(pos);
        file.write(row);
        backToStart();
    }
    
    public void rowAdd(int r, int[] data) throws IOException {
        if(r>=R) error("row should be less than %d", R);
        byte[] raw = intArrayToRow(data);
        long pos = gotoRow(r);
        byte[] row = new byte[colInBytes];
        file.read(row);
        for(int i=0; i<row.length && i<raw.length; i++) {
            row[i] ^= raw[i];
        }
        file.seek(pos);
        file.write(row);
        backToStart();
    }

    public int getRows() {
        return R;
    }

    public int getColumns() {
        return C;
    }
    
    public void identity() throws  IOException {
        if(R != C) error("can not make identity matrix with different row and col");
        for(int i=0; i<R; i++)
            add(1, i, i);
    }
    
    public boolean isAllZero(int r) throws IOException {
        byte[] row = getRawRow(r);
        for(byte b : row) {
            if(b != (byte)0) return false;
        }
        return true;
    }
    
    
    public static int[][] nullSpaceBy(LargeGF2Matrix gaussian, LargeGF2Matrix identity) throws IOException {
        Stream.Builder<int[]> builder = Stream.builder();
        int rows = identity.getRows();
        for(int r=rows-1; r>=0; r--) {
            if(gaussian.isAllZero(r)) {
                builder.accept(identity.getRow(r));
            }
        }
        gaussian.close();
        identity.close();
        return builder.build().toArray(int[][]::new);
    }
    
    public int[][] nullSpace() throws IOException {
        String tempIdentity = "./temp1";
//        String tempGuassian = "./temp2";
        int I = Math.max(R, C);
        Stream.Builder<int[]> builder = Stream.builder();
        LargeGF2Matrix identity = new LargeGF2Matrix(I, I, tempIdentity);
        identity.identity();
//        LargeGF2Matrix gaussian = new LargeGF2Matrix(R, C, tempGuassian);
//        int newRow = 0;
        boolean[] marker = new boolean[R];
        int onePercent = C/100;
        for(int c=0; c<C; c++) {
            if(onePercent!=0 && c % onePercent == 0) System.out.printf("calculate matrix: %d percent \n", c/onePercent);
            int[] col = getColumn(c);
            LinkedList<Integer> rows = new LinkedList<>();
            for(int j=0; j<R; j++) {
                if(col[j]==1 && !marker[j]) rows.add(j);
            }
            if(rows.isEmpty()) continue;
            int pivot = rows.poll();
            marker[pivot] = true;
            byte[] pivotRow = getRawRow(pivot);
            byte[] identityPivotRow = identity.getRawRow(pivot);
//            gaussian.rowAdd(newRow++, pivotRow);
            while(!rows.isEmpty()) {
                int row = rows.poll();
                rowAdd(row, pivotRow);
                identity.rowAdd(row, identityPivotRow);
            }
        }
        System.out.println("complete calculating. searching nullspace");
        for(int r=R-1; r>=0; r--) {
            if(isAllZero(r)) {
                builder.accept(identity.getRow(r));
            }
        }
//        gaussian.close();
        identity.close();
//        new File(tempIdentity).delete();
//        new File(tempGuassian).delete();
        System.out.println("finish");
        return builder.build().toArray(int[][]::new);
    }
    
    public static void main(String[] args) throws IOException {
        try (LargeGF2Matrix matrix = new LargeGF2Matrix(10, 10, "./testM")) {
            matrix.rowAdd(0, new int[]{1,0,0,1,0,0,0,1,0,1});
            matrix.rowAdd(1, new int[]{1,0,0,1,0,0,0,1,0,1});
            matrix.rowAdd(2, new int[]{1,1,0,1,0,0,0,1,0,1});
            matrix.rowAdd(3, new int[]{0,0,1,1,0,0,0,1,0,1});
            matrix.rowAdd(4, new int[]{1,0,0,1,0,0,0,1,1,1});
            matrix.rowAdd(5, new int[]{1,0,0,0,0,0,0,1,1,0});
            matrix.rowAdd(6, new int[]{0,0,0,1,0,1,0,0,1,0});
            matrix.rowAdd(7, new int[]{0,0,0,1,0,0,1,0,0,1});
            matrix.rowAdd(8, new int[]{0,0,1,0,0,1,0,0,0,0});
            matrix.rowAdd(9, new int[]{0,0,0,1,0,0,0,1,0,0});
            matrix.toMeatAxeBinaryFile("./testMeataxe");
        }
    }
    
    public void gf2Print(PrintStream out) throws IOException {
        for(int i=0; i<R; i++) {
            int[] row = getRow(i);
            for(int j=0; j<C; j++) {
                if(j != 0) out.print(' ');
                out.print(row[j]);
            }
            out.println();
        }
        backToStart();
    }
    
    public void gf2Print() throws IOException {
//        for(int i=0; i<R; i++) {
//            System.out.println(Arrays.toString(getRow(i)));
//        }
//        backToStart();
        gf2Print(System.out);
    }
    
    public void toMeatAxeBinaryFile(String filePath) throws FileNotFoundException, IOException {
        //field, rows, cols
        ByteBuffer header = ByteBuffer.allocate(4*3);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(2);
        header.putInt(R);
        header.putInt(C);
        
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            byte[] bytes = header.array();
            raf.write(bytes);
            
            FileChannel src = file.getChannel();
            raf.getChannel().transferFrom(src, bytes.length, file.length()-start);
        }
        backToStart();
    }
    
    public void hexPrint() throws IOException {
        for(int i=0; i<R; i++) {
            for(int j=0; j<colInBytes; j++) {
                System.out.print(Integer.toHexString(file.readByte()&0xff) + " ");
            }
            System.out.println();
        }
        backToStart();
    }
    
    public void close() throws IOException {
        file.close();
    }

    private void initializeZero() throws IOException {
        for(int i=0; i<R; i++) {
            file.write(new byte[colInBytes]);
        }
        backToStart();
    }
}
