/*
 * @author johannes2
 * @date 13.05.2008
 * @version $Id: ParallelSchedulerTaskTest.java,v 1.2 2010-06-13 19:08:35 johannes2 Exp $
 */
package com.gentics.contentnode.tests.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.utils.SQLDumpUtils;

@Ignore
public class ParallelSchedulerTaskTest {

	// false: run test on dev6 , true: run test local using user workspace from
	// dev42 (just for development)
	public static boolean testmode = false;

	public static final boolean PARALLEL = true;

	public static final boolean NOTPARALLEL = false;

	public static final String PATHTOSCRIPTS = "/tmp/scheduler/";

	public static NodeLogger logger = NodeLogger.getNodeLogger(ParallelSchedulerTaskTest.class);

	public DatabaseTaskUtils dbtask;

	public Properties schedulertestconf;

	public static final String CHECKOUTROOTDIR = "/home/disting/disting/launchdist/tmp/checkout/";

	public static final String DEV42USER = "johannes2";

	@Before
	public void setUp() throws Exception {
		schedulertestconf = new Properties();
		schedulertestconf.load(ParallelSchedulerTaskTest.class.getResourceAsStream("schedulertest.properties"));
		dbtask = new DatabaseTaskUtils(schedulertestconf);
		dbtask.sqlUtils.connectDatabase();
		dbtask.cleanUsecase();
	}

	@After
	public void tearDown() throws Exception {

		assertFalse("There was one or more existing PHP processes at the end of this test. This shouldn't occure. Killing the process . . .", killProcesses());
	}

