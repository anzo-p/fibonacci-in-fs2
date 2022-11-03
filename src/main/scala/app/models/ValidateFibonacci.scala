package app.models

import app.kafka.serdes.ValidationError
import cats.data.ValidatedNec
import cats.implicits._

object ValidateFibonacci {

  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  trait Positive[A] extends (A => Boolean)

  implicit val positiveLong: Positive[Long] = _ >= 0

  implicit val positiveBigint: Positive[BigInt] = _ >= 0

  def validatePositive[A](value: A)(implicit positive: Positive[A]): ValidationResult[A] =
    if (positive(value)) value.validNec else ValidationError(s"value $value cannot be negative").invalidNec

  def validateNotEqual(a: BigInt, b: BigInt): ValidationResult[BigInt] =
    if (a != b || a == 1) a.validNec
    else ValidationError(s"lowInteger $a and highInteger $b cannot be equal (unless when 1 and 1)").invalidNec

  def validate(round: Long, lowInteger: BigInt, highInteger: BigInt): ValidationResult[Fibonacci] = {
    val n  = validatePositive(round)
    val lo = validatePositive(lowInteger)
    val hi = validatePositive(highInteger).combine(validateNotEqual(highInteger, lowInteger)).as(highInteger)

    (n, lo, hi).mapN(Fibonacci.apply)
  }
}
