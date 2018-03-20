package com.ora.scalabeyondbasics

import org.scalatest.{FunSpec, Matchers}

import scala.reflect.ClassTag

class AdvancedImplicitsSpec extends FunSpec with Matchers {

  describe(
    """Implicits is like a Map[Class[A], A] where A is any object and it is tied into the scope,
      | and it is there when you need it, hence it is implicit. This provide a lot of great techniques that we
      | can use in Scala.""".stripMargin
  ) {

    it(
      """is done per scope so in the following example, we will begin with an implicit value
        |  and call it from inside a method which uses a multiple parameter list where one
        |  one group would """.stripMargin
    ) {

      implicit val rate: Int = 100

      def calculatePayment(hours: Int)(implicit rate: Int) = hours * rate

      calculatePayment(50) should be(5000)
    }

    it(
      """will allow you to place something manually, if you want to override the implicit value""".stripMargin
    ) {

      implicit val rate: Int = 100

      def calculatePayment(hours: Int)(implicit rate: Int) = hours * rate

      calculatePayment(50)(10) should be(500)
    }

    it(
      """will gripe at compile time if there are two implicit bindings of the same type.  It's
        |  worth noting that what Scala doing are compile time tricks for implicit. One strategy is to
        |  wrap a value in a type to avoid conflict""".stripMargin
    ) {

      case class Rate(value: Int)
      case class Age(value: Int)

      //implicit val rate: Int = 100
      //implicit val age: Int = 20

      implicit val rate = Rate(100)
      implicit val age = Age(20)

      def calculatePayment(hours: Int)(implicit rate: Rate) = hours * rate.value

      calculatePayment(50) should be(5000)
    }

    it(
      """is really used to bind services that require something and
        |  you don't particularly need to inject everywhere explicitly, in this
        |  case let's discuss Future[+T]""".stripMargin
    ) {

      import scala.concurrent._
      import scala.concurrent.duration._
      import scala.util.{Failure, Success}
      import java.util.concurrent.{ExecutorService, Executors}

      val executorService: ExecutorService = Executors.newCachedThreadPool
      implicit val executionContext: ExecutionContext =
        ExecutionContext.fromExecutor(executorService)

      val future: Future[Int] = Future {

        Thread.sleep(4000)

        100 * 4
      }

      future.onComplete {

        case Success(value) => println(s"Answer is $value")
        case Failure(err) =>
          println(s"Something bad happened => ${err.getMessage}")
      }

      Await.ready(future, 5.seconds)
    }

    it("""can bring up any implicit directly by merely calling up implicitly""") {

      case class IceCream(name: String)
      case class Scoops(n: Int, flavor: IceCream)

      implicit val flavorOfTheDay: IceCream = IceCream("Spring Green")

      def orderIceCream(nScoops: Int)(implicit flavorOfTheDay: IceCream) = {

        Scoops(nScoops, flavorOfTheDay)
      }

      orderIceCream(3)(IceCream("Rocky Road"))
      orderIceCream(3)

      def orderIceCream2(nScoops: Int) = Scoops(nScoops, implicitly[IceCream])
      def orderIceCream3(nScoops: Int,
                         flavor: IceCream = implicitly[IceCream]) =
        Scoops(nScoops, flavor)

      val iceCream = orderIceCream3(5)

      println(iceCream)
    }

    it(
      """the implicit group parameter list, can contain more than one parameter, but
        |  needs to be in the same implicit parameter group""".stripMargin
    ) {

      implicit val bonus = 5000
      implicit val currency = "Euro"

      def calcYearRate(amount: Int)(implicit bonusAmt: Int,
                                    preferredCurrency: String) =
        amount + bonusAmt + " " + preferredCurrency

      calcYearRate(60000) should be("65000 Euro")
    }

    it("""can also be replaced with default parameters, choose accordingly""") {

      def calcYearRate(amount: Int,
                       bonusAmt: Int = 5000,
                       preferredCurrency: String = "Euro") =
        amount + bonusAmt + " " + preferredCurrency

      calcYearRate(60000) should be("65000 Euro")
    }

    it(
      """Christopher A. Question: if you have a List[String] implicitly will it try
        | to inject into a List[Double]?""".stripMargin
    ) {

      implicit val listOfString = List("Foo", "Bar", "Baz")
      implicit val listOfDouble = List(1.0, 2.0, 3.0)

      val result = implicitly[List[Double]]

      result should be(listOfDouble)
    }

    it(
      """can be used for something like what Ruby has called
        |  monkey patching or Groovy calls mopping where we can add functionality to
        |  a class that we don't have access to, like isOdd/isEven
        |  in the Int class.  This is what we call implicit wrappers.
        |  First we will use a conversion method.""".stripMargin
    ) {

      class IntWrapper(x: Int) {

        def isOdd: Boolean = x % 2 != 0
        def isEven: Boolean = !isOdd
      }

      import scala.language.implicitConversions

      implicit def intToIntWrapper(x: Int) = new IntWrapper(x)

      5.isOdd should be(true)
      5.isEven should be(false)

      6.isOdd should be(false)
      6.isEven should be(true)
    }

    it(
      """Implicit wrappers can be created using a function and is often easier to mental map.""".stripMargin
    ) {

      class IntWrapper(x: Int) {

        def isOdd: Boolean = x % 2 != 0
        def isEven: Boolean = !isOdd
      }

      implicit val intToIntWrapper = (x: Int) => new IntWrapper(x)

      5.isOdd should be(true)
      5.isEven should be(false)

      6.isOdd should be(false)
      6.isEven should be(true)
    }

    it(
      """can be use a short hand version of this called implicit classes, before using them
        |  there are some rules:
        |  1. They can only be used inside of an object/trait/class
        |  2. They can only take one parameter in the constructor
        |  3. There can not be any colliding method name as that
        |     with the implicit outer scope""".stripMargin
    ) {

      import scala.language.implicitConversions

      implicit class IntWrapper[T: Numeric](x: T) {

        def isOdd: Boolean = implicitly[Numeric[T]].toDouble(x) % 2 != 0
        def isEven: Boolean = !isOdd
      }

      5.isOdd should be(true)
      5.isEven should be(false)

      6.isOdd should be(false)
      6.isEven should be(true)
    }

    it(
      """can also convert things to make it fit into a particular API,
        | this is called implicit conversion,
        | in this scenario we will use a method""".stripMargin
    ) {

      import scala.language.implicitConversions

      sealed abstract class Currency
      case class Dollar(value: Int) extends Currency
      case class Yen(value: Int) extends Currency

      implicit def intToDollar(i: Int): Dollar = Dollar(i)

      def addAmount(x: Dollar, y: Dollar): Dollar = Dollar(x.value + y.value)

      addAmount(Dollar(40), Dollar(100)) should be(Dollar(140))

      addAmount(50, 150) should be(Dollar(200))
    }

    it("""can also convert things to make it fit into a particular API,
        | this is called implicit conversion,
        | in this scenario we will use a function""".stripMargin) {
      pending
    }

    it(
      """is done automatically in Scala because what is inside of scala.Predef, for example,
        |  it explains how be can set a scala.Float , and there is java.lang.Float,
        |  java primitive float.
        |  We can investigate this by looking at
        |  the documentation.""".stripMargin
    ) {

      val f1: scala.Float = 4001.00f
      val f2: scala.Float = 5030.40f

      val result = java.lang.Math.min(f1, f2)

      result should be(f1)
    }
  }

