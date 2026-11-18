package com.zaheenmunshi.ecommerce.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** The real clock, shifted by an adjustable offset, so tests can "wait" without sleeping. */
public final class MutableClock extends Clock {

	private volatile Duration offset = Duration.ZERO;

	public void advance(Duration amount) {
		offset = offset.plus(amount);
	}

	public void reset() {
		offset = Duration.ZERO;
	}

	@Override
	public Instant instant() {
		return Instant.now().plus(offset);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

}
