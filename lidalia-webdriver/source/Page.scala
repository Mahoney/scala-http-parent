package uk.org.lidalia
package webdriver

import org.openqa.selenium.{By, WebElement}
import net.UriReference


trait PageFactory[P <: Page[P]] {

  def url: UriReference
  def apply(reusableWebDriver: ReusableWebDriver): P
}

trait Page[P <: Page[P]] {

  val driver: ReusableWebDriver

  def title = driver.getTitle

  def isCurrentPage: Boolean

  def $(selector: String): WebElement = {
    driver.findElement(By.cssSelector(selector))
  }

  override def toString = {
    s"${getClass.getSimpleName}[${driver.getCurrentUrl}]"
  }

}
