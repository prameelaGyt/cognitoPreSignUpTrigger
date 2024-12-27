package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreSignUpEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CognitoPreSignUpHandler implements RequestHandler<CognitoUserPoolPreSignUpEvent, CognitoUserPoolPreSignUpEvent> {

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private final CryptographyService cryptographyService = new CryptographyService();

    @Override
    public CognitoUserPoolPreSignUpEvent handleRequest(CognitoUserPoolPreSignUpEvent event, Context context) {
        String email = event.getRequest().getUserAttributes().get("email");
        context.getLogger().log("Checking if user with email exists: " + email);

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            // Query to check if the email exists in the database
            String sql = "SELECT account_type FROM users WHERE email = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, cryptographyService.encrypt(email));
                try (ResultSet resultSet = stmt.executeQuery()) {
                    if (resultSet.next()) {
                        String existingAccountType = resultSet.getString("account_type");

                        // Determine the account type being attempted
                        String attemptedAccountType = determineAccountType(event.getUserName());
                        if (!existingAccountType.equalsIgnoreCase(attemptedAccountType)) {
                            // Log and throw an exception to block the signup
                            String message = getCustomErrorMessage(existingAccountType);
                            context.getLogger().log(message);
                            throw new RuntimeException(message);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            context.getLogger().log("Database error: " + e.getMessage());
            throw new RuntimeException("Error checking user email. Please try again later.");
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        // Adjust auto-confirm and auto-verify settings
        // If you want to require email verification, do not auto-confirm or auto-verify
        event.getResponse().setAutoConfirmUser(false); // Ensure user is not auto-confirmed
        event.getResponse().setAutoVerifyEmail(false); // Ensure email verification is required

        return event;
    }

    // Determine account type based on username
    private String determineAccountType(String username) {
        if (username.startsWith("google_")) {
            return "google";
        } else if (username.startsWith("facebook_")) {
            return "facebook";
        } else {
            return "lawnMart";
        }
    }

    // Get custom error message based on existing account type
    private String getCustomErrorMessage(String accountType) {
        switch (accountType) {
            case "google":
                return "An account already exists with Google. Please log in with Google.";
            case "facebook":
                return "An account already exists with Facebook. Please log in with Facebook.";
            default:
                return "An account already exists with your email. Please log in using your app credentials.";
        }
    }
}
