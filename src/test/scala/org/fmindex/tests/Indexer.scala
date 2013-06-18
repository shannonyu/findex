package org.fmindex.tests

import org.scalatest.FunSuite
import org.fmindex._
import scalax.file.Path
import scalax.io._
import org.fmindex.Util._

class GSuite extends FunSuite {
  def fromString(ts:String) = ts.getBytes ++ List(0.asInstanceOf[Byte])
  test("SuffixArray generics bigInts conversion") {
    val bs = fromString("\276")
    val sa = new SAISBuilder(bs)
    assert(sa.S(0) == 194)
  }
}
trait RandomGenerator {
  // Random generator
  val random = new scala.util.Random

  // Generate a random string of length n from the given alphabet
  def randomString(alphabet: String)(n: Int): String =
    Stream.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString

  // Generate a random alphabnumeric string of length n
  def randomAlphanumericString(n: Int) =
    randomString("abcdefghijklmnopqrstuvwxyz0123456789")(n)

  //def fromString(ts:String) = ts.getBytes ++ List(0.asInstanceOf[Byte])
  def fromString(ts:String) = ts.getBytes ++ List(0.asInstanceOf[Byte])

  def testSA(s:Array[Int],sa:Array[Int],verbose:Boolean=false) {
    for ( i <- 0 until sa.length - 1 ) {
      val a = s.slice(sa(i),s.length)
      val b = s.slice(sa(i+1),s.length)
      val l = a.length min b.length
      var j = 0
      
      while ( j < l) {
        if ( a(j) < b(j)) {
          j=l+1
        } else if ( a(j)> b(j)) {
          printf("BAD a(%d)  > b(%d) on j = %d %d %d\n",i,i+1,j,a(j),b(j))
          if ( verbose ) {
            println("a="+a.mkString("."))
            println("b="+b.mkString("."))
          }
          j=l+1
        }
        j+=1
      }
    }
  }

}

class BasicTests extends FunSuite with RandomGenerator {


  test("SAISBuilder basics") {
    /*                   01234567890 */
    val bs = fromString("missisippi")
    val sa = new SAISBuilder(bs)
    assert(sa.n==11)
    val bkt = sa.bucketEnds
    assert(bkt(0)==1 && bkt(105)==5 && bkt(109)==6 && bkt(112)==8 && bkt(115) == 11  && bkt(115) == 11)
    //sa.printBuckets(sa.getBuckets())
    sa.buildSL()
    //sa.printSL(sa.t)
    assert(sa.SL2String() == "LSLLSLSLLLS")
    sa.markLMS()
    assert(sa.SA(0) == 10 )
    assert(sa.SA(1) == -1 )
    assert(sa.SA(2) == 6 )
    //sa.printSA()
    val bkt1 = sa.bucketStarts
    assert(bkt1(0)==0 && bkt(1)==1 && bkt(2)==1 && bkt(106)==5)
    // sa.printSA()
    sa.induceSAl()
    //sa.printSA()
  }
  test("buckets test") {
    val sa = new SAISBuilder(fromString("aaaabbbccdd"))
    assert(sa.bucketStarts(0)==0 && sa.bucketStarts(1)==1 && sa.bucketStarts(98)==5 && sa.bucketStarts(99)==8)

  }
  
  test("article example") {
    val sa = new SAISBuilder(fromString("mmiissiissiippii"))
    val naive = new NaiveBuilder(fromString("mmiissiissiippii"))
    val bkt = sa.bucketEnds
    
    sa.buildSL()
    sa.markLMS()

    assert(sa.SA(0) == 16 )
    assert(sa.SA(6) == 10 )
    assert(sa.SA(7) == 6 )
    assert(sa.SA(8) == 2 )

    sa.induceSAl()
    assert(Array(16,15,14,-1,-1,-1,10,6,2,1,0,13,12,9,5,8,4).sameElements(sa.SA))

    sa.induceSAs()

    assert(Array(16,15,14,10,6,2,11,7,3,1,0,13,12,9,5,8,4).sameElements(sa.SA))

    //sa.printSA()

    naive.build()
    
    // actually everething is already sorted
    assert(naive.SA.sameElements(sa.SA))

    // sa.buildStep2()
    val lms_count = sa.fillSAWithLMS()
    var (names_count,sa1:Array[Int]) = sa.calcLexNames(lms_count)

    assert(names_count==3)
    assert(Array(2,2,1,0).sameElements(sa1))

    // Avoid recursve plays - direct go to step3
    var SA3  = new NaiveIntBuilder(sa1)
    SA3.build()
    sa.buildStep3(SA3.SA)

    assert(Array(16,15,14,10,6,2,11,7,3,1,0,13,12,9,5,8,4).sameElements(sa.SA))

  }
  test("1000 syms") {
    val in = randomAlphanumericString(1000)
    val sa = new SAISBuilder(fromString(in))
    sa.build()
  }
  test("nonaive example - suffixes are not sorted after first induce step") {

    val in = // randomAlphanumericString(200)
      "2b2w9vzrtqy3vzclgoofxgz9nal81y1fg8rozxkb5aaep1vpafp3cgsumc0z1rhpatcwo4d7nxc751h3a4woj3dbjf6ynfbkoom8sxoc9t3dqzkfs9akc6cmsy7cndi6bf116fju5rcsysixgkaih4zbkl8qo3ko2c42f34x6cqdew8x2jgz36r4bskabx02lxbfzokc"
    
    val sa = new SAISBuilder(fromString(in))
    val naive = new NaiveBuilder(fromString(in))
    
    naive.build()
    sa.buildStep1()
    // LMSess should be sorted
    val lms_count = sa.fillSAWithLMS()
    assert(lms_count==66)

    // naive.printSA()
    // 148 -> 47.  pafp3cgs....
    // 149 -> 63.  patcwo4d....
    // sa.printSA()
    // 149 -> 63.  patcwo4d....
    // 148 -> 47.  pafp3cgs....

    assert(! naive.SA.sameElements(sa.SA))

    // Test full cycle

    val sa2 = new SAISBuilder(fromString(in))
    sa2.build()
    assert(naive.SA.sameElements(sa2.SA))
  }

  test("naive sort test") {
    val sa = new NaiveBuilder(fromString("missisippi"))
    assert(! sa.naiveIsSASorted())
    sa.build()
    assert(sa.naiveIsSASorted())
    assert(Array(10,9,6,4,1,0,8,7,5,3,2).sameElements(sa.SA))
  }
  test("sais builder") {
    val b = Array[Byte](97,115,100,10,97,115,100,10,-1,97,115,100,10,98,101,108,107,97,64,98,101,108,107,97,45,104,111,109,101,58,47,116,109,112,47,116,36,32,99,97,116,32,62,32,116,50,46,116,120,116,10,97,115,100,97,115,100,10,-1,0)
    val k = new SAISBuilder(b)
    k.build()
  }
  test("nulled array") {
    var nb = new  ByteArrayNulledWrapper("mmiissiissiippii".getBytes())
    var b0 = fromString("mmiissiissiippii")
    assert(nb.length == b0.length)
    for (i <- 0 until b0.length ) {
      assert(nb(i) == b0(i),"nb(i) == b0(i) i=%d nb(i)=%d b0(i)=%d".format(i,nb(i),b0(i)))
    }
    val sa = new SAISBuilder(nb)
    sa.build()
  }

