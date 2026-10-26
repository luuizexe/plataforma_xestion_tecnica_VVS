package gal.udc.fic.prperez.pleste.service.exceptions;

import jakarta.ws.rs.core.Response.Status;

public class InvalidPaginationException extends RESTException {
	public InvalidPaginationException(Integer skip, Integer count) {
		super("Invalid pagination: skip must be at least 0 and count at least 1 (skip=" + skip + ", count=" + count + ")", Status.BAD_REQUEST);
	}
}
