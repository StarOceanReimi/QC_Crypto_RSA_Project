package cracking.cluster;

import java.io.Serializable;
import java.math.BigInteger;
import static java.lang.String.format;

public class Job implements Serializable {

    private BigInteger start;
    private BigInteger end;
    private final BigInteger N;
    private final int B;
    private BigInteger q;
    
    public Job(BigInteger N, int B, BigInteger start, BigInteger end) {
        this.start = start;
        this.end   = end;
        this.N = N;
        this.B = B;
    }

    public Job(BigInteger N, int B, BigInteger q, BigInteger start, BigInteger end) {
        this.N = N;
        this.B = B;
        this.q = q;
    }
    
    public BigInteger getQ() {
        return q;
    }
    
    public BigInteger getStart() {
        return start;
    }

    public BigInteger getEnd() {
        return end;
    }

    public int getB() {
        return B;
    }

    public BigInteger getN() {
        return N;
    }

    @Override
    public String toString() {
        if(q == null)
            return format("start:%s, end:%s, target:%s, factorbase:%d", start, end, N, B);
        else
            return format("q:%s, target:%s, factorbase:%d", q, N, B);
    }
}
