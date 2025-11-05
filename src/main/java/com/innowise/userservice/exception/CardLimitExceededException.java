package com.innowise.userservice.exception;

public class CardLimitExceededException extends BussinessException {

    public static final int MAX_CARDS = 5;

    public CardLimitExceededException(Long userId) {
        super(String.format("User with id %d already has %d cards.", userId, MAX_CARDS));
    }
}
