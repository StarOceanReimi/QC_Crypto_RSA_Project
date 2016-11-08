import java.util.Random
import java.math.*
import groovy.transform.*

@Field
def randomBig = { from, to ->
    def rand = new Random()
    def dif  = (to - from) as BigDecimal
    def random = new BigDecimal(rand.nextDouble())
    random.multiply(dif).add(from) as BigInteger
}

@Field
def randomByBits = {
    def rand = new Random()
    if(it == 1) return rand.next(2)
    BigInteger r = 1
    for(i in 1..<it) {
        r = r.shiftLeft(BigInteger.ONE)
        r|= rand.nextInt(2)
    }
    return r
}

@Field
def primeByBits = {
    def n = randomByBits(it)
    if(n%2==0) n += 1
    while(1) {
        if(millarRabinTest(n))
            return n
        n += 2
    }
}

@Field 
def expMod = { a, e, n ->
    def base = a
    def ret = 1 as BigInteger
    def exp = e as BigInteger
    while(exp>0) {
        if(exp%2 == 1) {
            ret = ret*base%n
        }
        base = base**2%n
        exp = exp.shiftRight(1)
    }
    ret
}

@Field 
def millarRabinTest = { n, k=20 ->
    if(n<2) return false
    if(n<4) return true
    if(n%2==0) return false
    def (s, d) = [0, n-1 as BigInteger]
    while(d%2==0) {
        (s, d) = [s+1, d.shiftRight(1)]
    }
    def test = {
        def randomRoot = randomBig(2, n-2)
        def x = expMod(randomRoot, d, n)
        //println "$randomRoot^$d = $x mod $n"
        if(x == 1 || x == n-1) return true
        for(i in 0..<s) {
            x = x*x%n
            //println "$randomRoot^${d*2*(i+1)} = $x mod $n"
            if(x == 1) return false
            if(x == n-1) return true
        }
        false
    }
    for(i in 0..<k) {
        if(!test()) return false
    }
    true
}



class EratosPrimeGen {

    def criteria

    EratosPrimeGen(criteria=null) {
        this.criteria = criteria
        if(criteria == null) {
            criteria = { true }
        }
    }

    def gen() {
        new Iterator() {
            def D = [:]
            def q = 2
            def cnt = 0
            def nextPrime = q
            boolean hasNext() {
                def t = q
                while(1) {
                    if(!(t in D)) {
                        return criteria(t, cnt)        
                    }
                    t++
                }
            }

            Object next() {
                nextPrime = 0
                while(nextPrime == 0) {
                    if(!(q in D)) {
                        nextPrime = q
                        cnt++
                        D[q*q] = [q]
                    } else {
                        for(n in D[q]) {
                            if((n+q) in D) {
                                D[n+q].add(n)
                            } else {
                                D[n+q] = [n]
                            }
                        }
                        D.remove(q)
                    }
                    q++
                }
                nextPrime
            }
        }
    }
}

def eratosSieve = { 
    int[] sieve = new int[it]
    for(i in 2..<it) {
        if(!(sieve[i])) {
            sieve[i] = i
            for(int j=i; j<it; j+=i) {
                sieve[j] = sieve[j]?:i
            }
        }
    }
    sieve
}

def findPrimeFactor = {int n, sieve ->
    def factors = new TreeMap()
    while(1) {
        def p = sieve[n] as Integer
        if(p in factors) factors[p] += 1
        else factors[p] = 1
        if(n == p) break
        n /= p
    }
    factors
}

def expVector = {Map factors, factorBase -> 
    def vec = new int[factorBase.size]
    for(i in 0..<factorBase.size) {
        def f = factorBase[i]
        if(f in factors) {
            vec[i] = factors[f]%2
        }
    }
    vec
}

def flipVector = { vec, len = 0 ->
    def L = 0
    if(!len) { 
        try { len = vec.size } 
        catch(e) {len = vec.length }
    }
    vec.collect { (++L>len || it)?0:1 }
}

def gcd(a, b) {
    def r = b
    while(a != 0) {
        b = a
        a = r%a
        r = b
    }
    return b
}

def abs(n) {
    n<0?0-n:n
}

def pollar_rho = { BigInteger n ->
    BigInteger a=2, b=2, d=1
    def f = { BigInteger x -> (x*x+1) % n }
    while(d == 1) {
        a = f(a)
        b = f(f(b))
        if(a==b || d!=1) break
        d = gcd(abs(b-a), n)
    }
    return d
}

def newtonSqrt = { BigInteger n, scale=25, initGuess=20.0D, acc=150 ->
    def dn = new BigDecimal(n)
    def x_s = new BigDecimal(initGuess)
    def f  = { BigDecimal x -> x**2 - dn }
    def f_p = { BigDecimal x -> 2*x }
    while(--acc) {
        x_s = x_s - f(x_s).divide(f_p(x_s), scale, 5)
    }
    return x_s
}

def B = 1000

def sieve = eratosSieve(2500**2)

def g = new EratosPrimeGen({p, c->p<B}).gen()
def factorBase = g.collect { it }
def fB = factorBase[-1] 
def n1 = 809 * (primeByBits(38) as BigInteger)
def n2 = 199 * (primeByBits(39) as BigInteger)
println "$n1, ${n1/809}"
println "$n2, ${n2/199}"
println "*"*50

def n = n1*n2 as BigInteger

def d2 = newtonSqrt(n).setScale(0, RoundingMode.CEILING).toBigInteger()
println d2**2 as BigInteger
println n

def Q = { BigInteger X, BigInteger N -> (X**2 - N) }
def update = {f1, f2->
    def ret = new TreeMap(f1)
    f2.each { BigInteger k, v -> 
        if(k in ret) ret[k] += v
        else ret[k] = v
    }
    ret
}

def vs = []

while(1) {
    def qx = Q(d2, n)
    //println "Attempting $qx ..."
    def temp = qx
    def fs = new TreeMap<BigInteger, Integer>()
    while(qx>1) {
        if(millarRabinTest(qx)) break
        def f = pollar_rho(qx)
        //println "\t found factor: $f, ${f.class}"
        if(f == 1) break
        if(!millarRabinTest(f)) {
            fs = update(fs, findPrimeFactor(f, sieve))
        }
        else {
            fs[f] = 1
        }
        qx = qx.divide(f)
        if(millarRabinTest(qx)) break
    }
    if(millarRabinTest(qx)) {
        fs.put(qx, 1)
    }
    def largestFactor = fs.lastKey()
    if(largestFactor <= fB) {
        def v = expVector(fs, factorBase)
        println v
        vs.add([temp, v])
    }
    if(vs.size == 10) break
    d2 += 1
}

vs.each {
    println it
}



//println findPrimeFactor(Q(d2, n), sieve)

/**/
