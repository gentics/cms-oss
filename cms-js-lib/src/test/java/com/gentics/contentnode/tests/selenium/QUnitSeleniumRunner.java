package com.gentics.contentnode.tests.selenium;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public class QUnitSeleniumRunner {

	RemoteWebDriver driver;

	public QUnitSeleniumRunner(RemoteWebDriver driver) {
		this.driver = driver;
	}

	public void executeTest(String filename) throws InterruptedException {
		String hostname = "qa-sandbox-2.office";

		this.driver.get("http://" + hostname + "/DEV/test/js/" + filename);
		Thread.sleep(1500);
		WebElement testRootElement = driver.findElement(By.id("qunit-tests"));
		List<WebElement> testCaseElements = testRootElement.findElements(By.xpath("//ol[@id='qunit-tests']/li"));

		System.out.println(testCaseElements.size());

		// Iterate over all testcases
		for (WebElement testCaseElement : testCaseElements) {

			System.out.println("TestName: " + testCaseElement.findElement(By.className("test-name")).getText());
			System.out.println("Name: " + testCaseElement.findElement(By.className("module-name")).getText());
			String testCaseState = testCaseElement.getAttribute("class");

			System.out.println("Class: " + testCaseState);

			if (testCaseState.equalsIgnoreCase("running")) {
				System.out.println("This test is still running. We should treat it like it failed.");
				continue;
			}
			System.out.println("Failed: " + testCaseElement.findElement(By.className("failed")).getText());
			System.out.println("Passed: " + testCaseElement.findElement(By.className("passed")).getText());

			// Find all asserts
			List<WebElement> assertElements = testCaseElement.findElements(By.xpath(".//li"));

			System.out.println(assertElements.size());

			for (WebElement assertElement : assertElements) {
				String extraMessage = "";

				try {
					extraMessage = assertElement.findElement(By.className("test-message")).getText();
				} catch (NoSuchElementException e) {
					System.out.println("This assert has no extra test-message. Skipping..");
				}
				System.out.println("Assert: " + assertElement.getText() + " - " + extraMessage + " - " + assertElement.getAttribute("class"));
				try {
					String expectedValue = assertElement.findElement(By.xpath(".//pre")).getText();

					System.out.println("Expected: " + expectedValue);
				} catch (NoSuchElementException e) {
					System.out.println("This assert has no expected value. Skipping..");
				}
			}
			System.out.println("-------------\n");
		}
	}
}
