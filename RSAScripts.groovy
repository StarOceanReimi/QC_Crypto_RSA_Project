import groovy.transform.*

@Field def letter = "abcdefghijklmnopqrstuvwxyz"

@Field def dict = new HashSet()

def load_dict(criteria=null) {
    dict.clear()
    if(criteria == null)
        criteria = { true }
    new File('dict.txt').eachLine {
        if(criteria(it)) dict.add(it)
    }
}

def code = 21525

class Node {
    Node parent = null
    byte data   = -1
    String toString() {
        "Node[D:$data]"
    }
}

def possibleMessage(BigInteger code) {
    def codeList = []
    while(code > 0) {
        codeList.add(code % 10)
        code /= 10
    }
    def list = codeList.reverse()
    def root = new Node()
    def leaves = []
    buildTree(root, list, 0, leaves)
    def rst = []
    for(node in leaves) {
        def cs = []
        while(node.parent != null) {
            cs.add(node.data)
            node = node.parent
        }
        rst.add(cs.reverse().collect{letter[it-1]}.join(''))
    }
    rst
}

def buildTree(root, list, i, leaves) {
    if(i > list.size-1) { 
        leaves.add(root)
        return
    }
    byte c = list[i]
    if(c==0) return
    if(i<list.size-1 && (c==1 || (c==2 && list[i+1] < 7))) {
        def x = i
        def n1 = new Node(parent:root, data:c*10+list[++i])
        buildTree(n1, list, i+1, leaves)
        i = x
    } 
    def n = new Node(parent:root, data:c)
    buildTree(n, list, i+1, leaves)    
}

def encode(msg) {
    msg.collect { 
        def i = letter.indexOf(it)
        i != -1 ? i+1 : it
    }.join() as BigInteger
}

def mycode = encode("thisisamoresecretmessagetoberesolvedandlonger")
println Math.log(mycode)/Math.log(10)
//println possibleMessage(mycode)
load_dict { it.length() >= 3 }
def encoded_dict = [:]
dict.each { 
    encoded_dict[(encode(it) as String)] = it
}
encoded_dict = encoded_dict.sort { -it.value.length() }
//encoded_dict = encoded_dict.sort { -it.key.length() }

def str = mycode.toString()
def founded = []
def start = 0

for(wordEntry in encoded_dict) {
    def word = wordEntry.key
    def index = str.indexOf(word)
    if (index != -1) {
        founded.add([encoded_dict[word], [index, word.length()]])
        str = str.replace(word, "0"*word.length())
    }
}

println founded
def tmp = new StringBuilder(str)
for(info in founded) {
    def word = info[0]
    def pos  = info[1]
    def pad  = "_"*(pos[1]-word.length())
    tmp.replace(pos[0], pos[0]+pos[1], word+pad)
}
str = tmp.toString().replaceAll("_","")
println str

println possibleMessage(encode("1addload7518"))
