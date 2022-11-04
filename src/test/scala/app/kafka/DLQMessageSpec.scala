package app.kafka

import app.utils.ArbitraryGenerator.sample
import app.utils.BaseSpec

class DLQMessageSpec extends BaseSpec {

  import io.circe.syntax._

  "apply with two parameters" should {
    "create the instance with json fields" in {
      val key       = sample[String]
      val throwable = new Throwable(sample[String])

      val expected = new DLQMessage(
        key,
        throwable.toString.asJson,
        throwable.getStackTrace.toList.map(_.toString).asJson
      )

      DLQMessage.apply(key, throwable) mustBe expected
    }
  }
}
