package com.devarchive.controller;

import com.devarchive.service.LayoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LayoutController {

    private final LayoutService layoutService;

    public LayoutController(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @PostMapping("/layout/reorder")
    public ResponseEntity<Void> reorder(
            @RequestBody LayoutOrderRequest request
    ) {
        layoutService.reorder(request);
        return ResponseEntity.ok().build();
    }
}