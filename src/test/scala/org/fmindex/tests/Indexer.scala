package org.fmindex.tests

import org.scalatest.FunSuite
import org.fmindex._
import scalax.file.Path
import scalax.io._
import org.fmindex.Util._
import scala.collection.mutable.BitSet
import java.io.File

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

class KMP extends FunSuite with RandomGenerator {
  test("kmp creation") 
  {
    KMPBuffer.init(randomAlphanumericString(1025).getBytes)
  }
}
class BadTest extends FunSuite with RandomGenerator {
  

  
  /*  
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

    // Inverted suffix array t[i..] suffix rank on SA
    val isa = sa.convertSA2Rank(sa.SA) map { _ - 1 }
     
    
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

    val test_ranks_text = 
    """c=t cur_rank = 756
    9. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 828+occ(755,v) = 828+40 = 868
    8. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 663+occ(867,r) = 663+34 = 697
    7. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 620+occ(696,q) = 620+29 = 649
    6. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 119+occ(648,d) = 119+24 = 143
    5. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 985+occ(142,z) = 985+4 = 989
    4. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 232+occ(988,g) = 232+53 = 286
    3. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 794+occ(285,u) = 794+6 = 800
    2. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 486+occ(799,m) = 486+20 = 506
    1. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 160+occ(505,e) = 160+15 = 175
    0. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 419+occ(174,k) = 419+11 = 430
    """
    val tra = test_ranks_text.split("\n")

    val lastSym = bwt1.S(bwt1.length-1) 
    assert(lastSym == 'g')
    val rkLast = isa(bwt1.length-1)
    assert(rkLast==275)
    val rkFirst = isa(0)
    assert(rkFirst==290)

    val tRank0 = tra(0).split(" ")(3).toInt
    val tC0    = tra(0).split(" ")(0).split("=")(1)(0).toByte

    var j = bwt2.length -1
    var c = bwt2.S(j)
    var cur_rank = bwt1.bucketStarts(c)
    println(bwt2.S(j).toChar,bwt2.S(j-1).toChar,bwt2.S(j-2).toChar,bwt2.S(j-3).toChar,bwt2.S(j-4).toChar)
    assert(c==tC0,"c0 should:%c is:%c".format(tC0,c))
    assert(tRank0==cur_rank,"cur_rank should:%d is:%d".format(tRank0,cur_rank))


    val DR = """.*?compute_prefix_rank\(.*?,.\) (\d+)\+occ\((\d+),(.)\) = \d+\+(\d+) = (\d+).*""".r
    
    case class cinfo(cfirst:Int,oldrank_1:Int,c:Byte,occ:Int,rank:Int)

    val testelems = tra.slice(1,tra.length).map  {
      case DR(cfirst,oldrank_1,c,occ,rank) => cinfo(cfirst.toInt,oldrank_1.toInt,c(0).toByte,occ.toInt,rank.toInt)
      case d => {
        println(d)
        cinfo(-1,0,'a',0,0)
      }
    } filter {_.cfirst>0}
    j-=1
    println(bwt2.GT_TN.map{ if (_) "x" else "." }.mkString(""))
    for (i <- 0 until testelems.length) {
      val te = testelems(i)
      c = bwt2.S(j)
      println(j,c.toChar,te)
      assert(c==te.c,"%d. c should be '%c' but '%c'".format(i,te.c,c))
      val cfirst = bwt1.bucketStarts(c)
      assert(cfirst==te.cfirst)
      assert(te.oldrank_1 == cur_rank - 1 , "%d. oldrank_1 should be %d but %d".format(i,te.oldrank_1,cur_rank-1))
      val occ = bwt1.occ(c.toByte,cur_rank-1)
      assert(occ == te.occ)
      cur_rank = cfirst + occ
      if (lastSym == c) {
        if (bwt2.GT_TN(bwt2.length-j-1)) cur_rank+=1
        /*
        if (cur_rank == rkLast) {

        } else if (cur_rank>rkLast) {

        } else {

        }
        */
      }
      j-=1
      assert(cur_rank == te.rank, "%d. curRank should be %d but %d".format(i,te.rank,cur_rank))
    }
    
  }
  */
  
  test("BWTMerger.build.OF") {
    // Added Part
    val d1 = "dmbrrlwquggscrxmlqhbthpvqpccggedmiiuhwkugzvvcoaidwukjndzrggenybtxnhzewxfrpmfqbvidcuqppyimdjopzkqeepqkoysvktfvcjhfgmagfceoxwxwrfcpsomedsprbdkdzfyaidpeqejiirehsftrvftpnosrbbdwqsiibrzkbghqkkdtzqqgylomrmvcaevemhryuunlfeicakcocuhhneftmbdrnrtlhlaqucsgoutcaxhgbdpdqoqwjexhwynitbnrstxvseybzymijnfcskrzsiwsoelxtmrzzgcoqkvjhhyrnpeabgqbwdktlmbouwxoxtrpuuvetdergqfwdbsmwkaiuubiwaibsgkqjzbfvdovjhkciinbvlnpwhbnlsvpmwrliounhttpyziliwqgctzppupwyvdcxhnpiyqwjblggeulgcdtzzztrkpimjindrzkvpmhjfnchfvdgvimhrskngozynqrcytsufilaykzsymnythtmpmyeoishybfgckyttgvwufondqmffpyqiaqnrlchotunbigkdnvtqymbsshxxcjwkgtxokamvdoxupmmomyjrkcxjoiaspqpnoztdnjnrsbgrnofdkmixgwclcymkrcrmaurdbtuomdjbcvetwnduroxbczpvnwubqxqusylohjtkbhpupqdqhdkjstflvayszfmyammhxomkvgbcgkhgqjhfjsirphkgmnvgceowlrotjvtfazmczbqtcgstfvlbgpxqmnsdawsvqoyiilbsmbvvjokxhnusthyqbhrhvbkbtehtvegsxxjwmlujtrowoxskinhfahiegkaezvrjqnbsigpuusflioemmgobeziizwfmstqabvweprtgdqzlvvrwhnqnuynuoavbqrytqbgfopzuvnavdlehfviofzdqyhxtnqbrxafzgfvmrjfbqfrstcajkkggtdmpawtikjukeumeikuooyhxwaphkhjnagqbaiozrpzyn"
    // old Part
    val d2 = "uexmskeanytsljkujjpzjerzfjybohylttnxdtwjxeapudycsqtutuqgscdlpqtaqadxqwkokeaeylzbhkdrrcrxroixlodjjmgungyoehlnhlmcjaackicemmmxaehbvbchbhtmldfcikyzcfgqtviwzjpebtuaygnmpoambsbjhtekmoexqnolkphpzzzfgicfsbwljvavutuekegiobwhxuieruntdpadabrtemfqhhsgbbhpfeirfczbstoxesmyapxkrvwesixntlwihwjkajnentkvnjkvmqgvuhiyacrgstcockwrlohaaxfnkvdpdezfmtklggjemzpjrnakdxbfgijvlvidnlyqhjwllvumeixynjosngcakumzdkmuksrkyfcznvvnmcenvksyujfwubgpgcbujwapflxfwxvwplzfilwyvdspmpzipyfptgbkuslqkrdsnvrmwhptcbhrlswvzxzpczjxkuoecvlmooxnhsojhfbmrwbdihympbtsagytrhmjmamhfyymxzxhzpiquirwgafxgvwugvgndwnmcmkszzoiddspoxouznkuxpsdavpeigufarscdpymjnqufgdnlhgaouysnqkdaosjeezyffyjwzzdrbdevcgqlketqyljtldyropcgnmuorsjinagieukisgyjsfxhivedoqorykpeyjkglzqnmcbsezxzevypffbspaifwgxjdgahupdrmiqypisslzdcaehaabqxxayhdmeexzlsyibsoknrfsgczhlchysqlwazgjsykmybajtnxkorxjusuphlgoqavnbkwdrvqapmhnhjzpbvxgcswgnkgeqwsanimyryrxujjhhqitlkorlzetwfcgxrkyvtmrjzgbknlvcxolwkyopzflepisiwiazajrtzbeqwbxdfpwjflmmsseewuudgfbtzqenjqafwzcnfanycigwsflfvxojxpqhhzekjdkhgsptqveavquuoqujbezdkarayoml"
    val bwt1 = new BwtIndex(data=d1.getBytes.reverse)
    val bwt2 = new BwtIndex(data=d2.getBytes.reverse)
    
    val gt_eof = BWTMerger.compute_gt_eof(bwt1,bwt2)

    val (remapped,k) = BWTMerger.remap_alphabet2(bwt1,gt_eof)
    
    val tremap = "15,26,27,17,19,27,16,10,0,1,18,8,0,15,11,9,12,9,17,0,24,25,9,26,16,16,22,12,10,6,14,22,6,12,22,11,12,10,21,24,0,17,14,5,21,8,8,12,12,11,0,2,21,20,19,7,18,1,7,11,19,14,23,7,8,27,7,0,25,19,1,18,15,21,25,9,26,18,5,27,7,16,10,23,7,9,6,13,5,23,0,15,23,22,27,17,16,7,8,1,18,21,26,19,18,1,23,0,16,22,15,26,22,15,18,15,9,24,19,23,23,13,27,18,3,8,21,19,17,6,24,23,1,0,18,21,20,14,7,24,27,10,10,27,6,1,16,8,14,14,6,16,10,13,7,20,22,22,17,8,10,20,1,15,18,11,19,23,27,6,0,12,8,6,10,9,0,7,9,15,10,12,20,25,16,24,16,19,21,11,22,13,14,24,11,25,25,20,8,6,23,21,9,6,21,1,12,1,23,9,19,9,1,18,26,9,21,20,22,15,9,25,12,16,11,23,23,1,14,20,1,13,10,10,26,16,18,23,20,24,0,5,20,15,14,18,25,17,8,1,13,23,7,21,20,8,2,21,18,1,27,2,14,27,0,7,21,23,11,21,16,19,13,24,16,6,2,8,23,15,14,8,12,9,17,19,10,20,11,7,9,11,18,8,9,12,8,2,1,8,23,12,14,16,25,9,14,14,0,26,14,7,27,20,26,0,23,13,7,21,20,11,12,3,9,18,5,18,17,22,17,9,1,12,21,11,9,16,13,26,20,22,18,25,18,1,22,24,15,23,17,27,2,1,25,16,19,22,5,15,24,21,6,23,2,1,11,5,14,16,22,21,1,5,19,22,0,14,19,2,19,12,14,26,2,13,2,24,8,25,10,14,12,3,7,16,15,19,8,1,20,19,15,11,15,5,21,27,16,15,17,18,17,20,0,10,16,11,25,2,12,19,11,26,14,16,14,14,17,22,25,16,5,23,14,0,12,16,25,21,8,12,24,11,2,25,25,9,20,20,1,14,26,18,21,23,15,3,12,8,10,1,15,22,21,16,9,2,13,19,15,18,0,10,18,26,17,7,7,14,18,5,15,16,7,22,24,23,8,21,21,26,12,2,8,7,1,26,9,20,10,16,6,26,14,17,14,21,9,21,26,15,14,26,20,27,12,26,0,13,10,7,22,20,21,26,2,19,18,15,26,27,16,8,15,12,20,19,9,14,10,23,8,5,23,7,9,2,15,7,11,9,14,17,23,12,27,19,5,15,10,11,14,10,17,12,19,21,27,27,27,21,3,2,8,13,22,6,8,8,13,1,11,24,18,26,10,17,15,9,25,2,5,23,26,24,17,22,17,17,27,21,2,8,18,24,10,13,10,27,26,17,21,21,9,15,22,16,10,13,19,24,14,17,23,20,13,15,1,9,24,17,15,13,23,1,15,10,10,2,12,9,11,23,16,5,23,7,1,27,11,18,12,8,20,1,10,0,24,10,1,22,22,10,0,12,24,14,20,1,5,24,7,18,8,19,6,5,21,6,23,22,22,17,19,21,25,16,25,24,22,16,1,14,13,21,12,5,24,1,18,8,1,0,6,17,15,19,26,9,9,11,23,12,18,16,2,8,27,27,19,14,21,25,13,6,16,20,24,10,20,27,19,12,20,2,7,15,11,10,14,26,27,1,26,6,20,23,25,21,20,19,15,1,21,10,15,26,24,9,25,6,11,24,18,16,18,5,17,3,1,8,9,25,0,2,21,22,16,8,20,2,22,18,0,13,9,13,21,19,15,19,3,1,14,21,7,6,15,9,9,22,2,16,2,12,0,2,10,6,7,13,15,22,22,26,19,9,14,6,23,6,0,2,23,14,19,14,16,13,26,8,18,18,27,21,3,12,12,18,9,8,1,12,27,19,1,10,10,20,18,24,3,1,1,19,20,16,15,17,21,7,23,19,21,7,20,9,6,19,10,10,11,6,18,6,17,3,10,0,26,7,27,3,12,3,1,19,17,20,3,6,14,16,20,17,2,7,19,24,25,24,25,16,6,2,7,8,0,14,8,7,9,11,2,23,7,21,12,23,20,26,16,12,18,17,6,6,18,12,27,17,16,11,5,14,10,26,17,17,18,22,2,3,10,23,1,18,7,14,17,19,7,25,24,6,27,9,15,25,21,1,26,15,6,8,8,19,27,5,15,11,12,22,24,3,10,0,16,2,23,23,27,8,22,12,24,9,22,10,10,14,3,6,8,8,2,2,17,18,23,17,9,21,1,9,18,13,14,25,19,2,20,8,8,22,18,24,13,19,19,1,14,4"
    val tremap_ints = tremap.split(",").map({_.toInt})

    for (i <- 0 until tremap_ints.length) {
      assert( remapped(i) == (tremap_ints(i) +1) ,"remapped %d. %d != %d".format(i,remapped(i),tremap_ints(i)+1))
    }

    val sa = new SAISIntBuilder(new IntArrayWrapper(remapped),k)
    sa.build()

    val test_SA = "8,806,50,774,822,240,703,176,264,414,472,170,435,664,784,518,908,376,12,90,977,107,40,133,315,657,19,67,879,308,702,132,853,372,669,57,770,298,1004,628,655,846,365,581,205,332,842,230,249,1021,692,227,793,450,635,162,461,145,946,9,699,70,99,212,886,854,399,753,660,345,105,207,353,743,496,961,259,648,297,364,352,996,942,591,905,735,896,493,573,602,276,716,807,804,639,419,384,467,261,547,802,997,379,526,1011,256,51,775,781,914,823,979,386,444,852,769,792,885,572,992,890,393,124,323,975,877,943,883,457,836,1023,934,367,558,969,481,358,767,326,373,241,677,43,405,88,645,543,432,592,697,670,78,821,169,144,904,275,676,926,809,993,577,964,173,761,32,86,891,29,797,150,725,875,704,873,927,868,745,203,362,819,199,679,129,502,955,66,495,647,796,477,906,97,63,545,84,911,289,177,549,58,810,948,478,736,80,394,55,672,897,865,154,916,252,318,265,521,484,861,138,952,881,311,907,11,701,841,248,98,398,296,995,255,542,172,198,494,910,994,45,578,965,1013,293,771,459,159,281,46,440,579,574,147,533,831,603,674,966,653,779,125,488,983,1014,299,277,388,64,717,175,331,211,466,546,85,867,202,840,709,799,912,290,710,641,294,15,786,817,538,305,551,178,614,957,336,17,283,324,1005,209,498,447,1002,215,508,800,987,629,116,772,589,759,220,22,75,7,663,976,656,878,460,659,638,808,28,520,174,637,870,989,847,232,141,871,560,180,152,606,618,990,390,739,755,500,415,563,586,473,160,286,848,729,37,944,82,540,233,936,142,608,49,913,443,933,366,872,288,14,550,335,738,321,35,971,561,403,291,650,59,165,268,189,711,642,224,762,582,417,194,422,805,206,492,884,392,322,696,295,171,458,652,640,16,282,27,36,48,47,837,301,381,222,436,838,713,923,420,565,733,535,181,333,33,972,918,985,441,665,516,929,843,555,580,385,87,724,153,317,785,519,231,607,191,1007,626,811,468,1018,619,694,787,575,633,250,272,829,338,121,434,307,991,1022,42,149,818,137,310,909,280,562,539,935,391,693,306,148,426,827,424,892,368,302,504,949,427,552,622,479,244,377,825,667,228,794,506,721,30,61,192,1008,382,451,512,740,262,627,752,456,404,963,548,798,115,588,219,636,559,179,13,737,970,402,534,632,279,243,511,482,409,858,470,163,113,790,396,706,72,615,462,812,348,91,359,958,110,756,529,0,691,715,803,978,644,431,903,274,501,96,483,146,532,778,465,6,151,617,81,932,223,416,922,828,337,425,408,857,395,24,765,235,270,186,355,893,726,25,108,369,184,303,437,687,18,895,768,876,925,128,476,247,158,330,1001,564,41,505,587,631,705,95,931,938,598,410,939,998,950,284,683,3,412,888,859,611,328,596,428,553,623,350,599,471,783,56,344,104,258,123,480,766,325,77,874,947,10,700,292,673,839,164,651,928,1006,114,71,528,714,764,924,411,327,832,134,453,100,940,999,236,850,604,1016,245,342,213,584,474,833,845,1020,69,378,1010,791,557,675,54,951,397,210,816,537,869,285,421,380,732,271,826,720,60,751,401,469,789,127,887,103,527,1019,855,863,187,684,566,374,356,118,166,620,898,707,967,4,413,668,654,229,449,161,734,780,889,254,197,1012,866,499,287,320,625,136,242,856,894,849,53,536,750,400,448,523,217,340,155,746,238,727,182,313,920,514,730,371,1003,204,960,601,571,835,361,678,795,864,860,44,439,201,613,507,754,334,188,695,917,464,269,257,788,126,253,319,135,52,749,216,612,489,776,266,454,38,73,722,685,524,490,509,101,406,567,375,941,801,357,576,31,662,988,34,26,984,190,218,112,109,690,777,616,157,329,597,682,782,1015,341,522,370,463,661,156,681,813,973,346,485,429,814,93,89,106,131,226,634,945,363,820,646,62,544,83,915,251,541,487,208,267,300,712,554,316,120,433,824,455,278,643,1000,349,862,624,237,919,200,680,92,225,119,980,747,593,167,981,239,39,698,851,974,954,671,387,986,758,658,605,728,442,193,1017,621,666,347,273,185,630,595,763,583,117,360,689,130,486,20,901,899,139,773,590,418,760,304,446,21,74,389,221,723,430,902,354,183,686,246,343,68,1009,196,959,438,748,953,688,900,445,195,517,314,383,525,744,880,830,708,497,214,585,491,309,423,503,962,510,921,23,234,475,937,610,76,452,815,102,339,513,111,757,594,741,530,1,263,742,351,260,882,968,168,143,65,79,982,956,140,649,515,531,5,407,94,930,2,122,844,556,731,719,312,600,570,834,609,718,569,568"
    val test_SA_ints = test_SA.split(",").map { _.toInt }

    for (i <- 0 until test_SA_ints.length) {
      if ( test_SA_ints(i) != sa.SA(i + 1 )) {
        assert(false,"Problemma %d. %d != %d".format(i, test_SA_ints(i),sa.SA(i + 1 )))
      }
    }
    
    val ranks = sa.convertSA2Rank(sa.SA) map { _ - 1 }
    val test_ranks = "547,989,1010,619,722,1006,563,315,0,59,644,224,18,518,367,285,402,295,592,26,922,932,313,973,577,585,819,404,324,168,496,815,165,422,818,372,405,352,800,893,22,604,462,142,774,239,248,407,406,360,2,106,792,745,685,207,633,35,200,378,699,497,857,193,267,998,186,27,944,679,61,654,536,801,933,314,978,641,151,999,205,566,354,859,195,274,166,434,144,848,19,541,884,847,1008,609,557,192,228,62,664,807,981,706,635,70,849,21,586,824,544,984,823,532,653,512,308,917,716,886,870,457,1011,637,122,260,788,704,597,183,920,850,31,23,662,791,740,465,219,925,1002,332,358,997,154,57,559,252,475,463,170,564,336,436,211,753,839,828,600,246,348,728,55,531,649,379,717,890,996,153,11,398,234,163,326,269,7,198,291,517,335,420,757,940,588,912,581,711,781,381,821,442,498,906,388,954,946,733,235,181,882,776,276,178,764,44,391,71,864,299,688,271,63,673,964,303,794,751,822,514,312,935,411,568,384,885,851,51,492,726,47,440,331,356,974,579,667,880,755,892,5,140,741,525,488,671,942,599,227,48,453,861,213,789,732,232,105,786,636,76,993,98,504,990,8,215,798,865,380,785,580,696,454,911,555,156,90,265,874,524,468,247,403,296,617,692,349,737,366,197,281,376,646,243,284,397,230,78,37,264,866,409,481,589,930,289,474,459,29,967,466,222,1016,758,956,24,869,437,214,790,738,371,395,123,297,640,138,660,624,829,601,270,45,421,780,369,294,572,456,982,752,834,672,943,634,69,843,910,540,877,629,992,80,72,939,582,715,813,136,542,918,769,179,854,79,42,364,132,480,587,836,762,33,139,714,810,17,489,680,102,694,410,500,957,96,433,112,899,266,934,340,472,394,121,206,576,534,687,229,66,748,701,521,375,508,143,808,1007,574,528,613,659,620,723,9,344,569,387,928,95,416,693,389,968,478,573,476,484,626,845,937,553,147,871,458,12,412,590,948,775,249,426,905,362,113,953,931,301,749,727,53,501,979,663,799,873,507,128,399,245,320,56,538,837,784,562,272,97,446,702,530,631,10,347,675,975,598,190,203,487,638,135,527,558,217,844,921,863,261,796,805,966,392,87,236,187,74,963,300,736,343,556,184,969,482,605,494,778,304,806,971,526,502,983,760,1004,428,955,15,439,325,216,835,750,804,958,103,707,655,546,988,1005,560,253,522,419,746,690,288,470,355,862,233,146,858,194,273,99,510,199,368,290,485,627,868,431,1013,683,133,516,334,374,469,345,603,417,713,809,1023,1022,1018,767,118,88,251,451,814,161,240,250,432,43,386,916,674,965,346,606,513,310,927,83,148,889,986,914,625,830,612,630,1017,766,89,255,669,903,337,441,359,1020,977,623,795,777,292,537,827,565,338,448,718,908,486,628,879,739,444,505,39,307,913,607,523,452,852,54,515,327,322,94,401,283,383,875,552,145,856,188,77,1003,377,650,400,258,725,40,318,25,902,321,68,838,816,316,13,427,909,491,724,34,150,898,208,647,256,684,157,141,770,182,883,840,831,618,712,803,941,591,951,919,825,548,50,473,449,782,396,149,894,60,645,225,30,6,173,608,535,720,962,278,282,382,867,414,656,549,91,268,1021,1015,698,495,802,936,435,171,584,756,904,351,761,1014,695,418,729,85,204,519,370,341,503,987,991,73,959,177,754,888,949,793,747,700,506,67,779,342,545,985,901,311,929,164,385,915,657,578,639,137,594,115,36,244,309,926,3,107,797,826,561,259,730,108,832,632,14,438,286,450,787,703,533,682,116,52,493,771,189,169,511,279,305,812,100,550,93,390,1,92,323,159,201,445,539,841,846,980,689,287,464,180,855,152,4,110,872,490,697,477,571,455,961,254,661,676,1019,768,129,408,413,648,277,226,46,430,1012,677,41,330,350,744,668,895,114,32,65,709,742,575,529,622,773,218,878,710,772,210,735,275,176,691,328,333,365,174,642,172,595,125,319,28,960,221,994,127,393,117,64,705,621,731,120,167,479,583,743,593,86,209,719,924,952,923,938,554,155,84,191,223,16,467,237,196,280,361,109,860,212,783,424,881,759,972,570,415,658,596,158,175,651,429,1009,610,567,363,131,471,357,976,611,614,665,811,82,126,353,853,58,643,202,483,616,686,220,950,897,185,1001,293,543,947,765,75,970,509,162,241,257,721,995,134,520,373,423,842,896,124,317,20,551,111,887,891,1000,262,820,425,900,306,817,329,339,460,119,160,238,231,81,101,615,666,876,602,302,763,38,298,652,443,499,945,681,104,734,242,263,833,670,907,447,708,678,49,461,130"
    val test_ranks_ints = test_ranks.split(",").map { _.toInt }

    for (i <- 0 until test_ranks_ints.length) {
      if ( test_ranks_ints(i) != ranks(i  )) {
        assert(false,"Problemma %d. %d != %d".format(i, test_ranks_ints(i),ranks(i )))
      }
    }

    val bwt = (bwt1.sa2bwt2(sa.SA).map {_.toChar}).mkString("")

    val ts = "ikjxewbhzsqemiqygugvivwbyipfimgvdtsqdctnsrclthgsgrovdsvsievawrghdbgniqqkczftqfgvzguxespkdteoaoixyhzhucryrgaasjaoljwprktmskqkwpczntmjjrzquqqbaemnlogockbqvzzoorpidungxuhdifmlqajehyhtmgtpowzgvtpcovvvgjanbeqfczdrwctlvvlaiotmrymfqqhpfrkgsvkscmeteesqbkpmgtgcooycqgkodvzgbcwfciproffstqynffhkgjlrrxjftzjkkdbvyxpythwbngnwnxxoudbdgwicklenrublzinnowoixjtsamyagriwkdomimilkhwobisnftnsuninhzfqvthhoebowrcbydmjthadqchgujkgdvrxakvocprnibejtugazqbvgcexivaabiuqsfcwimhgnbroovvmibpmhsyanjhdibhgoryedkyfmhwfnavwbbprerllkbniclrvjyceqpubdhafdrgpvsydoorburopqhblwadhuiqquqcavxxwipnbzutzeufpkiymhmzsfyqytwxmeoamxmkxhsdeqryxuuviamiwezzyunpcmhuzqrnyqwmmmvpnufxrtzmohyebbbjfknjehnbrkwkpdgaybpposgumubwiqzrxmxnzgspnhysepkczomzjsslttbyclbvopkdowjlfngppmgmsikgptxcfhitvtdroitktbhutyfevokzvyiuhexzzzwdmrpdxvtmbkrlfujclgffqcxhpgcfqinmrsthqdrrqhrlmuhkogjsyowtnuppucgsfonbsvnkbfpuvdbwvlieedmdicliwbtgjpavdctgjqnfpqkeenjrcsdrvstdquxdckyaqskmqrkulohyjjhnxeuaxrfhhjhoxwtghtuwbstqqamxnovfowcjksmtbalrbqqtajebtshiqizhmutlmnnvmnnmypbfrvigdvewbsyrtukylkkszfpzqigzt"
    for (i <- 0 until ts.length) {
      if ( ts(i) != bwt(i)) {
        printf("%d. SHOULD:%c != IS:%c SA=%d\n",i,ts(i),bwt(i),sa.SA(i+1))
        assert(false)
      }
    }

    val start_sa_range = Array(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,30,78,114,152,186,223,269,315,360,390,432,458,505,548,592,631,677,723,762,810,848,892,926,955,990,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024)

    for (i <- 0 until (start_sa_range.length min bwt1.bucketStarts.length) ) {
      if ( start_sa_range(i) != bwt1.bucketStarts(i)) {
        printf("%d. SHOULD:%d != IS:%d \n",i,start_sa_range(i),bwt1.bucketStarts(i))
        assert(false)
      }
    }

    val isa = sa.convertSA2Rank(sa.SA) map { _ - 1 }
    //println(isa.mkString(","))

    val test_ranks_text = 
    """c=t cur_rank = 756
    9. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 828+occ(755,v) = 828+40 = 868
    8. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 663+occ(867,r) = 663+34 = 697
    7. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 620+occ(696,q) = 620+29 = 649
    6. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 119+occ(648,d) = 119+24 = 143
    5. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 985+occ(142,z) = 985+4 = 989
    4. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 232+occ(988,g) = 232+53 = 286
    3. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 794+occ(285,u) = 794+6 = 800
    2. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 486+occ(799,m) = 486+20 = 506
    1. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 160+occ(505,e) = 160+15 = 175
    0. cur_rank=cfirst+compute_prefix_rank(cur_rank-1,c) 419+occ(174,k) = 419+11 = 430
    """
    val tra = test_ranks_text.split("\n")

    val lastSym = bwt1.S(bwt1.length-1) 
    assert(lastSym == 'd')
    val rkLast = isa(bwt1.length-1)
    assert(rkLast==130,"rkLast should:%d is %d".format(130,rkLast))
    val rkFirst = isa(0)
    assert(rkFirst==547,"rkFirst should:%d is %d".format(547,rkFirst))

    val tRank0 = tra(0).split(" ")(3).toInt
    val tC0    = tra(0).split(" ")(0).split("=")(1)(0).toByte

    var j = bwt2.length -1
    var c = bwt2.S(j)
    var cur_rank = bwt1.bucketStarts(c)
    println(bwt2.S(j).toChar,bwt2.S(j-1).toChar,bwt2.S(j-2).toChar,bwt2.S(j-3).toChar,bwt2.S(j-4).toChar)
    assert(c==tC0,"c0 should:%c is:%c".format(tC0,c))
    assert(tRank0==cur_rank,"cur_rank should:%d is:%d".format(tRank0,cur_rank))


    val DR = """.*?compute_prefix_rank\(.*?,.\) (\d+)\+occ\((\d+),(.)\) = \d+\+(\d+) = (\d+).*""".r
    
    case class cinfo(cfirst:Int,oldrank_1:Int,c:Byte,occ:Int,rank:Int)

    val testelems = tra.slice(1,tra.length).map  {
      case DR(cfirst,oldrank_1,c,occ,rank) => cinfo(cfirst.toInt,oldrank_1.toInt,c(0).toByte,occ.toInt,rank.toInt)
      case d => {
        println(d)
        cinfo(-1,0,'a',0,0)
      }
    } filter {_.cfirst>0}
    j-=1
    println(bwt2.GT_TN.map{ if (_) "x" else "." }.mkString(""))
    for (i <- 0 until testelems.length) {
      val te = testelems(i)
      c = bwt2.S(j)
      println(j,c.toChar,te)
      assert(c==te.c,"%d. c should be '%c' but '%c'".format(i,te.c,c))
      val cfirst = bwt1.bucketStarts(c)
      assert(cfirst==te.cfirst)
      assert(te.oldrank_1 == cur_rank - 1 , "%d. oldrank_1 should be %d but %d".format(i,te.oldrank_1,cur_rank-1))
      val occ = bwt1.occ(c.toByte,cur_rank-1)
      assert(occ == te.occ)
      cur_rank = cfirst + occ
      if (lastSym == c) {
        if (bwt2.GT_TN(bwt2.length-j-1)) cur_rank+=1
        /*
        if (cur_rank == rkLast) {

        } else if (cur_rank>rkLast) {

        } else {

        }
        */
      }
      j-=1
      assert(cur_rank == te.rank, "%d. curRank should be %d but %d".format(i,te.rank,cur_rank))
    }
  }
  
}



