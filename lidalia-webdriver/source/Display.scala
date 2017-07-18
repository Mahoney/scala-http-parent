package uk.org.lidalia
package webdriver

import java.io.File
import java.time.Duration

import scalalang.NoOpValues.noop
import scalalang.TryFinally._try
import scalalang.WaitFor.waitFor
import scalalang.os.{Linux, OsFamily}
import scalalang.{NoneResourceFactory, ResourceFactory, SomeResoureFactory}
import uk.org.lidalia.scalalang.process.{ProcessBuilderExt, ProcessExt}

import collection.immutable
import sys.process.{Process, ProcessLogger}
import util.Random

object DisplayFactory {

  def apply(
    displayId: ?[Int] = None,
    options: Map[String, String] = Map(),
    osFamily: OsFamily = OsFamily(),
    command: String = "Xephyr"
  ): ResourceFactory[?[Display]] = {
      if (supportedOn(osFamily) && programExists(command)) {
        SomeResoureFactory(
          new DisplayFactory(command, displayId, options)
        )
      } else {
        NoneResourceFactory
      }
  }

  val supportedOsFamilies = immutable.Map[OsFamily, String](
    Linux -> "sudo apt-get install xserver-xephyr on Debian/Ubuntu"
  )

  def supportedOn(osFamily: OsFamily): Boolean = {
    supportedOsFamilies.keySet.contains(osFamily)
  }

  def programExists(name: String): Boolean = {
    val swallowOutput = ProcessLogger(noop)
    Process(s"which $name").!(swallowOutput) == 0
  }

  def adapt[R](factory: ResourceFactory[R]) = {
    new SomeResoureFactory(factory)
  }
}

class DisplayFactory private (
  command: String,
  displayId: ?[Int],
  options: Map[String, String]
) extends ResourceFactory[Display] {

  override def using[T](work: (Display) => T): T = {
    val _displayId = displayId.getOrElse(freeDisplayId())
    val proc = startXephyr(_displayId)

    _try {
      work(Display(_displayId))
    } _finally {
      proc.destroy()
    }
  }

  private def startXephyr[T](_displayId: Int): ProcessExt = {
    val optionString = options.map { case (k, v) => s"$k $v" }.mkString(" ")
    val proc = ProcessBuilderExt(s"$command :${_displayId} $optionString").run()
    waitFor(timeout = Duration.ofSeconds(10)) {
      new File(s"/tmp/.X11-unix/X${_displayId}").exists()
    }
    proc
  }

  private def freeDisplayId() = {
    val existingDisplayIds = lockFiles().map(x => x.getName.split('X')(1).split('-')(0).toInt)
    def randomFreeDisplayId(): Int = {
      val displayId = Random.nextInt(1000)
      if (existingDisplayIds.contains(displayId))
        randomFreeDisplayId()
      else
        displayId
    }
    randomFreeDisplayId()
  }

  private def lockFiles(): List[File] = {
    new File("/tmp").listFiles().filter { file =>
      file.isFile && file.getName.matches("\\.X\\d+-lock")
    }.toList
  }
}

case class Display(id: Int)