  describe("Locating implicits recipes") {

    it(
      """has a common way, to store that particular implicit
        |  recipe in an object that makes should make
        |  sense and then import that object""".stripMargin
    ) {

      object MyPredef {

        import scala.language.implicitConversions

        implicit class IntWrapper(x: Int) {

          def isOdd: Boolean = x % 2 != 0
          def isEven: Boolean = !isOdd
        }
      }

      import MyPredef._

      29.isOdd should be(true)
    }

    it(
      """can also use a companion object to store any implicit recipes""".stripMargin
    ) {

      class Artist(val firstName: String, val lastName: String)
      object Artist {

        import scala.language.implicitConversions

        implicit def tupleToArtist(t: (String, String)): Artist =
          new Artist(t._1, t._2)

      }

      def playPerformer(a: Artist) = s"Playing now ${a.firstName} ${a.lastName}"

      playPerformer(("Elvis", "Presley")) should be(
        s"Playing now Elvis Presley")
    }

    it("""can also use a package object to store some of these implicits""") {

      def numItems(list: List[String]) = list.mkString(", ")

      numItems(3 -> "Whoa") should be("Whoa, Whoa, Whoa")
    }

    it(
      """can use JavaConverters to convert a collection in Java to Scala and vice versa""") {
      pending
    }
  }

  describe(
    "View Bounds are used to ensure that there is a particular recipe for a certain type"
  ) {

    it(
      """Uses <% inside of a parameterized type declaration to determine if there is a conversion available
        | then within you can treat an object as an object of that type. It is unorthodox, and has since been
        | deprecated.""".stripMargin
    ) {

      import scala.language.implicitConversions

      class Employee(val firstName: String, val lastName: String)

      implicit def strToEmployee(s: String): Employee = {

        val first :: last :: _ = s.split(" ").toList
        new Employee(first, last)
      }

      def hireEmployee[A <% Employee](a: A) = {

        s"Hired an employee ${a.firstName} ${a.lastName}"
      }

      hireEmployee("Joe Employee") should be("Hired an employee Joe Employee")
    }
  }