  test("bwt test") {
    val sa = new SAISBuilder(fromString("abracadabra"))
    var f = Path.fromString("/tmp/bwttest.txt")
    sa.build()
    sa.writeBWTNative(f.path)

    val ti:Input = Resource.fromFile(f.path)
    var content = new String(ti.byteArray).replace('\0','$')
    assert(content == "ard$rcaaaabb")
  }

  test("fl test") {
    val sa = new SAISBuilder(fromString("abracadabra"))
    var f = Path.fromString("/tmp/bwttest.fl")
    sa.build()
    sa.writeFL(f.path)
    val ti:Input = f.inputStream
    //println(java.nio.ByteBuffer.wrap(ti.byteArray))
    val bucket_size = 256 * 4

    val fl_len = f.size match {
      case Some(size) =>
        val s = size -  bucket_size
        (s - s  % 4).toInt
      case None => -1
    }

    assert(fl_len >0 )
    val fi = f.bytes.drop(bucket_size)
    val fl:Array[Int] = new Array[Int](fl_len/4)


    for ( i <- 0 until fl_len by 4 ) {
      fl(i/4)= (fi(i) << 24 )+ (fi(i+1) << 16) +( fi(i+2) << 8 ) + fi(i+3)
    }
    //
    assert(Array(3,0,6,7,8,9,10,11,5,2,1,4).sameElements(fl))
    
    f.delete()
    //var content = new String(ti.byteArray).replace('\0','$')

    //assert(content == "ard$rcaaaabb")
  }

  test("Naive SuffixAlgo test: occ/cf") {
    val sa = new SAISBuilder(fromString("abracadabra"))
    assert(sa.cf(0) == 0)
    assert(sa.cf('a') == 1)
    assert(sa.cf('b') == 6)
    sa.build()
    assert(sa.BWT(0) == 'a' , "")
    sa.buildOCC
    /*
    *** printSA
     0 -> 11. $abracadabra
     
     1 -> 10. a$abracadabr  0
     2 ->  7. abra$abracad  6
     3 ->  0. abracadabra$  7
     4 ->  3. acadabra$abr  8
     5 ->  5. adabra$abrac  9

     6 ->  8. bra$abracada
     7 ->  1. bracadabra$a
     8 ->  4. cadabra$abra
     9 ->  6. dabra$abraca
    10 ->  9. ra$abracadab
    11 ->  2. racadabra$ab
    */
    assert(Array(3,0,6,7,8,9,10,11,5,2,1,4).sameElements(sa.OCC))
    /*
      a r d $ r c a a a a b b
      0 1 2 3 4 5 6 7 8 9 0 1
    $ 0 0 0 1 1 1 1 1 1 1 1 1
    a 1 1 1 1 1 1 2 3 4 5 5 5
    b 0 0 0 0 0 0 0 0 0 0 1 2
    c 0 0 0 0 0 1 1 1 1 1 1 1
    d 0 0 1 1 1 1 1 1 1 1 1 1
    r 0 1 1 1 2 2 2 2 2 2 2 2
    */

    def row(c:Byte) = for (i<-0 until sa.n) yield sa.occ(c,i)

    assert(Array(0,0,0,1,1,1,1,1,1,1,1,1).sameElements(row(0)))
    assert(Array(1,1,1,1,1,1,2,3,4,5,5,5).sameElements(row('a')))
    assert(Array(0,0,0,0,0,0,0,0,0,0,1,2).sameElements(row('b')))
    assert(Array(0,0,0,0,0,1,1,1,1,1,1,1).sameElements(row('c')))
    assert(Array(0,0,1,1,1,1,1,1,1,1,1,1).sameElements(row('d')))
    assert(Array(0,1,1,1,2,2,2,2,2,2,2,2).sameElements(row('r')))
    assert(Array(0,0,0,0,0,0,0,0,0,0,0,0).sameElements(row('x')))

    //assert(sa.occ(0,3)==1,"sa.occ(0,3)="+sa.occ(0,3))
    //assert(sa.occ(0,4)==1,"sa.occ(0,3)="+sa.occ(0,4))
  }
  test("plain searching") {
    val sa = new SAISBuilder(fromString("abracadabra"))
    sa.build()
    sa.buildOCC

    sa.search("bra".getBytes()) match {
      case None => assert(false,"bra not found")
      case Some((6,8)) => // OK
      case _ => assert(false ,"Bad answer")
    }
  }
  test("BWT walki") {
    val sa = new SAISBuilder(fromString("abracadabra"))
    sa.build()
    sa.buildOCC
    //sa.printSA()
    // cadabra
    //    6   
    assert(sa.BWT(6).toChar == 'a')
    assert(sa.getPrevI(6)==2)
    assert(sa.BWT(sa.getPrevI(6)).toChar == 'd')
    assert(sa.BWT(sa.getPrevI(2)).toChar == 'a')
    
    assert(sa.getNextI(6)==10)
    assert(sa.getNextI(10)==1)

  }
  test("BWT substrings") {
    val sa = new SAISBuilder(fromString("abracadabra"))
    sa.build()
    sa.buildOCC
    //sa.printSA()
    // cadabra
    assert(sa.nextSubstr(6,4)=="bra\0","sa.nextSubstr(6,4)="+sa.nextSubstr(6,4))
    assert(sa.prevSubstr(6,4)=="cada","sa.nextSubstr(6,4)="+sa.prevSubstr(6,4))

  }
  test("BWT substrings2") {
    val sa = new SAISBuilder(fromString("mmabcacadabbbca".reverse))
    sa.build()
    sa.buildOCC
    //sa.printSA()
    assert(sa.nextSubstr(11,3) == "cba",sa.nextSubstr(11,3))
    assert(sa.prevSubstr(11,3) == "aca",sa.prevSubstr(11,3))
  }
  test("getPrevRange") {
    val sa = new SAISBuilder(fromString("mmabcacadabbbca".reverse))
    sa.build()
    sa.buildOCC
    //sa.printSA()

    assert (sa.occ('b',6)==3)
    assert (sa.getPrevRange(0,16,'a')==Some((1,6)) )
    assert (sa.getPrevRange(1,6,'b')==Some((6,8)) )
  }

