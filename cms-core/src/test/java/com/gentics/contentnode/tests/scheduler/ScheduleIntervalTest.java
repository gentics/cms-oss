package com.gentics.contentnode.tests.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.rest.model.scheduler.IntervalUnit;
import com.gentics.contentnode.rest.model.scheduler.ScheduleInterval;

/**
 * Test cases for schedule intervals ("due" calculation based on start time, time of last execution and now)
 */
@RunWith(value = Parameterized.class)
public class ScheduleIntervalTest {
	/**
	 * date format for test data input
	 */
	protected final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

	/**
	 * Parse the formatted date into a calendar instance
	 * @param formatted formatted date
	 * @return calendar instance
	 * @throws ParseException
	 */
	protected final static Calendar getCalendar(String formatted) throws ParseException {
		Calendar cal = Calendar.getInstance();
		if (StringUtils.isEmpty(formatted)) {
			cal.setTimeInMillis(0);
		} else {
			Date date = format.parse(formatted);
			cal.setTimeInMillis(date.getTime());
		}
		return cal;
	}

	/**
	 * Parse the formatted date into unix timestamp (in seconds)
	 * @param formatted formatted date
	 * @return unix timestamp (in seconds)
	 * @throws ParseException
	 */
	protected final static int getTimestamp(String formatted) throws ParseException {
		if (StringUtils.isEmpty(formatted)) {
			return 0;
		} else {
			return (int) (getCalendar(formatted).getTimeInMillis() / 1000L);
		}
	}

