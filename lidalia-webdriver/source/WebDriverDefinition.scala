package uk.org.lidalia
package webdriver

import java.io.File
import java.nio.file.Files

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService.Builder
import org.slf4j.LoggerFactory
import uk.org.lidalia.scalalang.ResourceFactory
import uk.org.lidalia.scalalang.TryFinally._try
import uk.org.lidalia.scalalang.os.OsFamily
import uk.org.lidalia.webdriver.WebDriverDefinition.driverFile

import scala.collection.JavaConversions.mapAsJavaMap

object WebDriverDefinition {

  def apply(display: ?[Display] = None) = new WebDriverDefinition(display)

  val log = LoggerFactory.getLogger(classOf[WebDriverDefinition])

  def driverFile(os: OsFamily): File = {
    val fileName = s"chromedriver-${os.toString.toLowerCase}"
    getResourceAsFile(fileName)
  }

  def getResourceAsFile(fileName: String): File = {
    val url = Thread.currentThread().getContextClassLoader.getResource(fileName)
    val driverFile = try {
      new File(url.toURI)
    } catch {
      case e: Exception =>
        val in = url.openStream()
        val tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp")
        tempFile.deleteOnExit()
        Files.copy(in, tempFile.toPath)
        tempFile
    }

    driverFile.setExecutable(true)
    driverFile
  }

  def main(args: Array[String]) {
    WebDriverDefinition().using { driver =>
      println(s"Using $driver")
    }
  }
}

class WebDriverDefinition private(display: ?[Display]) extends ResourceFactory[ReusableWebDriver] {

  override def using[T](work: (ReusableWebDriver) => T): T = {

    val env = display.map { d => Map("DISPLAY" -> s":${d.id}") }.getOrElse(Map())
    val chromeDriver = driverFile(OsFamily())
    val chromeDriverService = new Builder()
        .usingDriverExecutable(chromeDriver)
        .withSilent(true)
        .withEnvironment(env)
        .build()

    _try {
      chromeDriverService.start()

      val driver = ReusableWebDriver(new ChromeDriver(chromeDriverService))

      work(driver)

    } _finally {
      chromeDriverService.stop()
    }
  }
}