  test("reducing bug example") {
    val d = Array[Byte](18,6,17,11,3,22,27,20,15,27,2,6,2,14,18,6,17,10,11,0)

    val sa = new SAISBuilder(d)
    val bkt = sa.bucketEnds
    println("d=" + d.mkString(","))

    //testSA(d,javasa,verbose=false)
    
    sa.buildSL()
    sa.markLMS()
    // IN SL Should be LMSess

    // 18,6,17,11,3,22,27,20,15,27,2,6,2,14,18,6,17,10,11,0
    // L  S  L  L S  S  L  L  S  L S L S  S  L S  L  S  L S
    //    *       *           *    *   *       *     *    *
    // BUCKETS:
    // 0   2 2  3  6  6 6 10 11 11 14 15 17 17 18 18 20 22 27 27
    // 19,12,10,4,-1,15,1,17,-1,-1,-1, 8,-1,-1,-1,-1,-1,-1,-1,-1

    assert(sa.SA.sameElements(Array(
      19,12,10,4,-1,15,1,17,-1,-1,-1,8,-1,-1,-1,-1,-1,-1,-1,-1
    )))
    
    sa.induceSAl()

    assert(sa.SA.sameElements(Array(
      19,12,10,4,11,15,1,17,18,3,-1,8,16,2,14,0,7,-1,9,6
    )))
    
    sa.induceSAs()

    assert(sa.SA.sameElements(Array(
      19,10,12,4,11,15,1,17,18,3,13,8,16,2,14,0,7,5,9,6
    )))
    

    // buildStep2()
    sa.fillSAWithLMS()

    assert(sa.SA.sameElements(Array(
      19,10,12,4,15,1,17,8,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    )))

    // println( sa.SA.mkString(","))

    val (names_count,reduced_string) = sa.calcLexNames(sa.lmsCount)

    assert(reduced_string.sameElements(Array(5,3,7,1,2,4,6,0)))
  }
  
}


class DFATests extends FunSuite with RandomGenerator {

  def `ab*c` = {
    val s = new StartState()
    val a = new State("a")
    val b = new State("b")
    val f = new FinishState()
    s.link(a,'a')
    a.link(b,'b')
    b.link(b,'b')
    b.link(f,'c')
    s
  }
  def `[cdfklm]b*c` = {
    val s = new StartState()
    val a = new State("a")
    val b = new State("b")
    val f = new FinishState()
    s.link(a,'c')
    s.link(a,'d')
    s.link(a,'f')
    s.link(a,'m')
    s.link(a,'k')
    s.link(a,'l')
    a.link(b,'b')
    b.link(b,'b')
    b.link(f,'c')
    s
  }
  def dfa1 ={
    val s = new StartState()
    val a = new State("a")
    val b = new State("b")
    val f = new FinishState()
    s.link(a,'c')
    s.link(a,'d')
    s.link(a,'f')
    s.link(a,'m')
    s.link(a,'l')
    s.link(a,0xfa)
    s.link(a,0xfb)
    s.link(a,0xfc)
    s.link(a,0xfd)
    s.link(a,0xfe)
    s.link(a,0xff)
    

    a.link(b,'b')
    b.link(b,'b')
    b.link(f,'c')
    s
  }
  test("basic dfa search") {
    // ab.*c
    val dfa = DFA.processLinkList(`ab*c`)
    assert(dfa.matchString("absbc")==false)
    assert(dfa.matchString("abbc"))
    assert(dfa.matchString("abc"))
  }
  test("dfa2") {
    /*
    [info] (a,97,b)
    [info] (b,100,f)
    [info] (a,100,f)
    [info] (s,97,a)
    [info] (b,97,b)
    */
    val s = new StartState()
    val a = new State("a")
    val b = new State("b")
    val f = new FinishState()
    s.link(a,'a')
    a.link(b,'a')
    a.link(f,'d')
    b.link(f,'d')
    b.link(b,'a')
    val dfa = DFA.processLinkList(s)
  }
  test("compileBuckets") {
    val dfa = DFA.processLinkList(`ab*c`)
    var m = dfa.buckets
    //dfa.dumpBuckets()
    assert(m.length == 4)
    assert(m(0).mkString(",")=="DFAChar('a'->1)")
    assert(m(1).mkString(",")=="DFAChar('b'->2)")
    assert(m(2).mkString(",")=="DFAChar('b'->2),DFAChar('c'->3)")
    assert(m(3).mkString(",")=="")
    val dfa2 = DFA.processLinkList(`[cdfklm]b*c`)
    var m2 = dfa2.buckets
    assert(m2.length == 4)
    assert(m2(0).mkString(",")=="DFABucket('c-d' ->1),DFAChar('f'->1),DFABucket('k-m' ->1)","movesBucket="+m(0).mkString(","))
    assert(m2(1).mkString(",")=="DFAChar('b'->2)")
    assert(m2(2).mkString(",")=="DFAChar('b'->2),DFAChar('c'->3)")
    assert(m2(3).mkString(",")=="")    
    val dfa3 = DFA.processLinkList(dfa1)
    assert(dfa3.buckets(0).mkString(",")=="DFABucket('c-d' ->1),DFAChar('f'->1),DFABucket('l-m' ->1),DFABucket('\\xfa-\\xff' ->1)")
  }
  test("match SA basics") {
    val sa = new SAISBuilder(fromString("mmabcacadabbbca".reverse))
    sa.build()
    sa.buildOCC
    //sa.printSA()
    val dfa = DFA.processLinkList(`ab*c`)
    val results = dfa.matchSA(sa)
    assert(results.size == 2)
    assert(results(0).toString=="cbbba")
    assert(results(1).toString=="cba")
    assert(sa.nextSubstr(results(1).sp,results(1).len)=="cba","sa.nextSubstr(results(1).sp,results(1).len)="+sa.nextSubstr(results(1).sp,results(1).len))
    assert(sa.nextSubstr(results(0).sp,results(0).len)=="cbbba")
  }
  test("match SA basics - abit larger string") {
    val sa = new SAISBuilder(fromString("2b2w9vzrtqy3vzclgoofxgz9nal81y1fg8rozxkb5aaep1vpafp3cgsumc0z1rhpatcwo4d7nxc751h3a4woj3dbjf6ynfbkoom8sxoc9t3dqzkfs9akc6cmsy7cndi6bf116fju5rcsysixgkaih4zbkl8qo3ko2c42f34x6cqdew8x2jgz36r4bskabx02lxbfzokc".reverse))
    sa.build()
    sa.buildOCC
    val dfa = DFA.processLinkList(`ab*c`)
    val results = dfa.matchSA(sa)    
    assert(results.size == 0)
  }