  describe(
    """Context Bounds works so that there is a type A, and it requires a B[A] somewhere
      |  within the the implicit scope, for example like Ordered[T], or TypeTag[T], or Numeric[T],
      |  this provides a way to check that something is something can be implicitly defined but
      |  gives the end user no opportunity to the ability to inject a different implementation""".stripMargin
  ) {

    it(
      """uses the signature [T:WrappedType], which is
        | equivalent to (t:T)(implicit w:WrappedType[T])
        | let's try it with """.stripMargin
    ) {

      trait Loggable[T] {

        def log(t: T): String
      }

      class Employee(val firstName: String, val lastName: String)

      implicit val loggableEmployee = new Loggable[Employee] {

        override def log(t: Employee): String =
          s"Employee: ${t.firstName} ${t.lastName}"
      }

      def writeToLog[T: Loggable](t: T): String = {

        val loggable = implicitly[Loggable[T]]
        loggable.log(t)
      }

      writeToLog(new Employee("Roy", "Rogers")) should be(
        "Employee: Roy Rogers")
    }
  }

  describe(
    """Type Constraints are used to ensure that a particular method can run
      | if a particular generic is of a certain type, this is typically used for
      | one method""".stripMargin
  ) {

    it(
      """uses one operator, =:= which is actually the full type =:=[A,B] that
        |  will to see if something is of the same type""".stripMargin
    ) {

      class MyPair[A, B](val a: A, val b: B) {

        def first: A = a
        def second: B = b

        def toList(implicit ev: A =:= B): List[A] =
          List(a, b).asInstanceOf[List[A]]
      }

      val myPairHomogenous = new MyPair(4, 10)

      myPairHomogenous.toList should be(List(4, 10))

      val myPairHeterogenous = new MyPair(4, "Foo")

      // I do not want the following to work
      // myPairHeterogenous.toList
    }

    it("""uses the operator, <:< which will test if A is a subtype of B""") {

      List(1 -> "one", 2 -> "two").toMap should be(Map(1 -> "one", 2 -> "two"))

      // List( 1, 2, 3, 4 ).toMap // won't compile
    }
  }

  describe("Getting around Erasure Using TypeTags") {

    it(
      "used to use Manifest but now uses a type tag to retrieve what is erased"
    ) {

      import scala.reflect.runtime.universe._

      def matchList[A](list: List[A])(implicit tag: TypeTag[A]): String = {

        tag.tpe match {

          case x if x =:= typeOf[String] => "List of String"
          case y if y =:= typeOf[Int]    => "List of Int"
          case _                         => "List of something else"
        }
      }

      matchList(List(1, 2, 3, 4)) should be("List of Int")
    }
  }

  describe(
    """Typeclasses are a way of generating or extending behavior using Java-like interfaces,
      |  but operate as outside.  There is another term for this,
      |  and it's called ad-hoc polymorphism""".stripMargin
  ) {

    it(
      """can be used to determine equality, so whether than make equals inside of an class,
        | it is now an outside concern""".stripMargin
    ) {

      trait Eq[T] {
        def myEquals(a: T, b: T): Boolean
      }

      implicit val eqTypeClass: Eq[Team] = new Eq[Team] {

        override def myEquals(a: Team, b: Team): Boolean =
          a.city == b.city && a.manager == b.manager && a.mascot == b.mascot
      }

      def isEquals[A](a: A, b: A)(implicit eq: Eq[A]) = eq.myEquals(a, b)

      /* Alternate method
        def isEquals[A : Eq]( a : A, b : B ) =
          implicitly[Eq].myEquals( a, b )
       */

      class Team(val mascot: String, val city: String, val manager: String)

      val a1 = new Team("Blue Jays", "Toronto", "Bobby")
      val a2 = new Team("Blue Jays", "Toronto", "Bobby")
      val a3 = new Team("Nationals", "Washington", "Carla")

      isEquals(a1, a2) should be(true)
    }

    it("can be used for ordering") {

      case class Employee(firstName: String, lastName: String)

      object Employee {
        implicit val orderEmployeesByFirstName = new Ordering[Employee] {
          override def compare(x: Employee, y: Employee): Int =
            x.firstName.compareTo(y.firstName)
        }

        implicit val orderEmployeesByLastName = new Ordering[Employee] {
          override def compare(x: Employee, y: Employee): Int =
            x.lastName.compareTo(y.lastName)
        }
      }

      val list: List[Employee] = List(
        Employee("Samuel", "Jackson"),
        Employee("Janice", "Joplin"),
        Employee("Jimmy", "Page"),
        Employee("The", "Edge"),
        Employee("Scarlet", "Johansson"),
        Employee("Justin", "Bieber")
      )

      import Employee.orderEmployeesByLastName

      list.sorted.head should be(Employee("Justin", "Bieber"))
    }
  }
}
