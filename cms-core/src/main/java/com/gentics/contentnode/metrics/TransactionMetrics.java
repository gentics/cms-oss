package com.gentics.contentnode.metrics;

import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.jmx.TransactionInfoMBean;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Meters for the transactions, read from the {@link TransactionInfoMBean}
 */
public class TransactionMetrics implements MeterBinder {
	@Override
	public void bindTo(MeterRegistry registry) {
		TransactionManager.getTransactionInfo().ifPresent(info -> {
			FunctionCounter.builder("cms.transactions", info, TransactionInfoMBean::getTotal)
				.description("Number of started transactions")
				.register(registry);
			Gauge.builder("cms.transactions.open", info, TransactionInfoMBean::getOpen)
				.description("Number of currently open transactions")
				.register(registry);
			Gauge.builder("cms.transactions.open.max", info, TransactionInfoMBean::getMaxOpen)
				.description("Maximum number of open transactions since the start")
				.register(registry);
		});
	}
}