  test("IntArrayWrapper.test") {
    val d = Array(9,28,21,12,7,16,17,10,6,27,12,25,14,26,10,6,26,21,24,4,14,17,19,7,10,11,10,7,11,3,15,24,10,2,28,2,18,4,19,4,28,16,16,17,22,15,7,21,13,26,27,20,22,19,19,21,2,10,12,22,2,2,7,4,25,15,22,16,23,25,20,24,4,26,24,3,5,1,10,10,3,3,14,1,28,2,14,2,22,20,2,1,21,28,20,1,4,15,28,5,17,23,24,12,27,24,10,3,19,12,12,10,27,13,4,3,1,6,12,11,2,21,25,21,19,17,23,11,11,10,11,20,2,7,7,6,13,20,10,22,5,13,10,10,13,14,5,3,27,21,3,28,27,11,1,14,6,18,19,21,19,12,26,3,25,13,2,10,23,19,23,28,25,20,9,26,7,20,14,4,2,25,27,2,7,11,5,1,6,17,21,10,17,26,23,18,11,2,15,13,9,24,10,28,19,4,2,11,13,12,6,11,4,7,14,10,18,26,11,1,10,10,21,3,18,21,5,2,5,3,22,12,25,14,21,13,22,12,7,16,11,19,11,5,27,14,24,13,5,27,21,27,1,19,3,1,22,5,23,28,21,6,9,27,26,10,7,7,9,24,2,10,12,18,10,22,11,26,4,12,22,2,13,1,24,10,5,25,25,13,7,20,13,2,11,25,22,1,7,2,1,23,13,7,12,13,4,28,14,3,14,4,23,14,14,11,26,15,27,20,10,23,24,19,22,26,19,18,20,28,27,28,20,2,16,4,20,4,10,13,4,21,10,7,18,27,23,5,5,28,13,20,5,23,15,27,21,21,26,1,18,21,1,5,13,22,22,13,4,2,26,25,10,6,12,21,16,18,25,25,15,23,21,17,26,19,4,5,13,13,4,3,14,25,15,2,7,9,28,5,17,13,10,26,22,11,18,27,15,5,26,23,13,11,20,24,17,27,23,7,14,2,14,12,20,1,1,24,18,26,4,11,4,18,6,12,23,24,9,27,7,20,26,24,18,12,2,19,3,19,23,22,9,28,17,10,12,26,16,27,28,15,6,25,9,27,17,24,17,20,18,18,6,17,11,3,22,27,20,15,27,2,6,2,14,18,6,17,10,11,21,21,6,12,25,22,15,1,18,27,21,28,13,2,23,26,14,1,27,21,21,1,26,20,2,6,26,15,25,9,24,20,13,28,22,24,2,21,26,5,2,27,6,12,6,10,15,14,7,22,19,27,27,22,24,5,2,15,23,4,5,1,21,2,23,25,7,18,4,16,13,26,2,25,13,16,12,25,15,11,16,25,1,21,19,26,18,28,25,14,19,24,18,27,18,27,26,21,23,26,19,16,13,16,28,27,27,25,17,20,22,4,28,17,12,9,24,5,14,23,25,6,10,3,1,13,18,21,20,24,19,16,1,6,21,4,23,21,7,11,22,6,3,2,17,26,6,10,17,21,3,16,21,24,16,22,3,26,13,16,5,22,11,15,2,20,23,20,22,12,24,13,27,10,5,21,4,22,21,11,12,20,28,10,6,25,25,27,4,21,18,23,19,1,28,24,15,11,24,20,7,22,3,11,18,22,27,12,3,16,5,1,21,15,9,25,24,7,10,1,15,13,15,17,19,20,21,20,3,1,17,7,5,20,24,17,4,19,25,28,16,28,10,23,24,10,3,5,27,11,20,23,3,7,3,9,28,10,23,21,2,16,23,27,12,21,12,5,20,27,12,16,5,20,20,5,16,13,17,4,12,2,4,22,10,19,20,27,19,12,16,28,1,23,19,16,21,17,17,21,1,3,14,19,25,4,11,16,1,19,24,21,21,14,20,17,4,7,12,19,18,25,24,20,28,6,17,3,3,23,1,28,3,4,7,18,1,10,7,18,4,24,2,6,3,12,4,28,6,7,16,23,22,14,28,24,4,17,28,18,9,25,19,6,7,15,24,24,10,1,24,4,15,10,17,25,17,3,17,3,2,6,25,17,17,12,18,19,19,14,22,26,7,1,1,11,10,6,27,14,18,16,17,20,20,10,16,14,3,23,10,14,28,5,21,20,25,27,24,16,3,28,10,16,28,6,7,7,12,11,5,22,7,12,19,10,1,19,7,13,1,10,22,19,12,10,22,28,12,12,5,10,5,25,15,17,13,21,24,19,7,5,17,9,28,16,19,13,25,28,25,3,4,3,19,21,18,20,25,15,11,13,4,25,22,11,2,6,15,6,21,16,1,4,5,25,24,7,5,19,18,21,6,7,7,20,17,16,16,25,23,4,27,19,15,24,12,2,10,27,7,6,15,16,22,7,1,26,12,13,19,24,6,8,0)
    def checkOn(d:Array[Int]) {
      //val sa = new SAISIntBuilder(new IntArrayWrapper(d),d.max+1)
      val sa = new SAISBuilder(d map {_.toByte})
      sa.build()
      testSA(sa.S.data.map{_.toInt},sa.SA,verbose=false)

      import SAIS._
      var javasa  = new Array[Int](d.size)
      sais.suffixsort(d map {_.toByte}, javasa , d.size)
      testSA(d,javasa,verbose=false)

      for ( i <- 0 until sa.SA.length) {
        if (javasa(i) != sa.SA(i)) {
          assert(false,"Bad differ on %d %d != %d".format(i,javasa(i),sa.SA(i)))
        }
      }
    }
    val badArray = d.slice(471,490) :+ 0
    //println("Bad Array = " , badArray.mkString(","))
    checkOn(badArray)
    checkOn(d)
    
  }

}

class ReTest extends FunSuite with RandomGenerator {
  /*
  def testRe(s:String,what:String) {
    var x = ReParser.parseItem(s)
    val t = x.nfa.dotDump
    assert(t == what,"Re " + s + " wait\n----\n" + what + "\n-----\nReceive:\n"+t)
  }
  test("nfa creation") {
    testRe("abc","""digraph graphname {
0 -> 2  [label="b"]
2 -> 4  [label="c"]
4 -> 6  [label="eps"]
5 -> 0  [label="a"]
6 -> F  [label="eps"]
S -> 5  [label="eps"]
}
""")
    testRe("ab(d)c","""digraph graphname {
0 -> 2  [label="b"]
2 -> 4  [label="d"]
4 -> 6  [label="c"]
6 -> 8  [label="eps"]
7 -> 0  [label="a"]
8 -> F  [label="eps"]
S -> 7  [label="eps"]
}
""")
    testRe("a(bc)(de)","""digraph graphname {
0 -> 5  [label="eps"]
10 -> 12  [label="eps"]
11 -> 8  [label="d"]
12 -> 14  [label="eps"]
13 -> 0  [label="a"]
14 -> F  [label="eps"]
2 -> 4  [label="c"]
4 -> 6  [label="eps"]
5 -> 2  [label="b"]
6 -> 11  [label="eps"]
8 -> 10  [label="e"]
S -> 13  [label="eps"]
}
""")
    testRe("a(b|ce|klm)d","""digraph graphname {
0 -> 13  [label="eps"]
1 -> 14  [label="eps"]
10 -> 12  [label="m"]
12 -> 14  [label="eps"]
13 -> 1  [label="b"]
13 -> 4  [label="c"]
13 -> 8  [label="k"]
14 -> 16  [label="d"]
16 -> 18  [label="eps"]
17 -> 0  [label="a"]
18 -> F  [label="eps"]
4 -> 5  [label="e"]
5 -> 14  [label="eps"]
8 -> 10  [label="l"]
S -> 17  [label="eps"]
}
""")
    testRe("a(bbbb|cc(c|d)c)d","""digraph graphname {
0 -> 21  [label="eps"]
10 -> 12  [label="c"]
12 -> 17  [label="eps"]
13 -> 18  [label="eps"]
16 -> 18  [label="eps"]
17 -> 13  [label="c"]
17 -> 16  [label="d"]
18 -> 20  [label="c"]
2 -> 4  [label="b"]
20 -> 22  [label="eps"]
21 -> 10  [label="c"]
21 -> 2  [label="b"]
22 -> 24  [label="d"]
24 -> 26  [label="eps"]
25 -> 0  [label="a"]
26 -> F  [label="eps"]
4 -> 6  [label="b"]
6 -> 7  [label="b"]
7 -> 22  [label="eps"]
S -> 25  [label="eps"]
}
""")
    // testRe("a(b|c)+d","")
    // testRe("a(b|c)*d","")
    // testRe("a(b|c)?d","")
  }
}
class NFATest extends FunSuite with RandomGenerator {
  test("transitions1") {
    val s = new NfaStartState()
    val l1 = new NfaState()
    val l2 = new NfaState()
    val l3 = new NfaState()
    val l4 = new NfaState()
    val l5 = new NfaState()
    val l6 = new NfaState()
    s.epsilon(l1)
    s.epsilon(l2)
    l2.epsilon(l3)
    l3.epsilon(s)
    l1.link(l4,'b')
    l1.link(l5,'a')
    l2.link(l6,'a')
    l3.link(l6,'b')
    assert(s.epsilons.sameElements(Set(s,l1,l2,l3)))    
    // Map(98 -> Set(NfaBaseState(5), NfaBaseState(7)), 97 -> Set(NfaBaseState(6), NfaBaseState(7)))
    val et = s.epsilonTransitions
    assert(et('a').sameElements(Set(l5,l6)))
    assert(et('b').sameElements(Set(l4,l6)))
  }
  
  test("transitions2") {
    val s = new NfaStartState()
    val l1 = new NfaState()
    val l2 = new NfaState()
    val l3 = new NfaState()
    val l4 = new NfaState()
    val l5 = new NfaState()
    val l6 = new NfaState()
    val l7 = new NfaState()
    s.epsilon(l1)
    s.epsilon(l2)
    l2.epsilon(l3)
    l3.epsilon(s)
    l1.link(l4,'b')
    l1.link(l5,'a')
    l2.link(l6,'a')
    l3.link(l6,'b')
    l6.link(l7,'c')
    assert(NFA.epsilons(Set(s,l2)).sameElements(s.epsilons))
    val et = NFA.epsilonTransitions(Set(s,l2,l6))
    assert(et('a').size==2)
    assert(et('b').size==2)
    assert(et('c').size==1)
    
  }
  */
}
class BWTMerge extends FunSuite with RandomGenerator {
  