class MergerTest extends FunSuite {
  import java.io.File

  class BWTLoader(f:File,bigEndian:Boolean=true) {
    val in = new java.io.FileInputStream(f)
    val inb = new java.io.DataInputStream(in)
    val size = if (bigEndian) inb.readLong() else java.lang.Long.reverseBytes(inb.readLong())
    val eof =  if (bigEndian) inb.readLong() else java.lang.Long.reverseBytes(inb.readLong())
    val b   = new Array[Byte](size.toInt)
    val rdn = inb.read(b)
    assert(rdn==size)
    inb.close
    in.close
    def == (that: BWTLoader): Boolean = size == that.size && eof == that.eof && b.sameElements(that.b)
  }

  class AUXLoader(f:File,bigEndian:Boolean=true) {
    val in = new java.io.FileInputStream(f)
    val inb = new java.io.DataInputStream(in)
    val occ   = new Array[Long](BWTMerger2.ALPHA_SIZE)
    
    for (i<-0 until occ.length) {
      occ(i) = if ( ! bigEndian ) java.lang.Long.reverseBytes(inb.readLong()) else inb.readLong()
    }

    inb.close
    in.close
    def == (that: AUXLoader): Boolean = occ.sameElements(that.occ)
  }

    test("FileBWTReader") {
      val r = new FileBWTReader("testdata/test.txt")
      var d = new Array[Byte](10)
      val n = r.copyReverse(d)
      assert(n == d.length,"n = %d != %d  ".format(n,d.length))
      val ts = d.reverse.map {_.toChar}  mkString("")
      assert(ts == "uexmskeany")
      
      val n2 = r.copyReverse(d)
      assert(n2 == d.length,"n = %d != %d  ".format(n2,d.length))
      val ts2 = d.reverse.map {_.toChar}  mkString("")
      
      assert(ts2 == "tsljkujjpz" , "ts2 = " + ts2)
    }

