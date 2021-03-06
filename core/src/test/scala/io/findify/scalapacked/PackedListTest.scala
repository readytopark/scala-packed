package io.findify.scalapacked

import io.findify.scalapacked.example.{Foo, FooCodec}
import io.findify.scalapacked.immutable.PackedList
import io.findify.scalapacked.immutable.PackedList.PackedSeqCanBuildFrom
import org.scalatest.{FlatSpec, Matchers}


/**
  * Created by shutty on 11/19/16.
  */


class PackedListTest extends FlatSpec with Matchers {
  implicit val encoder = new FooCodec()
  val buf = Range(0, 10).map(r => Foo(r, r.toFloat, r.toString)).to[PackedList](new PackedSeqCanBuildFrom[Foo]())

  "case class with int" should "convert from normal seq" in {
    buf.size shouldBe 10
    buf.head shouldBe Foo(0, 0.0f, "0")
    buf.last shouldBe Foo(9, 9.0f, "9")
    buf.map(_.i1).sum shouldBe 45
    buf.map(_.s).mkString shouldBe "0123456789"
  }

  it should "construct seq from element vararg" in {
    val buf2 = PackedList(Foo(111, 111.0f, "xxx"))
    buf2.head.i1 shouldBe 111
    val buf2a = PackedList(Foo(111, 111.0f, "xxx"), Foo(222, 222.0f, "yyy"))
    buf2a.size shouldBe 2
  }

  it should "append elements to list" in {
    val buf2 = PackedList(Foo(111, 111.0f, "xxx"))
    val buf3 = buf ++ buf2
    buf3.size shouldBe 11
  }

  it should "not mutate the original list" in {
    val a = PackedList(Foo(1,1f,"x"))
    val b = PackedList(Foo(1,1f,"x"))
    val c = a ++ b
    c.size shouldBe 2
    a.size shouldBe 1
  }

  it should "make cbf-based collection transformation" in {
    import PackedList._
    val a = List(Foo(1,1f,"x"))
    val b = a.to[PackedList]
    b.size shouldBe 1
  }

}