	/**
	 * This method is used to create a temporary shell script (bash)
	 * 
	 * @param bashScript
	 */
	public void writeScript(String bashScript, String name) {

		FileWriter fw = null;
		File dir = new File(PATHTOSCRIPTS);

		if (!dir.exists()) {
			if (!dir.mkdir()) {
				System.out.println("Could not create directory for scripts");
				return;
			}
		}
		File tmp = new File(PATHTOSCRIPTS + name);

		if (tmp.exists()) {
			tmp.delete();
		}

		try {
			fw = new FileWriter(tmp);
			fw.write("#!/bin/bash\n");
			fw.write(bashScript);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e) {}
		}

	}

	/**
	 * Execute the PHP scheduler process
	 * 
	 * @param remote
	 */
	public void executeScheduler(boolean remote, boolean paralleltasksEnabled) {
		if (remote) {
			remoteExecutionProcedure(paralleltasksEnabled);
		} else {
			localExecutionProcedure(paralleltasksEnabled);
		}

	}

	private void remoteExecutionProcedure(boolean paralleltasksEnabled) {

		String conf = new String();

		if (paralleltasksEnabled) {
			conf = "node.remote.parallel.conf";
		} else {
			conf = "node.remote.nonparallel.conf";
		}

		// this script will be placed in ~/ on dev42
		String remoteBashScript = "echo starting scheduler\n" + "source /Node/bin/env.sh\n" + "/Node/bin/php -q /home/" + DEV42USER
				+ "/cvs/node_41_stable/.node/sh.php --no-owner do=880 CONFIGFILE=/home/" + DEV42USER + "/" + conf + " &\n" + "sleep 1\n" + "echo PID: `pidof -s php`";

		writeScript(remoteBashScript, "remotestart.sh");

		// remote execution is only used for developing
		String bashScript = "echo Starting remote scheduler:\n" + "scp " + PATHTOSCRIPTS + "remotestart.sh " + DEV42USER + "@dev42.office:~/\n" + "scp "
				+ this.getClass().getResource(conf).getFile().replaceAll("%20", "\\\\ ") + " " + DEV42USER + "@dev42.office:~/\n"
				+ "ssh root@dev42.office sudo -u node sh /home/" + DEV42USER + "/remotestart.sh\n";

		writeScript(bashScript, "start.sh");
		executeScriptCommand("start.sh", false);

	}

	private void localExecutionProcedure(boolean paralleltasksEnabled) {

		String conf = new String();

		if (paralleltasksEnabled) {
			conf = "node.parallel.conf";
		} else {
			conf = "node.notparallel.conf";
		}

		// the following code is an ugly workaround - java exec does not support
		// full bash syntax even if i tried bash -c [BASH] (same with sh)
		String bashScript = "source /Node/bin/env.sh\n" + CHECKOUTROOTDIR + ".node/sh.php --no-owner do=880 CONFIGFILE="
				+ this.getClass().getResource(conf).getFile();

		writeScript(bashScript, "tmp.sh");

		String startBashScript = "mkdir " + CHECKOUTROOTDIR + ".node/modules/\n" + "ln -s " + CHECKOUTROOTDIR + "schedule.node/ " + CHECKOUTROOTDIR
				+ ".node/modules/scheduler\n" + "ln -s " + CHECKOUTROOTDIR + "system.node/ " + CHECKOUTROOTDIR + ".node/modules/system\n" + "sudo -u node bash "
				+ PATHTOSCRIPTS + "tmp.sh\n";

		writeScript(startBashScript, "start.sh");
		System.out.println("executeScriptCommand returned: " + executeScriptCommand("start.sh", true));

	}

	/**
	 * Kill remote/local process with given pid
	 * 
	 * @param pid
	 * @return
	 */
	private boolean killProcesses() {

		// get all pids of running php processes that match to the test php
		// processes
		ArrayList pids = getPids();

		if (pids == null) {
			return false;
		}

		// TODO add pids in one line to kill command to kill multiple processes
		// with one line
		String killBashScript = new String();
		int i = 0;

		while (i < pids.size()) {
			killBashScript += "sudo -u node kill " + pids.get(i) + "\n" + "echo RETURNED: $?\n";
			i++;
		}

		writeScript(killBashScript, "kill.sh");
		String output = new String();

		if (testmode) {
			String executekillBashScript = "scp " + PATHTOSCRIPTS + "kill.sh " + DEV42USER + "@dev42.office:~/\n"
					+ "ssh root@dev42.office sudo -u node sh /home/" + DEV42USER + "/kill.sh\n";

			writeScript(executekillBashScript, "execute_kill.sh");
			output = executeScriptCommand("execute_kill.sh", false);
		} else {
			output = executeScriptCommand("kill.sh", false);
		}
		System.out.println("Killoutput: " + output);

		int idx = output.indexOf("RETURNED: ");
		int end = output.indexOf("\n", idx);

		if (end == -1) {
			end = output.length();
		}
		String code = output.substring(idx + 10, end);
		int returnCode = Integer.parseInt(code);

		return returnCode == 0 ? true : false;

	}

	private ArrayList getPids() {
		String bashScript = "ps aux | grep php | grep sh.php | grep -v grep";

		return getPids(bashScript, "node");
	}

	/**
	 * This method is used to find all php processes that have just been started by the test
	 * 
	 * @return
	 */
	private ArrayList getPids(String locatorBashScript, String username) {

		System.out.println("Getting running processes");
		ArrayList pids = new ArrayList();

		writeScript(locatorBashScript, "getpids.sh");
		String output = new String();

		if (testmode) {
			String executeGetPidsBashScript = "scp " + PATHTOSCRIPTS + "getpids.sh " + DEV42USER + "@dev42.office:~/\n"
					+ "ssh root@dev42.office sudo -u node sh /home/" + DEV42USER + "/getpids.sh";

			writeScript(executeGetPidsBashScript, "RunGetpids.sh");
			output = executeScriptCommand("RunGetpids.sh", false);
		} else {
			output = executeScriptCommand("getpids.sh", false);
		}
		System.out.println("getPids Output: " + output);

		if (output.length() == 0) {
			return null;
		}
		String[] lines = output.split("\n");
		int i = 0;

		while (i < lines.length) {
			String p1 = lines[i].replaceAll(username + "[\\s]*", "");
			String p2 = p1.substring(0, p1.indexOf(" "));

			pids.add(p2);
			i++;
		}

		return pids;
	}

	/**
	 * This method is used to execute a shell script.
	 * 
	 * @param scriptname
	 *            only the name of the file of the script that is located in PATHTOSCRIPTS
	 * @param deatchProcess
	 * @return
	 */
	public String executeScriptCommand(String scriptname, boolean detachProcess) {
		return executeScriptCommand(PATHTOSCRIPTS, scriptname, detachProcess);
	}

	/**
	 * This method is used to execute a shell script.
	 * 
	 * @param detachprocess
	 *            boolean you can choose to detach the process from the jvm
	 * @param scriptpath
	 * @param scriptname
	 * @return output of the command
	 */
	public String executeScriptCommand(String scriptpath, String scriptname, boolean detachProcess) {

		String fullscriptpath = scriptpath + scriptname;
		String response = new String();
		String command = new String();

		if (detachProcess) {
			command = "screen -d -m bash " + fullscriptpath;
		} else {
			command = "bash " + fullscriptpath;
		}
		try {
			Process schedulerPHPProcess = Runtime.getRuntime().exec(command);

			if (detachProcess) {
				return "detached";
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(schedulerPHPProcess.getInputStream()));

			for (String s; (s = in.readLine()) != null;) {
				response += s + "\n";
			}
			if (response.length() != 0) {
				// removing last \n
				response = response.substring(0, response.length() - 1);
			}

		} catch (IOException e) {
			logger.debug("Error while execution:", e);
		}
		return response;
	}

	/**
	 * @param seconds
	 */
	private void doSleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testSelfTestDetachProcess() {

		// this is a test for local execution only
		if (!testmode) {
			File start = new File(PATHTOSCRIPTS + "start");
			File stop = new File(PATHTOSCRIPTS + "stop");

			start.delete();
			stop.delete();

			String bashScript = "touch " + PATHTOSCRIPTS + "start\n" + "sleep 4\n" + "touch " + PATHTOSCRIPTS + "stop\n";

			writeScript(bashScript, "tmp.sh");
			String output = executeScriptCommand("tmp.sh", true);

			assertTrue(output.equalsIgnoreCase("detached"));
			doSleep(1);

			assertTrue(start.exists());
			assertFalse(stop.exists());
			doSleep(5);
			assertTrue(stop.exists());
		}
	}

	@Test
	public void testSelfTestGetPids() {

		// this is a test for local execution only
		if (!testmode) {
			File start = new File(PATHTOSCRIPTS + "start");
			File stop = new File(PATHTOSCRIPTS + "stop");

			start.delete();
			stop.delete();

			String bashScript = "touch " + PATHTOSCRIPTS + "start\n" + "sleep 4\n" + "touch " + PATHTOSCRIPTS + "stop\n";

			writeScript(bashScript, "tmp.sh");
			String output = executeScriptCommand("tmp.sh", true);

			assertTrue(output.equalsIgnoreCase("detached"));

			String whoamibashScript = "whoami";

			writeScript(whoamibashScript, "who.sh");
			String iam = executeScriptCommand("who.sh", false);

			System.out.println("You are: '" + iam + "'");
			ArrayList pids = getPids("ps aux | grep sleep | grep -v grep", iam);

			assertTrue(pids.size() != 0);
			int i = 0;

			while (i < pids.size()) {
				System.out.println(pids.get(i));
				i++;
			}
			doSleep(1);
			assertTrue(start.exists());
			assertFalse(stop.exists());
			doSleep(5);
			assertTrue(stop.exists());
		}
	}

	@Test
	public void testSelfTestNotDetachProcess() {

		// this is a test for local execution only
		if (!testmode) {
			File start = new File(PATHTOSCRIPTS + "start");
			File stop = new File(PATHTOSCRIPTS + "stop");

			start.delete();
			stop.delete();

			String bashScript = "touch " + PATHTOSCRIPTS + "start\n" + "sleep 4\n" + "touch " + PATHTOSCRIPTS + "stop\n" + "echo 1234\n";

			writeScript(bashScript, "tmp.sh");
			String output = executeScriptCommand("tmp.sh", false);

			assertTrue(output.equalsIgnoreCase("1234"));
			doSleep(1);

			assertTrue(start.exists());
			assertTrue(stop.exists());

		}
	}

	// public void testPHPExists() {
	// // this is a test for local execution only
	// if (!testmode) {
	// String bashScript = "source /Node/bin/env.sh\n" + "/Node/bin/php -h";
	// writeScript(bashScript, "tmp.sh");
	// String output = executeScriptCommand("tmp.sh", false);
	// System.out.println("Php returned: " + output);
	// assertTrue(output.length() > 4);
	// }
	// }
	@Test
	public void testAmokPHP() {
		dbtask.insertUsecase(1);
		executeScheduler(testmode, PARALLEL);
		doSleep(1);
		ArrayList report = dbtask.getJobRun();

		if (report.size() > 2) {
			fail("There were results in the database. This shouln't occure because we want a still running not finished php process in the background.");
		}
		assertTrue("There was no existing PHP process at the end of this test. This shouldn't occure.", killProcesses());
	}

	/**
	 * Usecase 1: Test of start conditions of subtasks. no-parallel tasks should not run parallel.
	 */
	@Test
	public void testUseCase1() {

		// * create RootTask1 (sleep 5) (started upon scheduler start)
		// ** create SubTask1 (sleep 10) (Execution condition is termination of
		// RootTask1)
		// ** create SubTask2 (sleep 15) (Execution condition is termination of
		// RootTask1)
		dbtask.insertUsecase(1);

		executeScheduler(testmode, PARALLEL);
		System.out.println("waiting some time because the scheduler is preforming its tasks right now");
		doSleep(40);
		ArrayList report = dbtask.getJobRun();

		report = normalizeReport(report);
		printJobRun(report);

		// define start/stop times
		ArrayList references = new ArrayList();

		references.add(new int[] { 1, 0, 5 });
		references.add(new int[] { 2, 6, 16 });
		references.add(new int[] { 3, 16, 31 });

		compareJobRun(report, references, 2);

	}

	/**
	 * Usecase 2: Test if parallel task run parallel.
	 */
	@Test
	public void testUseCase2() {

		// * create RootTask1 (sleep 1) (started upon scheduler start)
		// ** create ParallelSubTask1 (sleep 20) (Execution condition is
		// termination of RootTask1)
		// ** create ParallelSubTask2 (sleep 10) (Execution condition is
		// termination of RootTask1)
		dbtask.insertUsecase(2);

		executeScheduler(testmode, PARALLEL);
		System.out.println("waiting some time because the scheduler is preforming its tasks right now");

		doSleep(40);
		ArrayList report = dbtask.getJobRun();

		report = normalizeReport(report);
		printJobRun(report);

		// define start/stop times
		ArrayList references = new ArrayList();

		references.add(new int[] { 1, 0, 1 });
		references.add(new int[] { 2, 1, 21 });
		references.add(new int[] { 3, 1, 11 });

		compareJobRun(report, references, 3);

	}

	/**
	 * Usecase 3: Test if one non parallel subtask can run together with multiple parallalRootTasks.
	 */
	public void testUseCase3() {

		// * create RootTask (sleep 1) (started upon scheduler start)
		// ** create SubTask1 (sleep 60) (Execution condition is termination of
		// RootTask)
		// ** create SubTask2 (sleep 60) (Execution condition is termination of
		// RootTask)
		// * create ParallelRootTask1 (sleep 10) (started upon scheduler start)
		// * create ParallelRootTask2 (sleep 10) (started upon scheduler start)
		dbtask.insertUsecase(3);

		executeScheduler(testmode, true);
		System.out.println("waiting some time because the scheduler is preforming its tasks right now");
		doSleep(35);

		ArrayList report = dbtask.getJobRun();

		// printJobRun(report);
		report = normalizeReport(report);
		printJobRun(report);

		// define start/stop times
		ArrayList references = new ArrayList();

		references.add(new int[] { 1, 0, 1 });
		references.add(new int[] { 2, 1, 7 });
		references.add(new int[] { 3, 7, 28 });
		references.add(new int[] { 4, 0, 10 });
		references.add(new int[] { 5, 0, 10 });

		compareJobRun(report, references, 3);

	}

	/**
	 * Usecase 4: Test if RootTask was not restarted when his subtasks (parallel) are currently running.
	 */
	@Test
	public void testUseCase4() {

		// * create RootTask (sleep 1) (Execution condition is an interval of
		// 1min)
		// ** create ParallelSubTask1 (sleep 60) (Execution condition is
		// termination of
		// RootTask)
		// ** create ParallelSubTask2 (sleep 60) (Execution condition is
		// termination of
		// ParallelSubTask1)
		dbtask.insertUsecase(4);

		// 1. start of scheduler
		executeScheduler(testmode, PARALLEL);

		// wait until RootTask is overdue
		System.out.println("waiting some time because the scheduler is preforming its tasks right now");
		doSleep(70);
		// 2. start of scheduler
		executeScheduler(testmode, true);

		// wait 5 sec and deactivate the roottask (stop the interval) otherwise
		// the scheduler won't stop.
		doSleep(5);
		dbtask.deactivateJob(1);

		// wait some time to make sure that the subtasks terminate
		doSleep(240);

		ArrayList report = dbtask.getJobRun();

		report = normalizeReport(report);
		printJobRun(report);

		// define start/stop times
		ArrayList references = new ArrayList();

		references.add(new int[] { 1, 0, 10 });
		references.add(new int[] { 2, 11, 71 });
		references.add(new int[] { 3, 71, 101 });

		compareJobRun(report, references, 3);

	}

	/**
	 * Usecase 5: Parallel Subtask with childSubTask (not parallel) & parentRootTask (not parallel))
	 */
	@Test
	public void testUseCase5() {

		// * create RootTaskA (sleep 1) (started upon scheduler start)
		// ** create SubTaskA1 (sleep 5) (Execution condition is termination of
		// RootTaskA)
		// ** create ParallelSubTaskA2 (sleep 20) (Execution condition is
		// termination of RootTaskA)
		// *** create SubTaskA21 (sleep 15) (Execution condition is termination
		// of SubTaskA)
		// ** create SubTaskA3 (sleep 15) (Execution condition is termination of
		// RootTaskA)

		dbtask.insertUsecase(5);

		executeScheduler(testmode, PARALLEL);
		System.out.println("waiting some time because the scheduler" + " is preforming its tasks right now");
		doSleep(80);

		ArrayList report = dbtask.getJobRun();

		report = normalizeReport(report);
		printJobRun(report);
		// define start/stop times

		ArrayList references = new ArrayList();

		references.add(new int[] { 1, 0, 1 });
		references.add(new int[] { 2, 1, 16 });
		references.add(new int[] { 3, 1, 21 });
		references.add(new int[] { 4, 32, 47 });
		references.add(new int[] { 5, 17, 32 });

		compareJobRun(report, references, 3);

	}

	/**
	 * Usecase 6: Complex parallel task. Everything parallel. Parallel Subtasks & RootTasks. Verify that all parallel tasks run parallel.
	 */
	@Test
	public void testUseCase6() {

		// * ParallelRootTaskA (sleep 5) (started upon scheduler start)
		// ** ParallelSubTaskA1 (sleep 10) (started upon scheduler start)
		// ** ParallelSubTaskA2 (sleep 10) (started upon scheduler start)
		// * ParallelRootTaskB (sleep 5) (started upon scheduler start)
		// ** ParallelSubTaskB1 (sleep 10) (started upon scheduler start)
		// ** ParallelSubTaskB2 (sleep 10) (started upon scheduler start)

		dbtask.insertUsecase(6);

		executeScheduler(testmode, PARALLEL);

		System.out.println("waiting some time because the scheduler" + " is preforming its tasks right now");
		doSleep(40);

		ArrayList report = dbtask.getJobRun();

		printJobRun(report);
		report = normalizeReport(report);
		printJobRun(report);
		// define start/stop times

		ArrayList references = new ArrayList();

		references.add(new int[] { 1, 0, 5 });
		references.add(new int[] { 2, 0, 5 });
		references.add(new int[] { 3, 6, 16 });
		references.add(new int[] { 4, 6, 16 });
		references.add(new int[] { 5, 6, 16 });
		references.add(new int[] { 6, 6, 16 });
		references.add(new int[] { 6, 16, 26 });

		compareJobRun(report, references, 2);

	}

	/*
	 * The Tests below are disable for now
	 */

	//
	// /**
	// * Usecase 7: Test if RootTask will not restart when his subtasks (not
	// * parallel) aren't terminated yet.
	// */
	// public void testUseCase7() {
	//
	// // * create RootTask (sleep 10) (Execution condition is an interval of
	// // 1min)
	// // ** create ParallelSubTask1 (sleep 60) (Execution condition is
	// // termination of RootTask)
	// // ** create ParallelSubTask2 (sleep 30) (Execution condition is
	// // termination of ParallelSubTask1)
	//
	// dbtask.insertUsecase(7);
	//
	// // 1. start of scheduler
	// executeScheduler(testmode, PARALLEL);
	//
	// // wait some time but do not wait until RootTask is overdue!
	//
	// System.out.println("waiting some time because the scheduler is"
	// + " preforming its tasks right now");
	// doSleep(70);
	//
	// // 2. start of scheduler
	// executeScheduler(testmode, PARALLEL);
	//
	// doSleep(5);
	// dbtask.deactivateJob(1);
	// // wait some time to make sure that the subtasks from the 1. run and the
	// // subtasks from the 2. run terminate
	// System.out.println("waiting some time because the scheduler is"
	// + " preforming its tasks right now");
	// doSleep(130);
	//
	// // Deactivate root task to make sure it wont respawn another scheduler
	// // run.
	//
	// ArrayList report = dbtask.getJobRun();
	//
	// report = normalizeReport(report);
	// printJobRun(report);
	// // define start/stop times
	// int[] startJobRunReference = {0, 10, 71};
	// int[] stopJobRunReference = {10, 71, 100};
	// int[][] referenceTimes = {startJobRunReference, stopJobRunReference};
	// compareJobRun(report, referenceTimes, 5);
	//
	// }
	//
	// /**
	// * Usecase 8: Test if RootTask will not restart when his subtasks (not
	// * parallel) aren't terminated yet. (Case with only dependencies to
	// * RootTask)
	// */
	// public void testUseCase8() {
	//
	// // * create RootTask (sleep 1) (Execution condition is an interval of
	// // 2 mins)
	// // ** create SubTask1 (sleep 60) (Execution condition is
	// // termination of RootTask)
	// // ** create SubTask2 (sleep 80) (Execution condition is
	// // termination of RootTask)
	// // ** create SubTask2 (sleep 40) (Execution condition is
	// // termination of RootTask)
	//
	// dbtask.insertUsecase(8);
	//
	// // 1. start of scheduler
	// executeScheduler(testmode, PARALLEL);
	//
	// // wait some time until RootTask is overdue.
	//
	// System.out.println("waiting some time because the scheduler is"
	// + " preforming its tasks right now");
	// doSleep(130);
	//
	// // Stop Interval - deactivate root task to make sure it wont respawn
	// // another scheduler
	// // run.
	// dbtask.deactivateJob(7);
	//
	// // wait some time to make sure that the subtasks from the 1. run and the
	// // subtasks from the 2. run terminate
	//
	// System.out.println("waiting some time because the scheduler is"
	// + " preforming its tasks right now");
	// doSleep(150);
	//
	// ArrayList report = dbtask.getJobRun();
	//
	// report = normalizeReport(report);
	// printJobRun(report);
	//
	// // define start/stop times
	// int[] startJobRunReference = {0, 61, 141};
	// int[] stopJobRunReference = {60, 141, 181};
	// int[][] referenceTimes = {startJobRunReference, stopJobRunReference};
	// compareJobRun(report, referenceTimes, 3);
	//
	// }
	/**
	 * This method subtracts the first time value from all time values to get the timings of the task relative to the first tasks time (start time)
	 * 
	 * @param report
	 * @return
	 */
	public ArrayList normalizeReport(ArrayList report) {
		ArrayList reportNormalized = (ArrayList) report.clone();
		long firsttime = Long.valueOf(((ArrayList) report.get(0)).get(2).toString()).longValue();

		int i = 0;

		while (i < report.size()) {
			ArrayList row = (ArrayList) reportNormalized.get(i);
			long cval;

			cval = Long.valueOf(row.get(2).toString()).longValue();
			row.set(2, String.valueOf((cval - firsttime)));

			cval = Long.valueOf(row.get(3).toString()).longValue();
			row.set(3, String.valueOf((cval - firsttime)));
			i++;
		}
		return reportNormalized;
	}

	/**
	 * Print the given 'jobrun' report ArrayList
	 * 
	 * @param report
	 * @return
	 */
	public boolean printJobRun(ArrayList report) {
		System.out.println("\nJobrun:");
		int i = 0;

		while (i < report.size()) {

			ArrayList row = (ArrayList) report.get(i);
			int e = 0;

			while (e < row.size()) {
				if (row.get(e) == null) {
					System.out.print("null - ");
				} else {
					System.out.print(row.get(e).toString() + " - ");
				}
				e++;
			}
			i++;
			System.out.println();
		}
		return false;
	}

	/**
	 * Compare the given 'jobrun' report data with reference times
	 * 
	 * @param report
	 * @param referenceTimes
	 * @param tolerance
	 *            +- tolerance of the reference timings
	 * @return
	 */
	public void compareJobRun(ArrayList report, ArrayList references, int tolerance) {

		int i = 0;

		while (i < report.size()) {

			ArrayList row = (ArrayList) report.get(i);
			int reportJobId = Integer.valueOf(row.get(1).toString()).intValue();
			int reportStart = Integer.valueOf(row.get(2).toString()).intValue();
			int reportStop = Integer.valueOf(row.get(3).toString()).intValue();

			int[] referenceValues = (int[]) references.get(i);
			int referenceJobID = referenceValues[0];
			int referenceStart = referenceValues[1];
			int referenceStop = referenceValues[2];

			// checking jobids
			assertFalse("The TaskIDs did not match. I was expecting '" + referenceJobID + "' but it was '" + reportJobId + "'", referenceJobID != referenceJobID);

			// checking starttime
			assertFalse("TaskID[" + reportJobId + "] - start time was '" + reportStart + "' but should be '" + referenceStart + "' (+- '" + tolerance + "')",
					reportStart - referenceStart > tolerance || reportStart - referenceStart < ((-1) * tolerance));

			// checking stoptime
			assertFalse("TaskID[" + reportJobId + "] - stop time was '" + reportStop + "' but should be '" + referenceStop + "' (+- '" + tolerance + "')",
					reportStop - referenceStop > tolerance || reportStop - referenceStop < ((-1) * tolerance));
			i++;
		}

	}

}