    test("FileBWTReader.small") {
      val r = new FileBWTReader("testdata/small.txt")
      var d = new Array[Byte](15)
      val n = r.copyReverse(d)
      assert(n == 10,"n = %d != %d  ".format(n,10))

      val ts = d.map { _.toChar }.slice(5,15).reverse mkString("")
      assert(ts == "missisippi" , "ts2 = " + ts)
      assert(r.isEmpty)
    }
    test("BWTMerger2 small file") {
      val r = new FileBWTReader("testdata/test1024.txt")
      val bm = new BWTMerger2(1024)
      val (of,af) = bm.merge(r)
      val bl = new BWTLoader(of)
      val tbl = new BWTLoader(new File("testdata/test1024.cmp.bwt"),false)
      assert(bl == tbl)
      val al = new AUXLoader(af)
      val tal = new AUXLoader(new File("testdata/test1024.cmp.aux"),false)
      assert(al == tal)
    }

    test("BWTMerger2.calcSA") {
      val bm = new BWTMerger2(1024)
      val d1 = "barbaarbadaacarb".reverse.getBytes
      val occ = bm.calcOcc(d1)
      assert(occ.view.slice(95,102).sameElements(Array(0,0,7,4,1,1,0)))
      val sa = bm.calcSA(d1)
      assert(sa.sameElements(Array(10,4,14,7,11,2,5,15,8,12,0,3,6,9,13,1)))
      val newRank0 = sa.indexOf(0)
      assert(newRank0==10)
    }

