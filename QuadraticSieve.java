import java.util.ArrayList;
import java.math.BigInteger;
import java.util.Random;
import java.util.HashMap;

public class QuadraticSieve{
  public static final BigInteger ONE = BigInteger.ONE;
  public static final BigInteger ZERO = BigInteger.ZERO;
  public static HashMap<Integer,BigInteger> tptp = new HashMap<>();
  
  public static BigInteger gcd(BigInteger a,BigInteger b){
    if(b.equals(BigInteger.ZERO))
      return a;
    else
      return gcd(b,a.mod(b));
  }
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
  public static ArrayList reduceFactorBase(BigInteger n,int b){
    int[] primes = primesLessThanErastothenes(b);//Find all primes less than B bound
    ArrayList factorbase = new ArrayList();//Reduce the prime factorbase
    for(int i=2;i<primes.length;i++){
      if(primes[i] == 0){
        if(legendre(n,BigInteger.valueOf(i)) == 1){
          factorbase.add(i);
          //System.out.print(i+" ");
        }
      }
    }
    return factorbase;
  }
  public static void dumbQuadraticSieve(){
    //BigInteger n = new BigInteger("613054310726032886180943888436325837702226698886723435429939101863");
    BigInteger n = new BigInteger("62113");
    int b = 37;
    //int b = 100;
    //BigInteger interval = new BigInteger("10000000");//Interval should be large enough to find pi(B)+1 b-smooth numbers
    BigInteger interval = new BigInteger("100");
    //BigInteger start = new BigInteger("782977848170708394135693691175755");
    BigInteger start = sqrt(n,1100);
    //System.out.println("Starting: "+start);
    HashMap<BigInteger, ArrayList<Integer>> b_smooth_candidates = new HashMap<BigInteger, ArrayList<Integer>>();
    ArrayList factorbase = reduceFactorBase(n,b);
    
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
    //Check if x^2-n is b-smooth
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
        System.out.println("Found B-Smooth "+i+" "+factors);
    }
  }
  public static void smarterQuadraticSieve(BigInteger n,int b,BigInteger interval){
    //https://cr.yp.to/factorization/smoothparts-20040510.pdf
    BigInteger start = sqrt(n,1100).add(ONE);
    ArrayList<Integer> factorbase = reduceFactorBase(n,b);
    
    long Z = 1;
    for(int p: factorbase){
      Z *= p;
    }
    for(long i = 0;interval.compareTo(BigInteger.valueOf(i)) > 0;i++){
      BigInteger x = start.add(BigInteger.valueOf(i));
      x = x.multiply(x);
      x = x.subtract(n);
      x = x.mod(n);
      
      
      BigInteger Zmodx = BigInteger.valueOf(Z).mod(x);
      int j = 1;
      while(x.compareTo(tptp.get(j)) >= 0){
        j++;
      }
      BigInteger y = Zmodx;
      for(int r=0;r<j;r++){
        y = y.multiply(y).mod(x);
      }
      BigInteger gcd = gcd(x,y);
      
      //System.out.print(start.add(BigInteger.valueOf(i))+" x: "+x+" y: "+y+"||"+gcd);
      //if(x.divide(gcd).equals(ONE))
      //  System.out.println("<--This is b-smooth");
      //else
      //  System.out.println();
      //System.out.println(start.add(BigInteger.valueOf(i))+": "+x.divide(gcd));
      //
      
      if(x.divide(gcd).equals(ONE))
        System.out.println(start.add(BigInteger.valueOf(i))+"|x: "+x);
      
    }
  }
  public static void main(String[] args){  
    tptp.put(1,BigInteger.valueOf(4));
    tptp.put(2,BigInteger.valueOf(16));
    tptp.put(3,BigInteger.valueOf(256));
    tptp.put(4,BigInteger.valueOf(65536));
    tptp.put(5,new BigInteger("4294967296"));
    tptp.put(6,new BigInteger("18446744073709551616"));
    tptp.put(7,new BigInteger("340282366920938463463374607431768211456"));
    tptp.put(8,new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936"));
      
    BigInteger n = new BigInteger("613054310726032886180943888436325837702226698886723435429939101863");
    int b = 200000;
    BigInteger interval = new BigInteger("5000000");//Interval should be large enough to find pi(B)+1 b-smooth numbers
    
    /*
    BigInteger n = new BigInteger("1237891986662");
    int b = 10000;
    BigInteger interval = new BigInteger("10000000");
    */
    /*
    BigInteger n = new BigInteger("62113");
    int b = 37;
    BigInteger interval = new BigInteger("100");
    */
    smarterQuadraticSieve(n,b,interval);
  }
}