class DatabaseTaskUtils {

	SQLUtils sqlUtils;
	public static NodeLogger logger = NodeLogger.getNodeLogger(DatabaseTaskUtils.class);

	public DatabaseTaskUtils(Properties conf) throws Exception {
		sqlUtils = SQLUtilsFactory.getSQLUtils(conf);
	}

	/**
	 * This method is used to activates jobs. This is useful to enable restart of interval task.
	 * 
	 * @param id
	 */
	public void activateJob(int id) {
		String sql = "UPDATE job SET status = ? WHERE id = " + id;
		ArrayList keys = new ArrayList();

		keys.add("1");
		try {
			sqlUtils.updateRecord(sql, keys);
		} catch (SQLException e) {
			logger.error("error while updating", e);
		}
	}

	/**
	 * This method is used to deactivates jobs. This is useful to disable restart of interval task.
	 * 
	 * @param id
	 */
	public void deactivateJob(int id) {
		String sql = "UPDATE job SET status = ? WHERE id = " + id;
		ArrayList keys = new ArrayList();

		keys.add("0");
		try {
			sqlUtils.updateRecord(sql, keys);
		} catch (SQLException e) {
			logger.error("error while updating", e);
		}
	}

	/**
	 * This method is used to insert the usecases (sql files) into the desired database
	 * 
	 * @param nCase
	 * @return
	 */
	public boolean insertUsecase(int nCase) {
		try {
			String url = this.getClass().getResource("Usecase" + nCase + "_UB.sql").getFile();

			url = url.replaceAll("%20", " ");

			File usecase = new File(url);

			if (!usecase.exists()) {
				throw new FileNotFoundException();
			}
			SQLDumpUtils dumpUtils = new SQLDumpUtils(sqlUtils);

			dumpUtils.evaluateSQLFile(usecase);
		} catch (Exception e) {
			logger.debug("Error", e);
			return false;
		}
		return true;
	}

