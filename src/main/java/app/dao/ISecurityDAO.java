package app.dao;


import app.entities.Role;
import app.entities.User;
import app.exceptions.ValidationException;

public interface ISecurityDAO {
    User getVerifiedUser(String username, String password) throws ValidationException; // used for login
    User createUser(String username, String password) throws ValidationException; // used for register
    Role createRole(String role);
    User addUserRole(String username, String role);
    void changePassword(String email, String currentPassword, String newPassword) throws ValidationException;
}
