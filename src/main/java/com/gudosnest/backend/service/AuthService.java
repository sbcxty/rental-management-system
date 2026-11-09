package com.gudosnest.backend.service;

import com.gudosnest.backend.model.User;
import com.gudosnest.backend.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

/** Sign up and log in. Passwords are stored only as jBCrypt hashes. */
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users = new UserRepository();

    public User logIn(String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            throw new RuleException("Enter your email address and password.");
        }
        UserRepository.Credentials found = users.findCredentials(cleanEmail)
                .orElseThrow(() -> new RuleException("That email and password don't match an account."));
        if (!BCrypt.checkpw(password, found.passwordHash())) {
            throw new RuleException("That email and password don't match an account.");
        }
        if (!found.active()) {
            throw new RuleException("This account is suspended.");
        }
        return found.user();
    }

    public User signUp(String fullName, String email, String password, boolean owner, boolean renter) {
        String name = fullName.trim().replaceAll("\\s+", " ");
        String cleanEmail = email.trim().toLowerCase();
        if (name.isEmpty()) {
            throw new RuleException("Enter your full name.");
        }
        if (name.length() > 100) {
            throw new RuleException("Full name must be 100 characters or fewer.");
        }
        if (!EMAIL.matcher(cleanEmail).matches()) {
            throw new RuleException("Enter a valid email address, for example name@example.com.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new RuleException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!owner && !renter) {
            throw new RuleException("Choose at least one account type: Renter, Owner, or both.");
        }
        if (users.emailExists(cleanEmail)) {
            throw new RuleException("An account with this email already exists. Log in instead.");
        }
        return users.insert(name, cleanEmail, BCrypt.hashpw(password, BCrypt.gensalt()), owner, renter);
    }
}
