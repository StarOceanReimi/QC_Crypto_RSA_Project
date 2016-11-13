import java.util.ArrayList;
import java.math.BigInteger;
import java.util.Random;
import java.util.HashMap;

public class QuadraticSieve{
  public static int[] primesLessThanErastothenes(int b){
    int[] primes = new int[b+1];
    for(int i = 2;i<primes.length;i++){
      if(primes[i] == 0){
        for(int j=(int)Math.pow(i,2);j<b+1;j+=i){
          primes[j] = i;
        }
      }
    }
    return primes;
  }
  public static int legendre(BigInteger p,BigInteger q){
    if(p.mod(q).equals(BigInteger.ZERO))
      return 0;
    if(p.compareTo(q) >= 0)
      return legendre(p.mod(q),q);
    if(p.compareTo(BigInteger.ONE) == 0)
      return 1;
    if(p.compareTo(BigInteger.valueOf(-1)) == 0){
      if(q.mod(BigInteger.valueOf(4)).compareTo(BigInteger.ONE) == 0)
        return 1;
      if(q.mod(BigInteger.valueOf(4)).compareTo(BigInteger.valueOf(3)) == 0)
        return -1;
    }
    if(p.compareTo(BigInteger.valueOf(2)) == 0){
      BigInteger x = q.mod(BigInteger.valueOf(8));
      if(x.compareTo(BigInteger.ONE) == 0 || x.compareTo(BigInteger.valueOf(7)) == 0)
        return 1;
      if(x.compareTo(BigInteger.valueOf(3)) == 0 || x.compareTo(BigInteger.valueOf(5)) == 0)
        return -1;
    }
    if(p.mod(BigInteger.valueOf(2)).compareTo(BigInteger.ZERO) == 0)
      return legendre(BigInteger.valueOf(2),q)*legendre(p.divide(BigInteger.valueOf(2)),q);
    if(p.mod(BigInteger.valueOf(4)).compareTo(BigInteger.valueOf(3)) == 0 && q.mod(BigInteger.valueOf(4)).compareTo(BigInteger.valueOf(3)) == 0)
      return -1*legendre(q,p);
    else return legendre(q,p);
  }
  public static int legendre(int p, int q){ 
    if(p%q == 0)
      return 0;
    if(p>=q)
      return legendre(p%q,q);
    if(p == 1)
      return 1;
    if(p == -1){
      if(q%4 == 1)
        return 1;
      if(q%4 == 3)
        return -1;
    }
    if(p == 2){
      if(q%8 == 1 || q%8 == 7)
        return 1;
      if(q%8 == 3 || q%8 == 5)
        return -1;
    }
    if(p%2 == 0)
      return legendre(2,q)*legendre(p/2,q);
    if(p%4 == 3 && q%4 == 3)
      return -1*legendre(q,p);
    else return legendre(q,p);
  }  
  public static int eulers_criterion(int p,int q){
    return (int)(Math.pow(p,(q-1)/2)%q);
  }
  //Babylonian Squareroot Method(approximate) for large integers
  public static BigInteger sqrt(BigInteger n,int sig_figs){
    BigInteger x = new BigInteger(n.bitLength()/2,new Random());
    for(int i=0;i<sig_figs;i++){
      x = x.add(n.divide(x));
      x = x.divide(BigInteger.valueOf(2));
    }
    return x;
  }
  
  public static void main(String[] args){  
    BigInteger ONE = BigInteger.ONE;
    BigInteger ZERO = BigInteger.ZERO;
    //BigInteger n = new BigInteger("613054310726032886180943888436325837702226698886723435429939101863");
    BigInteger n = new BigInteger("62113");
    int b = 37;
    //int b = 100;
    //BigInteger interval = new BigInteger("1000000");//Interval should be large enough to find pi(B)+1 b-smooth numbers
    BigInteger interval = new BigInteger("100");
    //BigInteger start = new BigInteger("782977848170708394135693691175755");
    BigInteger start = sqrt(n,1100);
    HashMap<BigInteger, ArrayList<Integer>> b_smooth_candidates = new HashMap<BigInteger, ArrayList<Integer>>();
    int[] primes = primesLessThanErastothenes(b);//Find all primes less than B bound
    ArrayList factorbase = new ArrayList();//Reduce the prime factorbase
    for(int i=2;i<primes.length;i++){
      if(primes[i] == 0){
        if(legendre(n,BigInteger.valueOf(i)) == 1){
          factorbase.add(i);
          //System.out.println(i);
        }
      }
    }
    //For every prime in the prime factorbase
    for(int p=0;p<factorbase.size();p++){
      int prime = (int)factorbase.get(p);
      int x = -1;
      int x2 = -1;
      //Find the initial value(solution)
      for(int i = 0;i<interval.intValue();i++){
        if(start.add(BigInteger.valueOf(i)).modPow(BigInteger.valueOf(2),BigInteger.valueOf(prime)).compareTo(n.mod(BigInteger.valueOf(prime))) == 0){
        //if(Math.pow((start+i),2)%prime == n%prime){
          x = i;
          for(int j = i+1;j<prime;j++){//Check for a second solution within a distance of the prime from first solution
            if(start.add(BigInteger.valueOf(j)).modPow(BigInteger.valueOf(2),BigInteger.valueOf(prime)).compareTo(n.mod(BigInteger.valueOf(prime))) == 0)
            //if(Math.pow((start+j),2)%prime == n%prime)
              x2 = j;
          }
          break;
        }
      }
      //Sieve the candidate b-smooth numbers
      if(x == -1)
        throw new RuntimeException("Should've found at least one solution given a quadratic residue");
      for(long i = x;i<interval.longValue();i+=prime){
        BigInteger num = start.add(BigInteger.valueOf(i));
        if(b_smooth_candidates.containsKey(num)){
          ArrayList t = b_smooth_candidates.get(num);
          t.add(prime);
          b_smooth_candidates.put(num,t);
        }
        else{
          ArrayList t = new ArrayList<Integer>();
          t.add(prime);
          b_smooth_candidates.put(num,t);
        }
      }
      if(x2 != -1){
        for(long i = x2;i<interval.longValue();i+=prime){
          BigInteger num = start.add(BigInteger.valueOf(i));
          if(b_smooth_candidates.containsKey(num)){
            ArrayList t = b_smooth_candidates.get(num);
            t.add(prime);
            b_smooth_candidates.put(num,t);
          }
          else{
            ArrayList t = new ArrayList<Integer>();
            t.add(prime);
            b_smooth_candidates.put(num,t);
          }          
        }
      }
    }
    for(BigInteger i:b_smooth_candidates.keySet()){
      BigInteger candidate = i.pow(2);
      candidate = candidate.subtract(n);
      ArrayList<Integer> factors = b_smooth_candidates.get(i);
      for(int f: factors){
        BigInteger pfactor = BigInteger.valueOf(f);
        while(candidate.mod(pfactor).compareTo(ZERO) == 0){
          candidate = candidate.divide(pfactor);
        }
      }
      if(candidate.compareTo(ONE) == 0)
        System.out.println(i+" "+factors);
    }
  }
}