    test("BWTMerger2.remapAlphabet") {
      val bm = new BWTMerger2(1024)
      val d1 = "barbaarbadaacarb".getBytes
      val d2 = "abaacarbadacarba".getBytes
      val sa = bm.calcSA(d2)
      val gtTn = bm.calcGtTn(sa.indexOf(0),sa)
      assert(gtTn.mkString(",")=="1,3,4,5,6,7,8,9,10,11,12,13,14")
      val gtEof = bm.computeGtEof(d1,d2,gtTn)
      assert(gtEof.mkString(",") == "0,1,2,3,5,6,7,8,9,11,12,13,14,15")
      val (remapped,asize) = bm.remapAlphabet(d1,gtTn)
      assert(remapped.sameElements(Array(3,1,6,3,1,1,6,3,1,5,1,1,4,1,6,2,0)))
      assert(asize == 7)
    }

    test("BWTMerger2 test2048 occ searcher") {
      val r = new FileBWTReader("testdata/test2048.txt")
      val bm = new BWTMerger2(1024)      
      val t1v = Array[Byte](110,121,122,112,114,122,111,105,97,98,113,103,97,110,106,104,107,104,112,97,119,120,104,121,111,111,117,107,105,101,109,117,101,107,117,106,107,105,116,119,97,112,109,100,116,103,103,107,107,106,97,99,116,115,114,102,113,98,102,106,114,109,118,102,103,122,102,97,120,114,98,113,110,116,120,104,121,113,100,122,102,111,105,118,102,104,101,108,100,118,97,110,118,117,122,112,111,102,103,98,113,116,121,114,113,98,118,97,111,117,110,121,117,110,113,110,104,119,114,118,118,108,122,113,100,103,116,114,112,101,119,118,98,97,113,116,115,109,102,119,122,105,105,122,101,98,111,103,109,109,101,111,105,108,102,115,117,117,112,103,105,115,98,110,113,106,114,118,122,101,97,107,103,101,105,104,97,102,104,110,105,107,115,120,111,119,111,114,116,106,117,108,109,119,106,120,120,115,103,101,118,116,104,101,116,98,107,98,118,104,114,104,98,113,121,104,116,115,117,110,104,120,107,111,106,118,118,98,109,115,98,108,105,105,121,111,113,118,115,119,97,100,115,110,109,113,120,112,103,98,108,118,102,116,115,103,99,116,113,98,122,99,109,122,97,102,116,118,106,116,111,114,108,119,111,101,99,103,118,110,109,103,107,104,112,114,105,115,106,102,104,106,113,103,104,107,103,99,98,103,118,107,109,111,120,104,109,109,97,121,109,102,122,115,121,97,118,108,102,116,115,106,107,100,104,113,100,113,112,117,112,104,98,107,116,106,104,111,108,121,115,117,113,120,113,98,117,119,110,118,112,122,99,98,120,111,114,117,100,110,119,116,101,118,99,98,106,100,109,111,117,116,98,100,114,117,97,109,114,99,114,107,109,121,99,108,99,119,103,120,105,109,107,100,102,111,110,114,103,98,115,114,110,106,110,100,116,122,111,110,112,113,112,115,97,105,111,106,120,99,107,114,106,121,109,111,109,109,112,117,120,111,100,118,109,97,107,111,120,116,103,107,119,106,99,120,120,104,115,115,98,109,121,113,116,118,110,100,107,103,105,98,110,117,116,111,104,99,108,114,110,113,97,105,113,121,112,102,102,109,113,100,110,111,102,117,119,118,103,116,116,121,107,99,103,102,98,121,104,115,105,111,101,121,109,112,109,116,104,116,121,110,109,121,115,122,107,121,97,108,105,102,117,115,116,121,99,114,113,110,121,122,111,103,110,107,115,114,104,109,105,118,103,100,118,102,104,99,110,102,106,104,109,112,118,107,122,114,100,110,105,106,109,105,112,107,114,116,122,122,122,116,100,99,103,108,117,101,103,103,108,98,106,119,113,121,105,112,110,104,120,99,100,118,121,119,112,117,112,112,122,116,99,103,113,119,105,108,105,122,121,112,116,116,104,110,117,111,105,108,114,119,109,112,118,115,108,110,98,104,119,112,110,108,118,98,110,105,105,99,107,104,106,118,111,100,118,102,98,122,106,113,107,103,115,98,105,97,119,105,98,117,117,105,97,107,119,109,115,98,100,119,102,113,103,114,101,100,116,101,118,117,117,112,114,116,120,111,120,119,117,111,98,109,108,116,107,100,119,98,113,103,98,97,101,112,110,114,121,104,104,106,118,107,113,111,99,103,122,122,114,109,116,120,108,101,111,115,119,105,115,122,114,107,115,99,102,110,106,105,109,121,122,98,121,101,115,118,120,116,115,114,110,98,116,105,110,121,119,104,120,101,106,119,113,111,113,100,112,100,98,103,104,120,97,99,116,117,111,103,115,99,117,113,97,108,104,108,116,114,110,114,100,98,109,116,102,101,110,104,104,117,99,111,99,107,97,99,105,101,102,108,110,117,117,121,114,104,109,101,118,101,97,99,118,109,114,109,111,108,121,103,113,113,122,116,100,107,107,113,104,103,98,107,122,114,98,105,105,115,113,119,100,98,98,114,115,111,110,112,116,102,118,114,116,102,115,104,101,114,105,105,106,101,113,101,112,100,105,97,121,102,122,100,107,100,98,114,112,115,100,101,109,111,115,112,99,102,114,119,120,119,120,111,101,99,102,103,97,109,103,102,104,106,99,118,102,116,107,118,115,121,111,107,113,112,101,101,113,107,122,112,111,106,100,109,105,121,112,112,113,117,99,100,105,118,98,113,102,109,112,114,102,120,119,101,122,104,110,120,116,98,121,110,101,103,103,114,122,100,110,106,107,117,119,100,105,97,111,99,118,118,122,103,117,107,119,104,117,105,105,109,100,101,103,103,99,99,112,113,118,112,104,116,98,104,113,108,109,120,114,99,115,103,103,117,113,119,108,114,114,98,109,100)
      val bs = Array[Long](0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,30,78,114,152,186,223,269,315,360,390,432,458,505,548,592,631,677,723,762,810,848,892,926,955,990,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024,1024)
      
      val occ  = bm.calcOcc(t1v)
      val bs2 = bm.calcBs (occ)
      assert(bs2.sameElements(bs))
      val gtEof=BitSet(0,1,2,3,4,5,6,10,13,18,20,21,23,24,25,26,30,31,34,38,39,41,42,44,52,53,54,56,60,61,62,65,68,69,71,72,73,74,76,77,79,81,83,89,91,92,93,94,95,96,100,101,102,103,104,106,108,109,110,111,112,113,114,115,117,118,119,120,121,122,123,126,127,128,130,131,134,135,136,137,139,140,143,146,148,149,151,155,156,157,158,161,163,164,166,167,168,179,182,183,184,185,186,187,188,190,191,192,193,195,196,197,200,201,204,208,210,213,214,216,217,218,219,221,223,225,226,228,229,234,235,236,237,238,239,242,243,244,245,246,247,250,251,253,254,257,258,260,262,263,266,267,269,270,271,272,273,274,278,279,280,284,285,287,292,300,302,303,304,306,307,309,310,312,313,314,316,319,320,325,327,328,329,330,334,337,338,339,340,341,342,343,344,346,347,348,349,350,351,354,355,356,357,359,360,361,363,368,369,370,371,374,375,377,378,380,382,383,387,389,391,395,396,397,400,401,402,404,406,407,408,409,410,411,412,413,416,418,421,423,424,425,426,427,428,429,430,431,433,434,437,438,439,442,445,446,448,449,451,452,453,454,455,456,462,463,464,465,468,469,470,471,474,475,476,479,480,482,483,485,486,487,489,490,491,497,499,501,503,504,505,506,507,509,510,511,512,513,514,515,517,522,523,524,525,527,528,529,530,531,532,534,536,537,539,541,544,548,552,553,554,556,557,559,562,564,566,567,568,569,570,571,575,576,583,584,585,587,588,590,593,594,595,596,597,598,599,600,601,604,605,609,610,611,612,613,615,616,617,619,620,621,622,623,624,625,626,627,630,631,632,633,634,636,643,644,646,649,651,654,658,661,662,666,667,668,671,673,675,678,680,681,682,683,684,685,686,687,688,689,690,691,693,694,695,698,700,705,706,707,708,712,714,715,718,719,720,721,722,723,726,727,728,730,731,732,734,737,740,741,742,744,746,747,748,749,750,751,752,754,756,757,758,760,763,764,765,766,768,773,776,777,778,780,782,783,787,788,789,790,791,794,795,798,801,803,811,812,813,814,815,816,818,820,824,825,826,827,828,829,830,832,833,834,835,839,844,845,849,850,851,855,856,857,858,859,860,862,863,864,866,869,874,876,880,882,887,888,889,892,893,894,895,898,899,900,901,902,903,909,915,917,919,920,921,922,924,925,928,930,931,932,935,937,938,939,940,941,945,947,949,950,951,953,954,956,958,959,960,962,963,967,968,970,973,974,978,980,981,982,984,986,988,991,998,999,1000,1001,1003,1006,1007,1008,1009,1010,1012,1015,1016,1017,1018,1019,1020,1022)
      val (bwt,searcher,rankFirst,rankLast) = bm.calcSAStatistic(t1v,bs,gtEof)

      val s_d = searcher.cf('d')
      val e_d = searcher.cf('e')

      //println(bwt.toList.slice(0,1007).count(_.toChar=='j'))
      //println(searcher.occtable.slice(s_d,e_d).mkString(","))
      
      assert(searcher.occ('j',130) == 3 ) 
      assert(searcher.occ('j',131) == 4 ) 
      assert(searcher.occ('j',132) == 5 ) 
      assert(searcher.occ('j',133) == 5 ) 
      assert(searcher.occ('j',600) == 16 ) 

      assert(searcher.occ('j',954) == 29 ) 
      assert(searcher.occ('j',968) == 30 ) 
      assert(searcher.occ('j',1007) == 30 ) 
      
      assert(searcher.occ('d',30) == 0 ) 
      assert(searcher.occ('d',31) == 0 )
      assert(searcher.occ('d',32) == 1 )
      assert(searcher.occ('d',998) == 36 )
      assert(searcher.occ('d',999) == 37 )
      assert(searcher.occ('d',1000) == 37 ,"searcher.occ('d',1000) != 37 but %d".format(searcher.occ('d',1000)))

      assert(searcher.occ('d',1004) == 37,"searcher.occ('d',1004) != 37 but %d".format(searcher.occ('d',1004)))
      assert(searcher.occ('q',730) == 31,"searcher.occ('q',730) != 31 but %d".format(searcher.occ('q',730)))
      assert(searcher.occ('t',661) == 25)
      assert(searcher.occ('e',411) == 13)
      
      assert(searcher.occ(0xff.toByte,411) == 0)
    }
    test("BWTMerger2 test2048 occ 0xff searcher") {
      val r = new FileBWTReader("testdata/test2048.txt")
      val bm = new BWTMerger2(1024)      
      val t1v = Array[Byte](110,121,122,-1,114,122,111,105,97,98,113,103,97,110,106,104,107,104,112,97,119,120,104,121,111,111,117,107,105,101,109,117,101,107,117,106,107,105,116,119,97,112,109,100,116,103,103,107,107,106,97,99,116,115,114,102,113,98,102,106,114,109,118,102,103,122,102,97,120,114,98,113,110,116,120,104,121,113,100,122,102,111,105,118,102,104,101,108,100,118,97,110,118,117,122,112,111,102,103,98,113,116,121,114,113,98,118,97,111,117,110,121,117,110,113,110,104,119,114,118,118,108,122,113,100,103,116,114,112,101,119,118,98,97,113,116,115,109,102,119,122,105,105,122,101,98,111,103,109,109,101,111,105,108,102,115,117,117,112,103,105,115,98,110,113,106,114,118,122,101,97,107,103,101,105,104,97,102,104,110,105,107,115,120,111,119,111,114,116,106,117,108,109,119,106,120,120,115,103,101,118,116,104,101,116,98,107,98,118,104,114,104,98,113,121,104,116,115,117,110,104,120,107,111,106,118,118,98,109,115,98,108,105,105,121,111,113,118,115,119,97,100,115,110,109,113,120,112,103,98,108,118,102,116,115,103,99,116,113,98,122,99,109,122,97,102,116,118,106,116,111,114,108,119,111,101,99,103,118,110,109,103,107,104,112,114,105,115,106,102,104,106,113,103,104,107,103,99,98,103,118,107,109,111,120,104,109,109,97,121,109,102,122,115,121,97,118,108,102,116,115,106,107,100,104,113,100,113,112,117,112,104,98,107,116,106,104,111,108,121,115,117,113,120,113,98,117,119,110,118,112,122,99,98,120,111,114,117,100,110,119,116,101,118,99,98,106,100,109,111,117,116,98,100,114,117,97,109,114,99,114,107,109,121,99,108,99,119,103,120,105,109,107,100,102,111,110,114,103,98,115,114,110,106,110,100,116,122,111,110,112,113,112,115,97,105,111,106,120,99,107,114,106,121,109,111,109,109,112,117,120,111,100,118,109,97,107,111,120,116,103,107,119,106,99,120,120,104,115,115,98,109,121,113,116,118,110,100,107,103,105,98,110,117,116,111,104,99,108,114,110,113,97,105,113,121,112,102,102,109,113,100,110,111,102,117,119,118,103,116,116,121,107,99,103,102,98,121,104,115,105,111,101,121,109,112,109,116,104,116,121,110,109,121,115,122,107,121,97,108,105,102,117,115,116,121,99,114,113,110,121,122,111,103,110,107,115,114,104,109,105,118,103,100,118,102,104,99,110,102,106,104,109,112,118,107,122,114,100,110,105,106,109,105,112,107,114,116,122,122,122,116,100,99,103,108,117,101,103,103,108,98,106,119,113,121,105,112,110,104,120,99,100,118,121,119,112,117,112,112,122,116,99,103,113,119,105,108,105,122,121,112,116,116,104,110,117,111,105,108,114,119,109,112,118,115,108,110,98,104,119,112,110,108,118,98,110,105,105,99,107,104,106,118,111,100,118,102,98,122,106,113,107,103,115,98,105,97,119,105,98,117,117,105,97,107,119,109,115,98,100,119,102,113,103,114,101,100,116,101,118,117,117,112,114,116,120,111,120,119,117,111,98,109,108,116,107,100,119,98,113,103,98,97,101,112,110,114,121,104,104,106,118,107,113,111,99,103,122,122,114,109,116,120,108,101,111,115,119,105,115,122,114,107,115,99,102,110,106,105,109,121,122,98,121,101,115,118,120,116,115,114,110,98,116,105,110,121,119,104,120,101,106,119,113,111,113,100,112,100,98,103,104,120,97,99,116,117,111,103,115,99,117,113,97,108,104,108,116,114,110,114,100,98,109,116,102,101,110,104,104,117,99,111,99,107,97,99,105,101,102,108,110,117,117,121,114,104,109,101,118,101,97,99,118,109,114,109,111,108,121,103,113,113,122,116,100,107,107,113,104,103,98,107,122,114,98,105,105,115,113,119,100,98,98,114,115,111,110,112,116,102,118,114,116,102,115,104,101,114,105,105,106,101,113,101,112,100,105,97,121,102,122,100,107,100,98,114,112,115,100,101,109,111,115,112,99,102,114,119,120,119,120,111,101,99,102,103,97,109,103,102,104,106,99,118,102,116,107,118,115,121,111,107,113,112,101,101,113,107,122,112,111,106,100,109,105,121,112,112,113,117,99,100,105,118,98,113,102,109,112,114,102,120,119,101,122,104,110,120,116,98,121,110,101,103,103,114,122,100,110,106,107,117,119,100,105,97,111,99,118,118,122,103,117,107,119,104,117,105,105,109,100,101,103,103,99,99,112,113,118,112,104,116,98,104,113,108,109,120,114,99,115,103,103,117,113,119,108,114,114,98,109,100)
      val occ  = bm.calcOcc(t1v)
      val bs = bm.calcBs (occ)
      
      val gtEof=BitSet(0,1,2,3,4,5,6,10,13,18,20,21,23,24,25,26,30,31,34,38,39,41,42,44,52,53,54,56,60,61,62,65,68,69,71,72,73,74,76,77,79,81,83,89,91,92,93,94,95,96,100,101,102,103,104,106,108,109,110,111,112,113,114,115,117,118,119,120,121,122,123,126,127,128,130,131,134,135,136,137,139,140,143,146,148,149,151,155,156,157,158,161,163,164,166,167,168,179,182,183,184,185,186,187,188,190,191,192,193,195,196,197,200,201,204,208,210,213,214,216,217,218,219,221,223,225,226,228,229,234,235,236,237,238,239,242,243,244,245,246,247,250,251,253,254,257,258,260,262,263,266,267,269,270,271,272,273,274,278,279,280,284,285,287,292,300,302,303,304,306,307,309,310,312,313,314,316,319,320,325,327,328,329,330,334,337,338,339,340,341,342,343,344,346,347,348,349,350,351,354,355,356,357,359,360,361,363,368,369,370,371,374,375,377,378,380,382,383,387,389,391,395,396,397,400,401,402,404,406,407,408,409,410,411,412,413,416,418,421,423,424,425,426,427,428,429,430,431,433,434,437,438,439,442,445,446,448,449,451,452,453,454,455,456,462,463,464,465,468,469,470,471,474,475,476,479,480,482,483,485,486,487,489,490,491,497,499,501,503,504,505,506,507,509,510,511,512,513,514,515,517,522,523,524,525,527,528,529,530,531,532,534,536,537,539,541,544,548,552,553,554,556,557,559,562,564,566,567,568,569,570,571,575,576,583,584,585,587,588,590,593,594,595,596,597,598,599,600,601,604,605,609,610,611,612,613,615,616,617,619,620,621,622,623,624,625,626,627,630,631,632,633,634,636,643,644,646,649,651,654,658,661,662,666,667,668,671,673,675,678,680,681,682,683,684,685,686,687,688,689,690,691,693,694,695,698,700,705,706,707,708,712,714,715,718,719,720,721,722,723,726,727,728,730,731,732,734,737,740,741,742,744,746,747,748,749,750,751,752,754,756,757,758,760,763,764,765,766,768,773,776,777,778,780,782,783,787,788,789,790,791,794,795,798,801,803,811,812,813,814,815,816,818,820,824,825,826,827,828,829,830,832,833,834,835,839,844,845,849,850,851,855,856,857,858,859,860,862,863,864,866,869,874,876,880,882,887,888,889,892,893,894,895,898,899,900,901,902,903,909,915,917,919,920,921,922,924,925,928,930,931,932,935,937,938,939,940,941,945,947,949,950,951,953,954,956,958,959,960,962,963,967,968,970,973,974,978,980,981,982,984,986,988,991,998,999,1000,1001,1003,1006,1007,1008,1009,1010,1012,1015,1016,1017,1018,1019,1020,1022)
      val (bwt,searcher,rankFirst,rankLast) = bm.calcSAStatistic(t1v,bs,gtEof)

      val directIdx = bwt.indexOf(0xff.toByte)
      assert(directIdx==721)

      assert(searcher.occ(0xff.toByte,directIdx)==1 )
      assert(searcher.occ(0xff.toByte,directIdx-1)==0 )
      assert(searcher.occ(0xff.toByte,directIdx+1)==1 )
    }

