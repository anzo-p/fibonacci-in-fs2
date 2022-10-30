package app

case class Fibonacci(lowInteger: BigInt, highInteger: BigInt)

object Fibonacci {
  import cats.data.Validated._
  import cats.data.ValidatedNec
  import cats.implicits._

  type ValidationResult[A] = ValidatedNec[String, A]

  def validatePositive(value: BigInt): ValidationResult[BigInt] =
    if (value >= 0) value.validNec else "value cannot be negative".invalidNec

  def validateNotEqual(a: BigInt, b: BigInt): ValidationResult[BigInt] =
    if (a != b || a == 1) a.validNec else "values cannot be equal".invalidNec

  def validateFibonacci(lowInteger: BigInt, highInteger: BigInt): ValidationResult[Fibonacci] = {
    val lo = validatePositive(lowInteger)
    val hi = validatePositive(highInteger).combine(validateNotEqual(highInteger, lowInteger)).as(highInteger)

    (lo, hi).mapN(Fibonacci.apply)
  }
}
