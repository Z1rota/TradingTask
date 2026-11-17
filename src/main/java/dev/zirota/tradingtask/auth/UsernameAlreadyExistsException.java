package dev.zirota.tradingtask.auth;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("Username is already registered");
    }
}