  test("compute_gt_eof") {
    val d1 = "abracadabra"
    val d2 = "bracaabraca"
    val b1 = new BwtIndex(data=d1.getBytes)
    val b2 = new BwtIndex(data=d2.getBytes)
    
    val gt_eof=BWTMerger.compute_gt_eof(b1,b2)
    for ( i <- 0 until d1.length) {
      val s1 = (d1 + d2).substring(i,d1.length+d2.length-1)
      assert((s1>d2) == gt_eof(i),"s1=%s,d2=%s,s1>d2 = %s != gt_eof(i=%d) = %s" format (s1,d2,s1>d2,i,gt_eof(i)))
    }
  }
  test("kmp_prefix") {
    val t = BWTMerger.kmp_preifx("ababaca".getBytes)
    assert(t.sameElements(Array(0,0,1,2,3,0,1)))
  }

  test("remap_alphabet") {
    val d1 = "abracadabra"
    val d2 = "bracaabraca"
    val b1 = new BwtIndex(data=d1.getBytes)
    val b2 = new BwtIndex(data=d2.getBytes)
    
    val gt_eof=BWTMerger.compute_gt_eof(b1,b2)
    val (remapped,k) = BWTMerger.remap_alphabet(b1,gt_eof)
    // 2,3,6,2,4,2,5,0,3,6,1
    // a b r a c a d a b r a
    assert(remapped.sameElements(Array(2,3,6,2,4,2,5,0,3,6,1)))
    //println(BWTMerger.remap_alphabet(b1,gt_eof).mkString(","))

    val sa = new SAISIntBuilder(new IntArrayWrapper(remapped),k)
    sa.build()

  }

  

  test("SAIS0FreeBuilder calcGTTN direct test") {
    val d2 = "daxecedaddbadxcdexdbaexacbcxaecbecbcaaaxbcbexeedbacbaebxdxadbxaxccbcxacdexabcddbccedaxcxecaxxacbbdbx"
    val sa = new SAIS0FreeBuilder(d2.getBytes)
    sa.build()
    assert(sa.bwtRank==56)
    val gtn = sa.calcGTTN
    for ( i <- 0 until d2.length) {
      val s1 = d2.substring(i,d2.length)
      assert((s1>d2) == gtn(i),"i=%d, s1=%s gtn(i)=%s (s1>d2)=%s" format (
        i,s1,gtn(i),(s1>d2)
      ))
    }
  }
  test("compute_gt_eof abit larger") {
    val d1 = "ecddxxdxbxacceaeexxxcdaxccxadbbccabccexxccbcbexdbddcabbecxbxxdbcabbeedxbxxaaddbaaxdedxdbexabexdxxaed"
    val d2 = "daxecedaddbadxcdexdbaexacbcxaecbecbcaaaxbcbexeedbacbaebxdxadbxaxccbcxacdexabcddbccedaxcxecaxxacbbdbx"
    val b1 = new BwtIndex(data=d1.getBytes)
    val b2 = new BwtIndex(data=d2.getBytes)
    
    val gt_eof=BWTMerger.compute_gt_eof(b1,b2)
    //println(gt_eof.mkString(","))
    for ( i <- 0 until d1.length) {
      val s1 = (d1 + d2).substring(i,d1.length+d2.length-1)
      //println(s1,d2,s1>d2)
      assert((s1>d2) == gt_eof(i),"s1=%s,d2=%s,s1>d2 = %s != gt_eof(i=%d) = %s" format (
        s1,d2,s1>d2,i,gt_eof(i))
      )
    }
  }

  
  test("compute_gt_eof abit larger2") {
    val d1 = "eecedebxedxdacebddbbdxxxdcdadxeecbeabeaebbcbddbdbddbbxaabbadaccxxdecxeabbedxabxeecdebcadeedadbbbxece"
    val d2 = "abdxcbbdccdeecexbbxecdbaeeeaeecxacxxcdcdadedddxbxdxxaedbcdbcbxdaedeaecbxdecxccbcxdebcxbxcdbxabbdbxda"
    val b1 = new BwtIndex(data=d1.getBytes)
    val b2 = new BwtIndex(data=d2.getBytes)
    
    val gt_eof=BWTMerger.compute_gt_eof(b1,b2)
    //println(gt_eof.mkString(","))
    for ( i <- 0 until d1.length) {
      val s1 = (d1 + d2).substring(i,d1.length+d2.length-1)
      //println(s1,d2,s1>d2)
      assert((s1>d2) == gt_eof(i),"s1=%s,d2=%s,s1>d2 = %s != gt_eof(i=%d) = %s" format (
        s1,d2,s1>d2,i,gt_eof(i))
      )
    }
  }

