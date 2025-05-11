package com.budgetapp.thrifty.handlers;

import com.budgetapp.thrifty.account.Account;
import java.util.ArrayList;
import java.util.List;

public class AccountsHandler {
    // Static list to store accounts (in a real app, this would be a database)
    private static final List<Account> accounts = new ArrayList<>();

    // Add the default test account
    static {
        accounts.add(new Account("Test", "User", "test@example.com", "password123"));
    }

    // Add a new account
    public static void addAccount(Account account) {
        accounts.add(account);
    }

    // Check if an account with the given email already exists
    public static boolean emailExists(String email) {
        for (Account account : accounts) {
            if (account.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    // Authenticate a user
    public static Account authenticate(String email, String password) {
        for (Account account : accounts) {
            if (account.getEmail().equalsIgnoreCase(email) &&
                    account.getPassword().equals(password)) {
                return account;
            }
        }
        return null;
    }

    // Get all accounts (for debugging)
    public static List<Account> getAllAccounts() {
        return new ArrayList<>(accounts);
    }
}
