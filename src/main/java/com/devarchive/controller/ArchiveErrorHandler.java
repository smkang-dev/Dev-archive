package com.devarchive.controller;

import com.devarchive.service.StaleRevisionException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ArchiveErrorHandler {
  @ExceptionHandler({IllegalArgumentException.class, StaleRevisionException.class})
  public Object handle(RuntimeException error, HttpServletRequest request) {
    int status = error instanceof StaleRevisionException ? 409 : 400;
    if (request.getRequestURI().endsWith("/layout/reorder"))
      return ResponseEntity.status(status).body(Map.of("message", error.getMessage()));
    ModelAndView view = new ModelAndView("request-error");
    view.setStatus(org.springframework.http.HttpStatus.valueOf(status));
    view.addObject("message", error.getMessage());
    return view;
  }
}