  test("BWTMerger.build") {
    val d1 = "barbaarbadaacarb"
    val d2 = "abaacarbadacarba"
    val bwt1 = new BwtIndex(data=d1.getBytes)
    val bwt2 = new BwtIndex(data=d2.getBytes)
    
    assert(bwt2.GT_TN.mkString(",") == 
      "false,true,false,true,true,true,true,true,true,true,true,true,true,true,true,false")
        
    val gt_eof = BWTMerger.compute_gt_eof(bwt1,bwt2)
    assert(gt_eof.mkString(",") == 
      "true,true,true,true,false,true,true,true,true,true,false,true,true,true,true,true")

    val (remapped,k) = BWTMerger.remap_alphabet(bwt1,gt_eof)

    assert(k==7)
    assert(remapped.mkString(",")=="3,0,6,1,0,0,6,3,0,5,0,0,4,0,6,2")
    
    val sa = new SAISIntBuilder(new IntArrayWrapper(remapped),k)
    sa.build()
    assert(sa.SA.mkString(",")=="10,4,11,8,1,13,5,3,15,7,0,12,9,2,14,6")

    val ranks = sa.convertSA2Rank(sa.SA)
    
    assert(ranks.mkString(",")=="10,4,13,7,1,6,15,9,3,12,0,2,11,5,14,8")

    val bwt = bwt1.sa2bwt(sa.SA)
    assert( bwt.map(_.toChar).mkString("")   == "dbabbcarrrbaaaaa" )

  }

  test("compute_gt_eof random") {
    val c = 100
    for ( i<- 0 until c ) {
      val d1 = randomString("abcdex")(100)
      val d2 = randomString("abcdex")(100)

      val b1 = new BwtIndex(data=d1.getBytes)
      val b2 = new BwtIndex(data=d2.getBytes)
      
      val gt_eof=BWTMerger.compute_gt_eof(b1,b2)
      //println(gt_eof.mkString(","))
      for ( i <- 0 until d1.length) {
        val s1 = (d1 + d2).substring(i,d1.length+d2.length-1)
        if ( (s1>d2) != gt_eof(i) ) {
          println(d1,d2)
        }
        assert((s1>d2) == gt_eof(i),"s1>d2 = %s != gt_eof(i=%d) = %s" format (s1>d2,i,gt_eof(i)))
      }
    }
  }

  test("BWTMerger.build0") {
    val d1 = "barbaarbadaacarb"
    val d2 = "abaacarbadacarba"
    val bwt1 = new BwtIndex(data=d1.getBytes)
    val bwt2 = new BwtIndex(data=d2.getBytes)
    
    assert(bwt2.GT_TN.mkString(",") == 
      "false,true,false,true,true,true,true,true,true,true,true,true,true,true,true,false")
        
    val gt_eof = BWTMerger.compute_gt_eof(bwt1,bwt2)
    assert(gt_eof.mkString(",") == 
      "true,true,true,true,false,true,true,true,true,true,false,true,true,true,true,true")

    val (remapped,k) = BWTMerger.remap_alphabet2(bwt1,gt_eof)
    
    val sa = new SAISIntBuilder(new IntArrayWrapper(remapped),k)
    
    sa.build()
    
    assert(sa.SA.mkString(",")=="16,10,4,11,8,1,13,5,3,15,7,0,12,9,2,14,6")

    val bwt = bwt1.sa2bwt2(sa.SA)
    assert( bwt.map(_.toChar).mkString("")   == "dbabbcarrrbaaaaa" )

  }
}

class BadTest extends FunSuite with RandomGenerator {
  

  
  
