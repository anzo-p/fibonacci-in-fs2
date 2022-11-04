package app.utils

import app.models.Fibonacci
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

object ArbitraryGenerator {

  case class PositiveLong(value: Long)

  case class PositiveBigint(value: BigInt)

  case class OneFibonacci(value: Fibonacci)

  implicit val arbitraryPositiveInteger: Arbitrary[PositiveLong] = Arbitrary {
    Gen.posNum[Long].map(PositiveLong)
  }

  implicit val arbitraryPositiveBigInt: Arbitrary[PositiveBigint] = Arbitrary {
    Gen.posNum[BigInt].map(PositiveBigint)
  }

  implicit val arbitraryFibonacci: Arbitrary[OneFibonacci] =
    Arbitrary {
      for {
        n      <- arbitrary[PositiveLong]
        bigint <- arbitrary[PositiveBigint]
      } yield {
        OneFibonacci(
          new Fibonacci(
            n.value,
            bigint.value,
            bigint.value + bigint.value
          )
        )
      }
    }

  def sample[A : Arbitrary]: A = arbitrary[A].sample.get
}