	/**
	 * Get test variations
	 * @return
	 */
	@Parameters(name = "{index}: start {0}, interval {1}, last execution {2}, now {3}, expect due {4}")
	public static Collection<Object[]> data() {
		// transition from CEST to CET is: 2023-10-29 03:00:00 CEST -> 2023-10-29 02:00:00 CET
		// transition from CET to CEST is: 2024-03-31 02:00:00 CET  -> 2024-31-03 03:00:00 CEST

		Collection<Object[]> data = new ArrayList<>();

		// every 5 minutes
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute), "2023-10-29 02:03:00 CEST", "2023-10-29 02:04:00 CEST", false});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute), "2023-10-29 02:03:00 CEST", "2023-10-29 02:05:00 CEST", true});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute), "2023-10-29 02:03:00 CEST", "2023-10-29 02:06:00 CEST", true});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute), "2023-10-29 02:05:00 CEST", "2023-10-29 02:06:00 CET", true});
		data.add(new Object[] {"2003-01-01 00:00:00 CET", new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute), "2023-12-12 07:18:00 CET", "2023-12-12 07:20:00 CET", true});
		data.add(new Object[] {"", new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute), "2023-12-12 07:18:00 CET", "2023-12-12 07:20:00 CET", true});

		// every 45 minutes
		data.add(new Object[] {"2023-10-29 01:45:00 CEST", new ScheduleInterval().setValue(45).setUnit(IntervalUnit.minute), "2023-10-29 01:45:00 CEST", "2023-10-29 02:29:00 CEST", false});
		data.add(new Object[] {"2023-10-29 01:45:00 CEST", new ScheduleInterval().setValue(45).setUnit(IntervalUnit.minute), "2023-10-29 01:45:00 CEST", "2023-10-29 02:30:00 CEST", true});
		data.add(new Object[] {"2023-10-29 01:45:00 CEST", new ScheduleInterval().setValue(45).setUnit(IntervalUnit.minute), "2023-10-29 02:30:00 CEST", "2023-10-29 02:14:00 CET", false});
		data.add(new Object[] {"2023-10-29 01:45:00 CEST", new ScheduleInterval().setValue(45).setUnit(IntervalUnit.minute), "2023-10-29 02:30:00 CEST", "2023-10-29 02:15:00 CET", true});

		// every hour
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.hour), "2023-10-27 07:30:00 CEST", "2023-10-27 08:00:00 CEST", true});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.hour), "2024-03-31 02:00:00 CET", "2024-03-31 03:00:00 CEST", false});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.hour), "2024-03-31 02:00:00 CET", "2024-03-31 04:00:00 CEST", true});

		// every day
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2023-10-28 06:00:00 CEST", "2023-10-29 05:00:00 CET", false});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2023-10-28 06:00:00 CEST", "2023-10-29 06:00:00 CET", true});

		data.add(new Object[] {"2023-10-28 02:30:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2023-10-28 02:30:00 CEST", "2023-10-29 02:30:00 CEST", true});
		data.add(new Object[] {"2023-10-28 02:30:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2023-10-29 02:30:00 CEST", "2023-10-29 02:30:00 CET", false});
		data.add(new Object[] {"2023-10-28 02:30:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2023-10-29 02:30:00 CEST", "2023-10-30 02:30:00 CET", true});

		data.add(new Object[] {"2024-03-30 02:30:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2024-03-30 02:30:00 CET", "2024-03-31 03:00:00 CEST", false});
		data.add(new Object[] {"2024-03-30 02:30:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2024-03-30 02:30:00 CET", "2024-03-31 03:29:00 CEST", false});
		data.add(new Object[] {"2024-03-30 02:30:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2024-03-30 02:30:00 CET", "2024-03-31 03:30:00 CEST", true});
		data.add(new Object[] {"2024-03-30 02:30:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2024-03-31 03:30:00 CEST", "2024-04-01 02:29:00 CEST", false});
		data.add(new Object[] {"2024-03-30 02:30:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.day), "2024-03-31 03:30:00 CEST", "2024-04-01 02:30:00 CEST", true});

		// every 2 days
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(2).setUnit(IntervalUnit.day), "2023-10-28 06:00:00 CEST", "2023-10-29 05:00:00 CET", false});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(2).setUnit(IntervalUnit.day), "2023-10-28 06:00:00 CEST", "2023-10-29 06:00:00 CET", true});

		// every week
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.week), "2023-10-27 06:00:00 CEST", "2023-11-03 05:00:00 CET", false});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.week), "2023-10-27 06:00:00 CEST", "2023-11-03 06:00:00 CET", true});
		data.add(new Object[] {"2023-10-27 06:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.week), "2023-10-28 06:00:00 CEST", "2023-11-03 06:00:00 CET", true});

		// every month
		data.add(new Object[] {"2023-10-27 01:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.month), "2023-10-27 01:00:00 CEST", "2023-11-27 00:00:00 CET", false});
		data.add(new Object[] {"2023-10-27 01:00:00 CEST", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.month), "2023-10-27 01:00:00 CEST", "2023-11-27 01:00:00 CET", true});
		data.add(new Object[] {"2023-10-31 00:00:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.month), "2023-10-31 00:00:00 CET", "2023-11-30 00:00:00 CET", true});
		data.add(new Object[] {"2023-10-31 00:00:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.month), "2023-11-30 00:00:00 CET", "2023-12-30 00:00:00 CET", false});
		data.add(new Object[] {"2023-10-31 00:00:00 CET", new ScheduleInterval().setValue(1).setUnit(IntervalUnit.month), "2023-11-30 00:00:00 CET", "2023-12-31 00:00:00 CET", true});

		return data;
	}

	/**
	 * Formatted start date
	 */
	@Parameter(0)
	public String start;

	/**
	 * Schedule interval
	 */
	@Parameter(1)
	public ScheduleInterval interval;

	/**
	 * Formatted date of last execution
	 */
	@Parameter(2)
	public String lastExecution;

	/**
	 * Formatted date of "now"
	 */
	@Parameter(3)
	public String now;

	/**
	 * Whether schedule is expected to be "due"
	 */
	@Parameter(4)
	public boolean expectDue;

	/**
	 * Test "due" calculation
	 * @throws ParseException
	 */
	@Test
	public void testDue() throws ParseException {
		assertThat(interval.isDue(getTimestamp(start), getTimestamp(lastExecution), getTimestamp(now)))
				.as(String.format("start: %s, last: %s, int: %s %s at %s", getCalendar(start).getTime(),
						getCalendar(lastExecution).getTime(), interval, expectDue ? "is due" : "is not due",
						getCalendar(now).getTime()))
				.isEqualTo(expectDue);
	}
}
