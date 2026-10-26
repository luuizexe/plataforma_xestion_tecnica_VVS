package gal.udc.fic.prperez.pleste.service.exceptions.authentication;

import gal.udc.fic.prperez.pleste.service.exceptions.RESTException;
import jakarta.ws.rs.core.Response.Status;

public class UnauthenticatedUserException extends RESTException {
	public UnauthenticatedUserException() {
		super("A valid token is required to do that.", Status.UNAUTHORIZED);
	}
}
