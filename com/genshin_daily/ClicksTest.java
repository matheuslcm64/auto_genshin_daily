package com.genshin_daily;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;

public class ClicksTest {
   private WebDriver driver;
   JavascriptExecutor js;
   ChromeOptions options;
   Actions actionProvider;

   @Before
   public void setUp() {
      System.setProperty("webdriver.chrome.driver", "C:\\Drivers\\chromedriver.exe");
      this.options = new ChromeOptions();
      this.options.addArguments(new String[]{"user-data-dir=C:\\Users\\Matheus Lucas\\AppData\\Local\\Google\\Chrome\\Automation"});
      this.driver = new ChromeDriver(this.options);
      this.js = (JavascriptExecutor)this.driver;
      this.actionProvider = new Actions(this.driver);
      
      try {
          // Replace with the path to your WebDriver executable
          String driverPath = "C:\\Drivers\\chromedriver.exe"; // For ChromeDriver

          // Create a process to run the driver with the --version flag
          Process process = new ProcessBuilder(driverPath, "--version").start();

          // Capture the output
          BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line;
          while ((line = reader.readLine()) != null) {
              System.out.println(line); // Print the version
          }

          // Wait for the process to complete
          process.waitFor();

      } catch (Exception e) {
          e.printStackTrace();
      }
      
   }

   @After
   public void tearDown() {
      this.driver.quit();
   }

   @Test
   public void clicks() {
      this.driver.navigate().to("https://webstatic-sea.mihoyo.com/ys/event/signin-sea/index.html?act_id=e202102251931481&lang=en-us");
      this.driver.manage().window().setSize(new Dimension(1366, 728));

      try {
         Thread.sleep(5000L);
      } catch (InterruptedException var6) {
         var6.printStackTrace();
      }

      int count_errors = 0, count_elems = 1;
      List<WebElement> elems = this.driver.findElements(By.cssSelector("div[class^=components-home-assets-__sign-content-test_---sign-list] > div"));
      System.out.println("Found "+elems.size()+" elements");

      for(WebElement elem : elems) {
         try {
        	System.out.println("Visiting element: "+count_elems);
            this.actionProvider.moveToElement(elem).click().perform();
            count_elems++;
            Thread.sleep(1000L);
         } catch (Exception var5) {
            count_errors++;
         }
      }

      System.out.println("Errors: "+count_errors);
   }
}
