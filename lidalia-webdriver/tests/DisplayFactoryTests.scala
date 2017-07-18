package uk.org.lidalia
package webdriver

import org.scalatest.{FunSuite, Tag}
import scalalang.os.{Linux, OsFamily, Windows}

import util.Random

class DisplayFactoryTests extends FunSuite {

  test("returns none if operating system does not support it") {
    val expectedOutcome = Random.nextInt()

    val outcome = DisplayFactory(osFamily = Windows).using { optionalDisplay =>
      (optionalDisplay, expectedOutcome)
    }

    assert(outcome == (None, expectedOutcome))
  }

  test("returns none if underlying process not installed") {
    val expectedOutcome = Random.nextInt()

    val outcome = DisplayFactory(osFamily = Linux, command = "not_a_real_command").using { optionalDisplay =>
      (optionalDisplay, expectedOutcome)
    }

    assert(outcome == (None, expectedOutcome))
  }

  testOnLinux("creates a display") {

    val expectedOutcome = Random.nextInt()

    val outcome = DisplayFactory(osFamily = Linux).using { optionalDisplay =>
      (optionalDisplay, expectedOutcome)
    }

    assert(outcome._1.isDefined)
    assert(outcome._2 == expectedOutcome)
  }

  def testOnLinux(testName: String, testTags: Tag*)(testFun: => Unit): Unit = {
    if (OsFamily() == Linux) {
      test(testName, testTags:_*)(testFun)
    } else {
      ignore(testName)(testFun)
    }
  }
}