    test("BWTMerger test2048.txt") {
        
        
        val r = new FileBWTReader("testdata/test2048.txt")
        val bm = new BWTMerger2(1024)
        val (of,af) = bm.merge(r)
        
        
        val bl = new BWTLoader(of)
        val tbl = new BWTLoader(new File("testdata/test2048.cmp.bwt"),false)
        assert(bl == tbl)
        
        
        val al = new AUXLoader(af)
        val tal = new AUXLoader(new File("testdata/test2048.cmp.aux"),false)
        assert(al == tal)
        
    }
    
    test("BWTMerger test3072.txt") {
        val r = new FileBWTReader("testdata/test3072.txt")
        val bm = new BWTMerger2(1024)
        val (of,af) = bm.merge(r)
        
        val bl = new BWTLoader(of)
        val tbl = new BWTLoader(new File("testdata/test3072.cmp.bwt"),false)
        assert(bl == tbl)
        
        val al = new AUXLoader(af)
        val tal = new AUXLoader(new File("testdata/test3072.cmp.aux"),false)
        assert(al == tal)
    }

    test("BWTMerger test.txt") {
      val r = new FileBWTReader("testdata/test.txt")
      val bm = new BWTMerger2(1024)
      val (of,af) = bm.merge(r)
      
      val bl = new BWTLoader(of)
      val tbl = new BWTLoader(new File("testdata/test.cmp.bwt"),false)
      assert(bl == tbl)
      
      val al = new AUXLoader(af)
      val tal = new AUXLoader(new File("testdata/test.cmp.aux"),false)
      assert(al == tal)      
    }

