package uk.org.lidalia.scalatest

import org.scalatest.{Args, Status, Suite, Suites}
import uk.org.lidalia.scalalang.ResourceFactory
import uk.org.lidalia.scalatest.AroundSuites.FactoryData

import scala.collection.immutable

object AroundSuites {
  type FactoryData[T] = Either[ResourceFactory[T], T]
}

abstract class AroundSuites[R](resourceData: FactoryData[R]) extends Suite {

  def suites(resource: R): immutable.Seq[Suite]

  override def run(testName: Option[String], args: Args): Status = {

    resourceData.fold(
      { resourceFactory =>
        runUsingResource(testName, args, resourceFactory)
      },
      { resource =>
        runSuites(testName, args, resource)
      }
    )
  }

  private def runUsingResource(testName: Option[String], args: Args, resourceFactory: ResourceFactory[R]): Status = {
    resourceFactory.using { resource =>

      val status = runSuites(testName, args, resource)

      status.waitUntilCompleted()

      status
    }
  }

  private def runSuites(testName: Option[String], args: Args, resource: R): Status = {
    new Suites(
      suites(resource): _*
    ).run(testName, args)
  }

  def this(resourceFactory: ResourceFactory[R]) = this(Left(resourceFactory))

}