	/**
	 * Fetch 'jobrun' records from database. This includes the run times of each performed task.
	 * 
	 * @return
	 */
	public ArrayList getJobRun() {
		ArrayList report = new ArrayList();

		try {
			ResultSet rs = sqlUtils.executeQuery("select * from jobrun order by job_id, starttime, endtime");

			while (rs.next()) {
				ArrayList row = new ArrayList();
				int i = 1;

				while (i < rs.getMetaData().getColumnCount()) {
					row.add(rs.getObject(i));
					i++;
				}
				report.add(row);
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("Error", e);
			return null;
		}
		return report;
	}

	/**
	 * Cleanup the database
	 * 
	 * @return
	 */
	public boolean cleanUsecase() {
		ArrayList sql = new ArrayList();

		sql.add("truncate job");
		sql.add("truncate task");
		sql.add("truncate taskparam");
		sql.add("truncate tasktemplate");
		sql.add("truncate tasktemplateparam");
		sql.add("truncate jobdependency");
		sql.add("truncate jobrun");

		int i = 0;

		while (i < sql.size()) {
			try {
				sqlUtils.executeQueryManipulation(sql.get(i).toString());
			} catch (Exception e) {
				logger.debug("Error", e);
				return false;
			}
			i++;
		}
		return true;

	}

}
