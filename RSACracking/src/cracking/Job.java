package cracking;

import java.io.Serializable;
import java.math.BigInteger;
import static java.lang.String.format;

public class Job implements Serializable {

    private final BigInteger start;
    private final BigInteger end;
    private final int pos;

    public Job(BigInteger begin, BigInteger start, BigInteger end) {
        this.start = start;
        this.end = end;
        this.pos = start.subtract(begin).intValue();
    }

    public BigInteger getStart() {
        return start;
    }

    public BigInteger getEnd() {
        return end;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return format("start:%s, end:%s, pos:%d", start, end, pos);
    }
}
