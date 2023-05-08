package com.gentics.contentnode.tests.selenium;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

public class QUnitSeleniumRunnerTest {

	RemoteWebDriver driver;

	@Before
	public void setup() {
		driver = new ChromeDriver();
	}

	@BeforeClass
	public static void setupOnce() {
		System.setProperty("webdriver.chrome.driver", "/opt/selenium/chromedriver_linux64_23.0.1240.0");
	}

	@Test
	public void testRunner() throws InterruptedException {

		QUnitSeleniumRunner runner = new QUnitSeleniumRunner(driver);

		runner.executeTest("content-object-tests.html");

	}

	@After
	public void tearDown() {
		driver.close();
	}
}
