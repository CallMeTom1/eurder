package com.tbaar.eurder.common;

import java.util.UUID;

public final class UniqueEmail {

    private UniqueEmail() {
    }

    public static String of(String name) {
        return name + "." + UUID.randomUUID() + "@eurder.test";
    }
}
