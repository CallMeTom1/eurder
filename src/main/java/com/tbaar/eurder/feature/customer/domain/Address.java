package com.tbaar.eurder.feature.customer.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Address {

    private String street;

    private String houseNumber;

    private String postalCode;

    private String city;

    @Enumerated(EnumType.STRING)
    private Country country;

    protected Address() {
    }

    public Address(String street, String houseNumber, String postalCode, String city, Country country) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public Country getCountry() {
        return country;
    }
}
