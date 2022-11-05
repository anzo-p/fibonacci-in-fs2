package app.model

import app.kafka.serdes.ValidationError
import app.models.Fibonacci
import app.utils.BaseSpec

class FibonacciSpec extends BaseSpec {

  "create" should {
    "return Valid Fibonacci when values are acceptable" in {
      Fibonacci.create(1, 2, 3) mustBe Right(new Fibonacci(1, 2, 3))
    }

    "return Invalid when validations are violated" in {
      Fibonacci.create(0, 1, 1) mustBe Left(ValidationError("value 0 in field: 'round' cannot be negative"))
    }
  }

  "inc" should {
    "return Valid Fibonacci with the values properly increased" when {
      "initial values 0 and 1" in {
        Fibonacci.create(1, 0, 1).map(_.inc mustBe new Fibonacci(2, 1, 1))
      }

      "second round values 1 and 1" in {
        Fibonacci.create(2, 1, 1).map(_.inc mustBe new Fibonacci(3, 1, 2))
      }

      "inductively from round 3 onwards" in {
        Fibonacci.create(3, 1, 2).map(_.inc mustBe new Fibonacci(4, 2, 3))
      }
    }
  }
}
