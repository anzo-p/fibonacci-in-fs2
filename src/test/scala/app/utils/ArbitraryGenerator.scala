package app.utils

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Arbitrary

object ArbitraryGenerator {

  def sample[A : Arbitrary]: A = arbitrary[A].sample.get
}
