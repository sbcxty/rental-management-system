package com.gudosnest.backend.service;

import com.gudosnest.backend.model.User;
import com.gudosnest.backend.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

/** Sign up and log in. Passwords are stored only as jBCrypt hashes. */
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String PASSWORD_HINT = MIN_PASSWORD_LENGTH + "+ characters with a letter, number and symbol";
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
        if (!isStrongEnough(password)) {
            throw new RuleException("Password must contain at least one letter, one number and one symbol.");
        }
        if (!owner && !renter) {
            throw new RuleException("Choose at least one account type: Renter, Owner, or both.");
        }
        if (users.emailExists(cleanEmail)) {
            throw new RuleException("An account with this email already exists. Log in instead.");
        }
        return users.insert(name, cleanEmail, BCrypt.hashpw(password, BCrypt.gensalt()), owner, renter);
    }

    /** True when the password has at least one letter, one digit and one symbol (anything else, not a space). */
    static boolean isStrongEnough(String password) {
        boolean letter = false, digit = false, symbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                letter = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isWhitespace(c)) {
                symbol = true;
            }
        }
        return letter && digit && symbol;
    }
}
