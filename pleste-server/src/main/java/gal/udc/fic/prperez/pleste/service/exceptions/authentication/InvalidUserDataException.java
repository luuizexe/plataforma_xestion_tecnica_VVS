package gal.udc.fic.prperez.pleste.service.exceptions.authentication;

import gal.udc.fic.prperez.pleste.service.exceptions.RESTException;
import jakarta.ws.rs.core.Response.Status;

public class InvalidUserDataException extends RESTException {
	public InvalidUserDataException(String field) {
		super("The " + field + " of the user is missing or empty", Status.BAD_REQUEST);
	}
}
