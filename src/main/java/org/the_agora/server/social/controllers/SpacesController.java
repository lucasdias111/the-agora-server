package org.the_agora.server.social.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.the_agora.server.social.models.Space;
import org.the_agora.server.social.services.SpacesService;

import java.util.List;

@RestController
@RequestMapping("/spaces")
public class SpacesController {
    private final SpacesService spacesService;

    public SpacesController(SpacesService spacesService) {
        this.spacesService = spacesService;
    }

    public List<Space> getAllSpaces() {
        return spacesService.getSpaces();
    }
}
