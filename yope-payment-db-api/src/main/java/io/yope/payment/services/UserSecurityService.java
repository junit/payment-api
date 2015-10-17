/**
 *
 */
package io.yope.payment.services;

import org.springframework.security.core.userdetails.User;

/**
 * @author mgerardi
 *
 */
public interface UserSecurityService {

    User createUser(String username, String password, String role);

    User getUser(String username);

    User getCurrentUser();

    Boolean deleteUser(String username);


}