  test("BWTMerger.build") {
    // Added Part
    val d1 = "gfvqkjxagtnmfgyhbjvmqyduwnnorggfspqegvwedansfmfbitwdkimwrpsqcdcwzwkqnzgoegqvskomwehejjzthjqthakgqahqjgteijggfznhzcnvywrsezlhuclnhrronplyfhiaagxtlqqpjoowfbcocowohmdvahvvmgfqwgpzodvzltungfzdjcfbvdpghapgdczauccofzrvwpqjgdorlssvqanidwqlcasoosnquaznjqyrqhtdbjdoknerrenjyrejsjyunbsuhzgcgcuriyechvuhznzwqdovregoacrsrqomkmahgvwgmsaencjytpictgrvimvzaqupsdywwfhzrjistdsehykvjtrurbmitenkxctnvsncsohfxobcftigsudsfanqvrspkachfwulevgjozdtrowyyznknqxusxypypvqlwzpxqsawnimwjnkwbxkndpgwubsaedumbevtyyqtglmhfjfybexsbvtzkrvgwmxfbrxassyalxubkzsypamtwjfssihofplbfbymrytciofpprovoygwfmzynxjhozgtuqcqbjpvxrgygvujfpdidxpvaarjlblguyovrikuxemypitxhkoezggbmwlcdkkedqxosumwwpnsjfhwxbdkttkeaspaxssymuerkzeeuypghsdkhdrdnbrzyzrpqxtqvuhrymxilludlclzdkjgkuabgatwibkrgkwwehvakbtjdxithpjhbvggghxygfszuetacqaysyekvlyeiqingjtkslwjtcebespcshhaixphlgdifjkibdqzhvgkmbipuxohsofaeigbywbdlrgxgrwzuquhbkwcxjqsqpflaiyzcsycelkhhkethrkfggbrihiiuoqswsbijfacdkyhjjqchvyjvuoezmdarzsabrtblbzalcchhaecvxdvrwuntmwdgbbtjhbsqqtryxksgmtonnzdqdpbzbhvmcighihgqoldvsxfhxlwjyfhongjszg"
    // old Part
    val d2 = "tvrqdzgumekahkhtgrpsvhdjxcveifpadpjgjuignlvvrhqshjequbgudhbmibjjuuzknqjtlpqclvmxeslilnxjnusquggdkqlrxiqiqtjxjjitpqzduwkgkvkzqvxshkekhzgrzskhvvvpzxkczbrnbvgnhlqecnlybmndogktltjattjspzvosppyitffumxsgqjemhujrcsjyjcxmwmdxvipgafilmjpoxjipqrikxjqhithalrjiamkurakcnyzxftfxpppntjefxqvfgimebjpzdwxrceffsjjmixtfbqflgfqcogmmibmxigpswmvoeioayuvodrarivdaqfrhbnafukmqkzwpfpbgiyqwcwvoyklgrubboqhgfioibgjpuntqrfuzsejwnugdrvjknkyatntifhrcbdwczfossphiuxphlogdjusptrmuficaegeabqwwjnhcixjsjeefmsirzvojywbwnsxijeusnitazssdefbfnwaksmuhzqzkwahstzuzuvjxhyajqlwdrijdzhvmlrpobknapekcbphelyypijdydojxwqqomluvnhnsgqzdjoguskygbihsgpfmuuiwgzxwftcoqslcghcfbxyfpklguyjfzanqsesitcwnzkrqzoebfdivzlxznvyrxbfdywympxunfokhspqsffgoyomkfqosvxzsdkdkmdcubivbjnbdegdvnbmoherrmrxazwijffbwbpzresgknmrzzhenwaivsvteglxugreeyysblghxqivgkybofncuenozjzjcbzzijlfmucvgvbhzqwbiebtsekcczmkpzaseuuhxgrpjopftntwzjybaqkgibuotynbdnqmzixxqkufozdmboiftvvjuswktxhaojckptubqytrkyidjecpaqvthjyjprkcszdgnbpyynpdjkqzsiusslavpzuuhuccxesmxdcudsslmgnlppcbelgptmwbmbwpcqpryfyqlbvqcaalnyrvmpru"
    val bwt1 = new BwtIndex(data=d1.getBytes.reverse)
    val bwt2 = new BwtIndex(data=d2.getBytes.reverse)
    
    val gt_eof = BWTMerger.compute_gt_eof(bwt1,bwt2)

    val (remapped,k) = BWTMerger.remap_alphabet2(bwt1,gt_eof)
    
    val tremap = "8,27,20,11,6,15,16,9,5,26,11,24,13,25,9,5,25,20,23,3,13,16,18,6,9,10,9,6,10,2,14,23,9,1,27,1,17,3,18,3,27,15,15,16,21,14,6,20,12,25,26,19,21,18,18,20,1,9,11,21,1,1,6,3,24,14,21,15,22,24,19,23,3,25,23,2,4,0,9,9,2,2,13,0,27,1,13,1,21,19,1,0,20,27,19,0,3,14,27,4,16,22,23,11,26,23,9,2,18,11,11,9,26,12,3,2,0,5,11,10,1,20,24,20,18,16,22,10,10,9,10,19,1,6,6,5,12,19,9,21,4,12,9,9,12,13,4,2,26,20,2,27,26,10,0,13,5,17,18,20,18,11,25,2,24,12,1,9,22,18,22,27,24,19,8,25,6,19,13,3,1,24,26,1,6,10,4,0,5,16,20,9,16,25,22,17,10,1,14,12,8,23,9,27,18,3,1,10,12,11,5,10,3,6,13,9,17,25,10,0,9,9,20,2,17,20,4,1,4,2,21,11,24,13,20,12,21,11,6,15,10,18,10,4,26,13,23,12,4,26,20,26,0,18,2,0,21,4,22,27,20,5,8,26,25,9,6,6,8,23,1,9,11,17,9,21,10,25,3,11,21,1,12,0,23,9,4,24,24,12,6,19,12,1,10,24,21,0,6,1,0,22,12,6,11,12,3,27,13,2,13,3,22,13,13,10,25,14,26,19,9,22,23,18,21,25,18,17,19,27,26,27,19,1,15,3,19,3,9,12,3,20,9,6,17,26,22,4,4,27,12,19,4,22,14,26,20,20,25,0,17,20,0,4,12,21,21,12,3,1,25,24,9,5,11,20,15,17,24,24,14,22,20,16,25,18,3,4,12,12,3,2,13,24,14,1,6,8,27,4,16,12,9,25,21,10,17,26,14,4,25,22,12,10,19,23,16,26,22,6,13,1,13,11,19,0,0,23,17,25,3,10,3,17,5,11,22,23,8,26,6,19,25,23,17,11,1,18,2,18,22,21,8,27,16,9,11,25,15,26,27,14,5,24,8,26,16,23,16,19,17,17,5,16,10,2,21,26,19,14,26,1,5,1,13,17,5,16,9,10,20,20,5,11,24,21,14,0,17,26,20,27,12,1,22,25,13,0,26,20,20,0,25,19,1,5,25,14,24,8,23,19,12,27,21,23,1,20,25,4,1,26,5,11,5,9,14,13,6,21,18,26,26,21,23,4,1,14,22,3,4,0,20,1,22,24,6,17,3,15,12,25,1,24,12,15,11,24,14,10,15,24,0,20,18,25,17,27,24,13,18,23,17,26,17,26,25,20,22,25,18,15,12,15,27,26,26,24,16,19,21,3,27,16,11,8,23,4,13,22,24,5,9,2,0,12,17,20,19,23,18,15,0,5,20,3,22,20,6,10,21,5,2,1,16,25,5,9,16,20,2,15,20,23,15,21,2,25,12,15,4,21,10,14,1,19,22,19,21,11,23,12,26,9,4,20,3,21,20,10,11,19,27,9,5,24,24,26,3,20,17,22,18,0,27,23,14,10,23,19,6,21,2,10,17,21,26,11,2,15,4,0,20,14,8,24,23,6,9,0,14,12,14,16,18,19,20,19,2,0,16,6,4,19,23,16,3,18,24,27,15,27,9,22,23,9,2,4,26,10,19,22,2,6,2,8,27,9,22,20,1,15,22,26,11,20,11,4,19,26,11,15,4,19,19,4,15,12,16,3,11,1,3,21,9,18,19,26,18,11,15,27,0,22,18,15,20,16,16,20,0,2,13,18,24,3,10,15,0,18,23,20,20,13,19,16,3,6,11,18,17,24,23,19,27,5,16,2,2,22,0,27,2,3,6,17,0,9,6,17,3,23,1,5,2,11,3,27,5,6,15,22,21,13,27,23,3,16,27,17,8,24,18,5,6,14,23,23,9,0,23,3,14,9,16,24,16,2,16,2,1,5,24,16,16,11,17,18,18,13,21,25,6,0,0,10,9,5,26,13,17,15,16,19,19,9,15,13,2,22,9,13,27,4,20,19,24,26,23,15,2,27,9,15,27,5,6,6,11,10,4,21,6,11,18,9,0,18,6,12,0,9,21,18,11,9,21,27,11,11,4,9,4,24,14,16,12,20,23,18,6,4,16,8,27,15,18,12,24,27,24,2,3,2,18,20,17,19,24,14,10,12,3,24,21,10,1,5,14,5,20,15,0,3,4,24,23,6,4,18,17,20,5,6,6,19,16,15,15,24,22,3,26,18,14,23,11,1,9,26,6,5,14,15,21,6,0,25,11,12,18,23,5,7"
    val tremap_ints = tremap.split(",").map({_.toInt})

    for (i <- 0 until tremap_ints.length) {
      assert( remapped(i) == (tremap_ints(i) +1) ,"remapped %d. %d != %d".format(i,remapped(i),tremap_ints(i)+1))
    }

    val sa = new SAISIntBuilder(new IntArrayWrapper(remapped),k)
    sa.build()

    val test_SA = "883,425,790,982,95,362,116,187,622,297,826,77,219,930,884,614,154,709,719,359,497,252,926,798,551,701,572,91,255,300,782,859,283,426,1016,511,507,83,820,683,90,299,60,771,227,481,832,976,870,514,61,132,395,183,270,56,166,1007,206,293,281,85,421,483,197,546,333,750,633,35,446,654,120,526,87,553,503,562,180,369,530,33,115,613,718,254,869,632,80,817,961,822,75,736,742,744,692,834,81,309,791,391,29,698,640,867,223,107,963,448,229,475,818,898,163,646,147,910,150,205,179,368,114,390,962,549,386,983,806,212,823,337,430,795,769,278,19,861,96,558,846,432,37,726,335,340,678,772,666,311,625,830,63,972,72,1001,836,306,39,601,186,76,550,700,226,545,529,228,146,347,940,140,387,363,607,765,951,399,99,988,762,722,757,664,903,920,650,352,257,942,984,286,409,737,243,248,348,482,631,833,915,992,853,838,1022,261,611,534,636,210,532,117,373,434,492,135,977,1011,815,486,472,188,156,623,979,462,871,674,515,15,8,887,882,1015,298,743,62,950,987,721,134,1010,133,266,916,993,267,396,707,23,27,184,628,917,303,922,807,928,419,213,854,238,4,839,824,556,828,343,290,176,994,440,46,690,538,1023,268,605,200,519,850,704,174,438,464,262,397,745,953,452,0,708,925,858,32,612,79,735,106,663,941,285,372,673,14,7,886,265,26,827,342,78,142,220,24,129,488,271,57,455,338,143,900,535,895,912,637,863,191,215,774,221,138,274,931,935,167,747,732,320,402,1008,111,202,218,153,975,196,119,28,474,211,431,185,919,242,885,25,128,127,669,970,207,652,796,569,693,405,240,130,739,413,489,629,687,294,276,315,770,1006,445,697,835,939,756,533,209,237,3,604,934,110,118,918,938,109,304,1018,760,779,272,875,923,808,423,670,754,374,58,279,435,659,231,10,566,493,161,456,103,929,282,165,292,502,367,113,389,339,971,305,247,302,289,199,141,401,412,208,388,144,711,648,564,592,767,615,1019,350,136,946,235,364,957,560,48,661,522,506,82,420,86,308,897,178,310,145,155,537,214,314,422,313,20,484,889,579,792,803,233,879,608,245,392,12,901,843,496,394,653,408,978,461,45,703,862,969,568,686,198,710,536,1012,944,712,65,547,380,30,1004,855,517,479,317,354,97,981,621,797,909,334,699,761,649,239,565,591,766,559,896,41,997,5,891,42,376,955,785,641,644,1013,840,67,751,570,998,458,780,913,730,593,868,816,866,805,768,725,720,952,6,487,454,473,603,874,400,945,996,873,787,21,713,468,892,598,788,638,189,43,125,100,466,864,634,383,192,416,847,825,557,36,829,433,485,471,849,273,195,444,890,470,876,157,966,327,360,224,990,616,694,680,810,377,428,216,406,582,498,344,584,576,682,253,447,204,385,38,852,949,22,927,924,241,933,108,778,160,956,878,1003,620,590,784,124,326,989,809,877,53,714,775,54,964,158,323,449,169,1020,580,799,793,727,574,540,424,94,89,513,131,332,717,336,764,351,689,173,894,137,319,291,521,177,478,804,995,469,763,893,715,599,657,51,740,655,70,723,414,618,967,905,441,758,776,813,671,328,789,361,510,55,749,552,639,222,149,665,624,225,991,260,491,627,341,190,668,755,2,234,47,802,702,980,375,786,382,965,679,159,123,573,716,617,904,509,490,801,356,587,17,642,947,121,357,527,250,500,92,296,59,280,691,645,600,139,256,630,1014,921,451,773,974,651,404,275,236,658,230,366,842,495,44,66,932,52,539,88,667,365,524,543,880,324,695,476,936,819,741,548,1000,346,418,899,126,301,411,312,353,194,681,783,168,656,748,626,381,450,841,436,733,101,321,609,554,68,504,588,752,258,170,831,269,525,74,18,860,845,71,544,606,1021,986,706,437,857,31,734,105,284,201,1005,102,246,660,685,908,643,724,467,415,443,427,581,948,619,322,688,520,812,800,856,571,960,794,610,555,518,463,371,164,288,563,578,232,11,393,968,567,943,64,379,865,872,597,851,172,69,122,295,973,494,999,985,705,811,287,378,675,181,676,906,728,958,358,561,162,429,277,528,635,881,175,13,264,217,1017,647,505,516,316,457,575,384,589,325,512,586,16,403,410,193,73,442,370,49,251,480,182,677,531,1009,439,662,152,738,696,759,753,9,112,888,244,407,465,583,777,1002,318,477,50,148,508,355,249,499,542,345,417,104,907,596,263,585,541,595,459,330,781,84,34,821,398,98,902,914,837,814,672,911,746,731,937,501,349,307,460,40,954,729,453,602,848,203,93,331,259,1,523,844,684,959,577,171,151,594,329"
    val test_SA_ints = test_SA.split(",").map { _.toInt }

    for (i <- 0 until test_SA_ints.length) {
      if ( test_SA_ints(i) != sa.SA(i + 1 )) {
        assert(false,"Problemma %d. %d != %d".format(i, test_SA_ints(i),sa.SA(i + 1 )))
      }
    }
    
    

    
    //printf("SAAAAAAAA - %d,%d\n",sa.SA(301),sa.SA(302));

    /*
    val ranks = sa.convertSA2Rank(sa.SA) map { _ - 1 }
    val j = ranks.indexOf(300)
    println(j,ranks(j-1),ranks(j),ranks(j+1))
    */
    val bwt = (bwt1.sa2bwt2(sa.SA).map {_.toChar}).mkString("")
    //assert ( (bwt  == 
    //
    //)
    val ts = "grsnrscentpeikacihcxmyhneewbcbzhkagslluqrgtjeyvicrbrmyvskjdktzlfiersczjmivlskxddehdhrqofhowzvhugtfcladijsoshdqeiclxtensqlkkkcuqaoicrxwoxvvapviponkybslspgkvujkqticdnsvxblujtdavrgzzgngjhzinrthdhmcikebtbzsqzvswjxjyahpsgbgzppalammbhbhhhxtacbqvogybhffgbvqhbsgktdqudfjjfdwhhkxgymrlfgjkwpmrvwfgcottgqvvfhvvyevwzxoixiasakagiobbodhufrzfmslthrpajbzzrkbjvxytpjgofdgjqahiusmbtdmctnhykhgmbtldvpycjsfktsoqjfgzqgxyqhoggliyfhdfttynfqhvgbwrztykhijvuwmeouiehmxwnnajzfoseqnsvrxcgbznrckamglbudbywcswqeycwhttwiyfztsdwwvbahfwkwbwcqgxrxudsqivbcjkgkqedhzogpnszqcvmgtbinxjhzkcfwrkvaenfzfzoemrwslmvnwohfnqeyhbshvdggbgdlpzjuvlrjfsqacqkisqnvhivagyxuabzxdwvoajitcysnqyvxusxejptohqcpvcuklaldstjztxizsdrkvwrkygvgylgoeoqouyibweispsgeqvjpopsquaohyefppzsudotjzlgsafjnuqdqwarpeyivyxxnkbsbyyawjjgnrhaineudwexhkrctuwomhrgbdkzylqpchcrmwyycoaxdexpahrhdmqnjhohlbnbsneqdgtxsazrtgqwwuvmuyagmuljzysrorxaqsruigwqmnzquumfxcwbzjjlrjedwofygzusidjnegpepfbwrqkskjpisotglypacufijqoutayfheodrbksmwwbhgkzetrufhfepgprdmtxcamepypojwygpqznznabagmlndrrcgntsedydgwgdohsyugklawpucnr"
    for (i <- 0 until ts.length) {
      if ( ts(i) != bwt(i)) {
        printf("%d. %c != %c\n",i,ts(i),bwt(i))
        
      }
    }
  }
  
}