    test("BWTMerger test-part.txt") {
      val r = new FileBWTReader("testdata/test-part.txt")
      val bm = new BWTMerger2(1024)
      val (of,af) = bm.merge(r)
      
      val bl = new BWTLoader(of)
      val tbl = new BWTLoader(new File("testdata/test-part.cmp.bwt"),false)
      assert(bl == tbl)
      
      val al = new AUXLoader(af)
      val tal = new AUXLoader(new File("testdata/test-part.cmp.aux"),false)
      assert(al == tal)      
      
    }

    test("BWTMerger testdata/2048-0.txt second segment should have sa[0] = 0") {
      //val r = new DirBWTReader("testdata/t1","testdata/t1",debugLevel=1)
      val r = new FileBWTReader("testdata/test2048-2.txt")
      val bm = new BWTMerger2(1024,debugLevel=0)
      val (of,af) = bm.merge(r)

      val bl = new BWTLoader(of)
      val tbl = new BWTLoader(new File("testdata/test2048-2.cmp.bwt"),false)
      assert(bl == tbl)
        
      val al = new AUXLoader(af)
      val tal = new AUXLoader(new File("testdata/test2048-2.cmp.aux"),false)
      assert(al == tal)      

    }

}

class DirBWTReaderTest extends FunSuite {
  test("DirBWTReader testdata/") {
    val r = new DirBWTReader("testdata/t1")
    var c = r.getByte
    assert(c==117)
    c = r.getByte
    assert(c==101)
    val t=new Array[Byte](1024*10)
    val n = r.copyReverse(t)
    assert(n==3070)   
  }
}

