package cracking.cluster;
import java.io.Serializable;
import java.math.BigInteger;

public class Result implements Serializable {

    private String msg;

    private BigInteger[] BSmooth;
    
    public Result(String msg) {
        this.msg = msg;
    }

    public void setBSmooth(BigInteger[] bSmooth) {
        this.BSmooth = bSmooth;
    }

    public BigInteger[] getBSmooth() {
        return BSmooth;
    }
    
    @Override
    public String toString() {
        return msg;
    }

}
