package cracking.cluster;
import java.io.Serializable;

public class Result implements Serializable {

    private final String msg;

    private SmoothInfo[] BSmooth;
    
    public Result(String msg) {
        this.msg = msg;
    }

    public SmoothInfo[] getBSmooth() {
        return BSmooth;
    }

    public void setBSmooth(SmoothInfo[] BSmooth) {
        this.BSmooth = BSmooth;
    }

    @Override
    public String toString() {
        return msg;
    }

}
