import java.math.BigInteger;
import java.util.Random;

public class pollard_p_minus_one{
  //GCD function, A must be > B
  public static BigInteger gcd(BigInteger a,BigInteger b){
    if(b.equals(BigInteger.ZERO))
      return a;
    else
      return gcd(b,a.mod(b));
  }
  public static void p_Minus_One(BigInteger n){
    while(true){
      //a = some random integer from 1 to n
      BigInteger a = new BigInteger(n.bitLength(),new Random());
      while(a.compareTo(n) >= 0){
        a = new BigInteger(n.bitLength(),new Random());
      }
      //end of generate a
      
      if(gcd(n,a).compareTo(BigInteger.valueOf(1)) >0){//If there exists a common factor between n and a, meaning a is a multiple of one of the prime factors of n
        System.out.println("Found Factor:"+gcd(n,a));
        return;
      }
      else{
        BigInteger r = BigInteger.valueOf(2);
        BigInteger a_r = a;
        while(r.compareTo(n) <0){//Repeat this process until r = n, in which case the program must terminate(i.e. failed to find a factor)
          a_r = a_r.pow(r.intValue());
          BigInteger d = gcd(n,a_r.subtract(BigInteger.valueOf(1)));
          if(d.equals(n))
            break;//Failed to find a factor, Need to run function again with a different a
          else if(d.compareTo(BigInteger.valueOf(1)) > 0){//gcd(a^(r!)-1, n) = d <-- Prime factor of n
            System.out.println("Found factor:"+d);
            return;
          }else if(d.compareTo(BigInteger.valueOf(1)) == 0)//gcd(a^(r!)-1, n) = 1, so try gcd(a^(r+1)!, n)
            r = r.add(BigInteger.valueOf(1));
        }
      }
    }
  }
  public static void main(String[] args){
    BigInteger n = new BigInteger("613054310726032886180943888436325837702226698886723435429939101863");
    p_Minus_One(n);
  }
}