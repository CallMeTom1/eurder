package com.tbaar.eurder.feature.customer.domain;

import com.tbaar.eurder.feature.security.domain.Credentials;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "customers")
public class Customer {

    @Id
    private UUID id = UUID.randomUUID();

    @Indexed(unique = true)
    private UUID credentialsId;

    private String email;

    private String firstName;

    private String lastName;

    private Address address;

    private String phoneNumber;

    protected Customer() {
    }

    public Customer(Credentials credentials, String firstName, String lastName, Address address, String phoneNumber) {
        this.credentialsId = credentials.getId();
        this.email = credentials.getEmail();
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCredentialsId() {
        return credentialsId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Address getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
