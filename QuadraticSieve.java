import java.util.ArrayList;
import java.math.BigInteger;
import java.util.Random;

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
  public static int legendre_eulers(int p,int q){
    return (int)(Math.pow(p,(q-1)/2)%q);
  }
  
  public static void main(String[] args){
    int n = 87463;
    int b = 37;
    int start = (int)Math.sqrt(n);
    //Interval should be large enough to find pi(B)+1 b-smooth numbers
    int interval = 30;
    ArrayList[] b_smooth_candidates = new ArrayList[interval];//Contains the prime factorization of the number start+index i
    for(int i=0;i<interval;i++){
      b_smooth_candidates[i] = new ArrayList();
    }
    int[] primes = primesLessThanErastothenes(b);//Find all primes less than B bound
    ArrayList factorbase = new ArrayList();//Reduce the prime factorbase
    for(int i=2;i<primes.length;i++){
      if(primes[i] == 0){
        if(legendre(n,i) == 1){
          factorbase.add(i);
          //System.out.println(i);
        }
      }
    }
    //For every prime in the prime factorbase
    for(int p=0;p<factorbase.size();p++){
      int prime = (int)factorbase.get(p);
      int x=-1;
      int x2=-1;
      //Find the initial value(solution)
      for(int i=0;i<b_smooth_candidates.length;i++){
        if(Math.pow((start+i),2)%prime == n%prime){
          x = i;
          for(int j=i+1;j<prime;j++){//Check for a second solution within a distance of the prime from first solution
            if(Math.pow((start+j),2)%prime == n%prime)
              x2 = j;
          }
          break;
        }
      }
      //Sieve the candidate b-smooth numbers
      for(int i=x;i<b_smooth_candidates.length;i+=prime){
        b_smooth_candidates[i].add(prime);
      }
      if(x2!=-1){       
        for(int i=x2;i<b_smooth_candidates.length;i+=prime){
          b_smooth_candidates[i].add(prime);
        }
      }
    }
    for(int i = 0;i<b_smooth_candidates.length;i++){
      System.out.println(start+i+": "+b_smooth_candidates[i]);
    }
    
  }
}