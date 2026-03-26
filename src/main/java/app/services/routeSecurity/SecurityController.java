package app.services.routeSecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import app.exceptions.TokenVerificationException;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import app.config.HibernateConfig;
import jakarta.persistence.EntityManagerFactory;
import app.dao.ISecurityDAO;
import app.dao.UserDAO;
import app.entities.User;
import app.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;

import java.text.ParseException;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController implements ISecurityController{
    private ISecurityDAO userDAO;

    public SecurityController() {
        this.userDAO = new UserDAO(HibernateConfig.getEntityManagerFactory());
    }

    public SecurityController(EntityManagerFactory emf) {
        this.userDAO = new UserDAO(emf);
    }
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ITokenSecurity tokenSecurity = new TokenSecurity();


    @Override
    public void register(Context ctx) {
        UserDTO user = ctx.bodyAsClass(UserDTO.class);
        userDAO.createUser(user.getEmail(), user.getPassword());
        ObjectNode node = objectMapper.createObjectNode();
        node.put("msg", "loginregist er success");
        ctx.json(node).status(201);
    }
    @Override
    public void login(Context ctx) {
        UserDTO user = ctx.bodyAsClass(UserDTO.class);
        try{
            User userEntity = userDAO.getVerifiedUser(user.getEmail(), user.getPassword());
            String token = createToken(new UserDTO(userEntity.getEmail(),userEntity.getRolesAsStrings()));
            ObjectNode node =objectMapper.createObjectNode();

            ctx.status(200).json(node
                    .put("token", token)
                    .put("username", userEntity.getEmail()));


        }catch (ValidationException e){
            throw new ApiException(401,e.getMessage());
        }

    }




    private String createToken(UserDTO user) {
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = Utils.getPropertyValue("ISSUER", "config.properties");
                TOKEN_EXPIRE_TIME = Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties");
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            }
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch (Exception e) {
//            logger.error("Could not create token", e);
            throw new ApiException(500, "Could not create token");
        }
    }
    @Override
    public void authenticate(Context ctx) {
        // This is a preflight request => no need for authentication
        if (ctx.method().toString().equals("OPTIONS")) {
            ctx.status(200);
            return;
        }
        // If the endpoint is not protected with roles or is open to ANYONE role, then skip
        Set<String> allowedRoles = ctx.routeRoles().stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
        if (isOpenEndpoint(allowedRoles))
            return;

        // If there is no token we do not allow entry
        UserDTO verifiedTokenUser = validateAndGetUserFromToken(ctx);
        ctx.attribute("user", verifiedTokenUser); // -> ctx.attribute("user") in ApplicationConfig beforeMatched filter
    }

    @Override
    public void authorize(Context ctx) {
        Set<String> allowedRoles = ctx.routeRoles()
                .stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());

        // 1. Check if the endpoint is open to all (either by not having any roles or having the ANYONE role set
        if (isOpenEndpoint(allowedRoles))
            return;
        // 2. Get user and ensure it is not null
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ForbiddenResponse("No user was added from the token");
        }
        // 3. See if any role matches
        if (!userHasAllowedRole(user, allowedRoles))
            throw new ForbiddenResponse("User was not authorized with roles: " + user.getRoles() + ". Needed roles are: " + allowedRoles);
    }


    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }


    private static String getToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null) {
            throw new UnauthorizedResponse("Authorization header is missing"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }

        // If the Authorization Header was malformed, then no entry
        String token = header.split(" ")[1];
        if (token == null) {
            throw new UnauthorizedResponse("Authorization header is malformed"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }
        return token;
    }



    private boolean isOpenEndpoint(Set<String> allowedRoles) {
        // If the endpoint is not protected with any roles:
        if (allowedRoles.isEmpty())
            return true;

        // 1. Get permitted roles and Check if the endpoint is open to all with the ANYONE role
        if (allowedRoles.contains("ANYONE")) {
            return true;
        }
        return false;
    }
    private UserDTO validateAndGetUserFromToken(Context ctx) {
        String token = getToken(ctx);
        UserDTO verifiedTokenUser = verifyToken(token);
        if (verifiedTokenUser == null) {
            throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }
        return verifiedTokenUser;
    }
    private UserDTO verifyToken(String token) {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : Utils.getPropertyValue("SECRET_KEY", "config.properties");

        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                throw new ApiException(403, "Token is not valid");
            }
        } catch (ParseException | ApiException e) {
//            logger.error("Could not create token", e);
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        } catch (TokenVerificationException e) {
            throw new RuntimeException(e);
        }
    }
}
