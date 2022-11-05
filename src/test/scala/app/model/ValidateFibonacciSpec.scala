package app.model

import app.kafka.serdes.ValidationError
import app.models.{Fibonacci, ValidateFibonacci}
import app.utils.BaseSpec
import cats.data.Chain
import cats.data.Validated.{Invalid, Valid}

class ValidateFibonacciSpec extends BaseSpec {

  "validate" should {
    "return Valid Fibonacci instance" when {
      "all values are acceptable" in {
        ValidateFibonacci.validate(
          1,
          2,
          3
        ) mustBe Valid(new Fibonacci(1, 2, 3))
      }

      "'low' and 'high integers' equal, though both are 1" in {
        ValidateFibonacci.validate(
          1,
          1,
          1
        ) mustBe Valid(new Fibonacci(1, 1, 1))
      }
    }

    "return Invalid" when {
      "non-positive value in 'round'" in {
        ValidateFibonacci.validate(
          0,
          1,
          2
        ) mustBe Invalid(Chain(ValidationError("value 0 in field: 'round' cannot be negative")))
      }

      "'low integer' is negative" in {
        ValidateFibonacci.validate(
          1,
          -1,
          1
        ) mustBe Invalid(Chain(ValidationError("value -1 in field: 'lowInteger' cannot be negative")))
      }

      "'low' and 'high integers' are both negative, and should also chain the validations" in {
        ValidateFibonacci.validate(
          1,
          -1,
          -2
        ) mustBe Invalid(
          Chain(
            ValidationError("value -1 in field: 'lowInteger' cannot be negative"),
            ValidationError("value -2 in field: 'highInteger' cannot be negative")))
      }

      "equal vales in 'low' and 'high integers', unless both are 1" in {
        ValidateFibonacci.validate(
          1,
          2,
          2
        ) mustBe Invalid(Chain(ValidationError("'lowInteger' 2 and 'highInteger' 2 cannot equal (unless when 1 and 1)")))
      }
    }
  }
}