class UtilTest extends FunSuite {
  test("isBinary") {
    assert(! Util.isBinary(new File("testdata/at.h")).getOrElse(true))
  }
}

class MergerTest2 extends FunSuite {
   class BWTLoader(f:File,bigEndian:Boolean=true) {
    val in = new java.io.FileInputStream(f)
    val inb = new java.io.DataInputStream(in)
    val size = if (bigEndian) inb.readLong() else java.lang.Long.reverseBytes(inb.readLong())
    val eof =  if (bigEndian) inb.readLong() else java.lang.Long.reverseBytes(inb.readLong())
    val b   = new Array[Byte](size.toInt)
    val rdn = inb.read(b)
    //assert(rdn==size)
    inb.close
    in.close
    def == (that: BWTLoader): Boolean = size == that.size && eof == that.eof && b.sameElements(that.b)
  }
  class AUXLoader(f:File,bigEndian:Boolean=true) {
    val in = new java.io.FileInputStream(f)
    val inb = new java.io.DataInputStream(in)
    val occ   = new Array[Long](BWTMerger2.ALPHA_SIZE)
    
    for (i<-0 until occ.length) {
      occ(i) = if ( ! bigEndian ) java.lang.Long.reverseBytes(inb.readLong()) else inb.readLong()
    }

