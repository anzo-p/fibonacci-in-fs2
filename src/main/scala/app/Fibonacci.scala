package app

case class Fibonacci(lowInteger: BigInt, highInteger: BigInt)

object Fibonacci {
  import cats.data.Validated._
  import cats.data.ValidatedNec
  import cats.implicits._

  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  private def apply = new Fibonacci(_, _)

  def validatePositive(value: BigInt): ValidationResult[BigInt] =
    if (value >= 0) value.validNec else ValidationError(s"value $value cannot be negative").invalidNec

  def validateNotEqual(a: BigInt, b: BigInt): ValidationResult[BigInt] =
    if (a != b || a == 1) a.validNec
    else ValidationError(s"lowInteger $a and highInteger $b cannot be equal (unless when 1 and 1)").invalidNec

  def validateFibonacci(lowInteger: BigInt, highInteger: BigInt): ValidationResult[Fibonacci] = {
    val lo = validatePositive(lowInteger)
    val hi = validatePositive(highInteger).combine(validateNotEqual(highInteger, lowInteger)).as(highInteger)

    (lo, hi).mapN(Fibonacci.apply)
  }

  def create(lowInteger: BigInt, highInteger: BigInt): Either[ValidationError, Fibonacci] = {
    validateFibonacci(lowInteger, highInteger) match {
      case Invalid(e) =>
        Left(ValidationError(e.toChain.toList.mkString(",")))

      case Valid(a) =>
        Right(a)
    }
  }
}
