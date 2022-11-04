package app.utils

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{EitherValues, Inspectors, OptionValues}

trait BaseSpec extends AnyWordSpecLike with must.Matchers with OptionValues with EitherValues with Inspectors with ScalaFutures {

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = scaled(Span(1000, Millis)),
    interval = scaled(Span(50, Millis))
  )
}