    inb.close
    in.close
    def == (that: AUXLoader): Boolean = occ.sameElements(that.occ)
  }
  
  test("KMPBuffer") {
    val t = new Array[Byte](1024)
    val kmpIn = KMPBuffer.init(t)
    def writeBit(v:Boolean,i:Int) = {
      if(v){
        kmpIn.pending1 = i
      } else {
        kmpIn.pending0 = i
      }
      kmpIn.writeBit(v)
    }
    writeBit(true,3)
    //[info] 00000007,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    //[info] !3 bit(s) stored in bit buffer!
    writeBit(true,3)
    //[info] 00000007,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    //[info] BWTMerger2.merge: start step 7 lastStep=23 sec readed: 70.0 Mb speed=431.4 k/sec Avgspeed=0.7 M/sec saisLastIteration = 4 diskread = 0 diskreadTotal = 1 calcGaps = 14 calcGapsTotal =47
    //[info] KMPBuffer.rewind 3 1 0
    writeBit(false,1)
    //[info] 00000007,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    //[info] !4 bit(s) stored in bit buffer!
    writeBit(true,2)
    //[info] 00000003,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    //[info] BWTMerger2.merge: start step 8 lastStep=27 sec readed: 80.0 Mb speed=368.1 k/sec Avgspeed=0.6 M/sec saisLastIteration = 4 diskread = 0 diskreadTotal = 2 calcGaps = 18 calcGapsTotal =65
    //[info] KMPBuffer.rewind 2 5 0
    writeBit(false,5)
    //[info] 00000003,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    //[info] !7 bit(s) stored in bit buffer!
    writeBit(false,22)
    //[info] 001c0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(true,1)
    //[info] 003c0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(false,1)
    //[info] 003c0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(true,1)
    //[info] 00bc0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(false,1)
    //[info] 00bc0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(true,2)
    //[info] 06bc0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(false,6)
    //[info] 06bc0000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,
    writeBit(true,1)
    //[info] 06bc0002,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,00000000,

  }
  
  

  /*
  test("justi") {
    val r = new DirBWTReader("/usr/include","testdata/include",debugLevel=1)
    val out = new java.io.FileOutputStream("/tmp/t.txt")

    for ( i <- 0 until 134 * 1024) {
      val c = r.getByte
      out.write(c)
    }
  }
  */
  

}