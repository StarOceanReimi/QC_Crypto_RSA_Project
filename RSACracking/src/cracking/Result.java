package cracking;
import java.io.Serializable;

public class Result implements Serializable {

    private String msg;

    public Result(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return msg;
    }

}
