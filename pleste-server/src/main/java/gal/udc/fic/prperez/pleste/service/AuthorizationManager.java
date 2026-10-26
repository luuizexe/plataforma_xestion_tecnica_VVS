package gal.udc.fic.prperez.pleste.service;

import gal.udc.fic.prperez.pleste.service.dao.users.Roles;
import gal.udc.fic.prperez.pleste.service.dao.users.SQLTokenDao;
import gal.udc.fic.prperez.pleste.service.dao.users.User;
import gal.udc.fic.prperez.pleste.service.exceptions.authentication.IllegalActionForUserException;
import gal.udc.fic.prperez.pleste.service.exceptions.authentication.UnauthenticatedUserException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationManager implements ContainerRequestFilter {
	private final SQLTokenDao tokenDatabase;

	public @Autowired AuthorizationManager(SQLTokenDao tokenDatabase) {
		this.tokenDatabase = tokenDatabase;
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IllegalActionForUserException, UnauthenticatedUserException {
		String token, path, method;
		User user;

		token = requestContext.getHeaderString("X-API-KEY");
		path = requestContext.getUriInfo().getPath();
		method = requestContext.getMethod();

		if( token == null || token.isEmpty() ) {
			if(!(path.matches("users/login") || path.matches("openapi.json"))) {
				throw new UnauthenticatedUserException();
			}
		} else {
			if (tokenDatabase.existsByToken(token)) {
				user = tokenDatabase.getByToken(token).getUser();
				// Un usuario sen rol trátase coma un usuario normal
				boolean isAdministrator = Roles.ADMINISTRATOR.equals(user.getRole());
				if (path.matches("templates/?.*")) {
					if(isAdministrator || method.equals("GET")) {
						return;
					} else {
						throw new IllegalActionForUserException(user.getUsername());
					}
				}
				if (path.matches("users/?.*")) {
					if( !isAdministrator && !method.equals("GET") && !(method.equals("POST") && path.matches(".*/logout$"))) {
						throw new IllegalActionForUserException(user.getUsername());
					}
				}
			} else {
				throw new UnauthenticatedUserException();
			}
		}
	}
}
