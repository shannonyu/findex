package org.fmindex.tests

import org.scalatest.FunSuite
import org.fmindex._
import scalax.file.Path
import scalax.io._
/*
class RE2Parser  extends FunSuite with RandomGenerator {
  import org.fmindex.re2._
  test("re2post") {
    val r = REParser.re2post("abc")
    assert(r=="ab.c.","r="+r)
  }
  test("re2post2") {
    val r = REParser.re2post("a(bb)+a")
    assert(r=="abb.+.a.","r="+r)
  }
  test("re2post3") {
    val r = REParser.re2post("(a|b)")
    assert(r=="ab|","r="+r)
  }
  test("re2post4") {
    val r = REParser.re2post("((a|b)*aba*)*(a|b)(a|b)")
    
    assert(r=="ab|*a.b.a*.*ab|.ab|.","r="+r)
  }
  test("createNFA") {
    var nfa = REParser.createNFA("ab.c.")
    nfa match {
      case REParser.ConstState('a',out) => 
        nfa = out.s
      case _ => assert(false)
    }
    nfa match {
      case REParser.ConstState('b',out) => 
        nfa = out.s
      case _ => assert(false)
    }
    nfa match {
      case REParser.ConstState('c',out) => 
        nfa = out.s
      case _ => assert(false)
    }
    nfa match {
      case REParser.MatchState => 
      case _ => assert(false)
    }
  }
  
  test("orNFA") {
    var nfa = REParser.createNFA("ab|c.")

    var (out1,out2) = nfa match {
      case REParser.SplitState(out1,out2) => 
        (out1.s,out2.s)
      case _ => assert(false)
    }
    
    out1 = out1 match {
      case REParser.ConstState('a',out) => 
        out.s
      case _ => assert(false)
    }
    out2 = out2 match {
      case REParser.ConstState('b',out) => 
        out.s
      case _ => assert(false)
    }

    out1 = out1 match {
      case REParser.ConstState('c',out) => 
        out.s
      case _ => assert(false)
    }

    out2 = out2 match {
      case REParser.ConstState('c',out) => 
        out.s
      case _ => assert(false)
    }

    out1 match {
      case REParser.MatchState => 
      case _ => assert(false)
    }
  }
  test("plusNFA") {
    var out1 = REParser.createNFA("a+")


    var out2 = out1 match {
      case REParser.ConstState('a',out) => 
        out.s
      case _ => assert(false)
    }

    var (out3,out4) = out2 match {
      case REParser.SplitState(out1,out2) => 
        (out1.s,out2.s)
      case _ => assert(false)
    }
    
    out1 = out3 match {
      case REParser.ConstState('a',out) => 
        out.s
      case _ => assert(false)
        ???
    }
    
    out4 match {
      case REParser.MatchState => 
      case _ => assert(false)
    }
  }
  
  test("starNFA") {
    var out1 = REParser.createNFA("a*")

    var (out2,out3) = out1 match {
      case REParser.SplitState(out1,out2) => 
        (out1.s,out2.s)
      case _ => assert(false)
    }

    out2 = out2 match {
      case REParser.ConstState('a',out) => 
        out.s
      case _ => assert(false)
        ???
    }

    out3 match {
      case REParser.MatchState => 
      case _ => assert(false)
    }
    
    out2 match {
      case REParser.SplitState(out1,out2) => 
        (out1.s,out2.s)
      case _ => assert(false)
    }

  }

  test("questionNFA") {
    var out1 = REParser.createNFA("a?")

    var (out2,out3) = out1 match {
      case REParser.SplitState(out1,out2) => 
        (out1.s,out2.s)
      case _ => assert(false)
    }

    out2 = out2 match {
      case REParser.ConstState('a',out) => 
        out.s
      case _ => assert(false)
        ???
    }

    out3 match {
      case REParser.MatchState => 
      case _ => assert(false)
    }
    
    out2 match {
      case REParser.MatchState => 
      case _ => assert(false)
    }

  }

  test("matchString1") {
    var re1 = REParser.createNFA("ab.c.")
    assert(REParser.matchNFA(re1,"abc")==true)
    assert(REParser.matchNFA(re1,"atc")==false)
  }

  test("matchString2") {
    var re1 = REParser.createNFA("am|c.")
    assert(REParser.matchNFA(re1,"ac")==true)
    assert(REParser.matchNFA(re1,"mc")==true)
    assert(REParser.matchNFA(re1,"Xc")==false)
    assert(REParser.matchNFA(re1,"c")==false)
  }

  test("matchString3") {
    val re = "(a|m)c"
    assert(REParser.matchString(re,"ac")==true)
    assert(REParser.matchString(re,"mc")==true)
    assert(REParser.matchString(re,"Xc")==false)
    assert(REParser.matchString(re,"c")==false)
  }

  test("matchString4") {
    val re = "a*b?c+"
    assert(REParser.matchString(re,"abc")==true)
    assert(REParser.matchString(re,"bc")==true)
    assert(REParser.matchString(re,"bcc")==true)
    assert(REParser.matchString(re,"aaabc")==true)
  }


  test("match SA basics") {
    val sa = new SAISBuilder(fromString("mmabcacadabbbca".reverse))
    sa.build
    sa.buildOCC
    sa.printSA()
    var re1 = REParser.createNFA("mm.a.")
    val results = ReParser.matchSA(re1,sa)

    /*
    val dfa = DFA.processLinkList(`ab*c`)
    
    assert(results.size == 2)
    assert(results(0).toString=="cbbba")
    assert(results(1).toString=="cba")
    assert(sa.nextSubstr(results(1).sp,results(1).len)=="cba","sa.nextSubstr(results(1).sp,results(1).len)="+sa.nextSubstr(results(1).sp,results(1).len))
    assert(sa.nextSubstr(results(0).sp,results(0).len)=="cbbba")
    */
  }

}
*/