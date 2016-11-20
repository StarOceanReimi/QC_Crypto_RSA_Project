package cracking;

import java.io.Serializable;
import java.math.BigInteger;
import static java.lang.String.format;

public class Job implements Serializable {

    private final BigInteger start;
    private final BigInteger end;
    private final BigInteger N;
    private final int B;

    public Job(BigInteger N, int B, BigInteger start, BigInteger end) {
        this.start = start;
        this.end   = end;
        this.N = N;
        this.B = B;
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
        return format("start:%s, end:%s, for Integer:%s, FactorBase: %d", start, end, N, B);
    }
}
