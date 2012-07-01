/*
A Java-based HTTP stub server

Copyright (C) 2012 Alexander Zagniotov, Isa Goksu and Eric Mrak

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.stubby.handlers;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.stubby.database.Repository;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Zagniotov
 * @since 6/17/12, 11:25 PM
 */
public final class ClientHandler extends AbstractHandler {

   protected static final String BAD_POST_REQUEST_MESSAGE = "Oh oh :( Bad request, POST body is missing";
   private Repository repository;

   public ClientHandler(final Repository repository) {
      this.repository = repository;
   }

   @Override
   public void handle(final String target,
                      final Request baseRequest,
                      final HttpServletRequest request,
                      final HttpServletResponse response) throws IOException, ServletException {

      baseRequest.setHandled(true);
      setResponseMainHeaders(response);

      String postBody = null;
      if (request.getMethod().toLowerCase().equals("post")) {

         try {
            postBody = HandlerHelper.inputStreamToString(request.getInputStream());
            if (postBody == null || postBody.isEmpty()) {
               response.setContentType(MimeTypes.TEXT_PLAIN_UTF_8);
               response.setStatus(HttpStatus.BAD_REQUEST_400);
               response.sendError(HttpStatus.BAD_REQUEST_400, BAD_POST_REQUEST_MESSAGE);
               return;
            }
         } catch (Exception ex) {
            response.setContentType(MimeTypes.TEXT_PLAIN_UTF_8);
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            response.sendError(HttpStatus.BAD_REQUEST_400, BAD_POST_REQUEST_MESSAGE);
            return;
         }
      }

      Map<String, String> responseBody = repository.retrieveResponseFor(constructFullURI(request), request.getMethod(), postBody);
      if (responseBody.size() == 1) {
         response.setStatus(HttpStatus.NOT_FOUND_404);
         response.sendError(HttpStatus.NOT_FOUND_404, responseBody.get(Repository.NOCONTENT_MSG_KEY));
         return;
      }

      setStubResponseHeaders(responseBody, response);
      response.setStatus(Integer.parseInt(responseBody.get(Repository.TBL_COLUMN_STATUS)));
      response.getWriter().println(responseBody.get(Repository.TBL_COLUMN_BODY));
   }

   private String constructFullURI(final HttpServletRequest request) {
      final String pathInfo = request.getPathInfo();
      final String queryStr = request.getQueryString();
      final String queryString = (queryStr == null || queryStr.equals("")) ? "" : String.format("?%s", request.getQueryString());
      return String.format("%s%s", pathInfo, queryString);
   }

   private void setResponseMainHeaders(final HttpServletResponse response) {
      response.setHeader(HttpHeaders.SERVER, HandlerHelper.constructHeaderServerName());
      response.setHeader(HttpHeaders.DATE, new Date().toString());
      response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"); // HTTP 1.1.
      response.setHeader(HttpHeaders.PRAGMA, "no-cache"); // HTTP 1.0.
      response.setDateHeader(HttpHeaders.EXPIRES, 0);
   }

   private void setStubResponseHeaders(final Map<String, String> responseBody, final HttpServletResponse response) {

      final List<String> nonHeaderProperties = Arrays.asList(
            Repository.TBL_COLUMN_BODY,
            Repository.NOCONTENT_MSG_KEY,
            Repository.TBL_COLUMN_STATUS);

      response.setCharacterEncoding("UTF-8");

      for (Map.Entry<String, String> entry : responseBody.entrySet()) {
         if (nonHeaderProperties.contains(entry.getKey())) {
            continue;
         }
         response.setHeader(entry.getKey(), entry.getValue());
      }
   }
}