package app.models

import app.kafka.serdes.ValidationError
import cats.data.ValidatedNec
import cats.implicits._

object ValidateFibonacci {

  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  trait Minimum[A] extends (A => Boolean)

  implicit val minimumLong: Minimum[Long] = _ >= 1

  implicit val minimumBigint: Minimum[BigInt] = _ >= 0

  def validatePositive[A](value: A, fieldName: String)(implicit positive: Minimum[A]): ValidationResult[A] =
    if (positive(value)) value.validNec else ValidationError(s"value $value in field: '$fieldName' cannot be negative").invalidNec

  def validateNotEqual(a: BigInt, b: BigInt): ValidationResult[BigInt] =
    if (a != b || a == 1) a.validNec
    else ValidationError(s"'lowInteger' $a and 'highInteger' $b cannot equal (unless when 1 and 1)").invalidNec

  def validate(round: Long, lowInteger: BigInt, highInteger: BigInt): ValidationResult[Fibonacci] = {
    val n  = validatePositive(round, "round")
    val lo = validatePositive(lowInteger, "lowInteger")
    val hi = validatePositive(highInteger, "highInteger").combine(validateNotEqual(highInteger, lowInteger)).as(highInteger)

    (n, lo, hi).mapN(Fibonacci.apply)
  }
}
