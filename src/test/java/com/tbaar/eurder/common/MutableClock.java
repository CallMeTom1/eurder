package com.tbaar.eurder.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(ZonedDateTime dateTime) {
        this(dateTime.toInstant(), dateTime.getZone());
    }

    private MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setTo(ZonedDateTime dateTime) {
        this.instant = dateTime.toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
