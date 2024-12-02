package dev.cat6

import dev.cat6.aemo.NemParseException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status.BAD_REQUEST
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class NemParserExceptionMapper : ExceptionMapper<NemParseException> {
  private data class NamParseExceptionResponse(val reason: String?)

  override fun toResponse(exception: NemParseException): Response? {
    return Response.status(BAD_REQUEST).entity(NamParseExceptionResponse(exception.message)).build()
  }
}
