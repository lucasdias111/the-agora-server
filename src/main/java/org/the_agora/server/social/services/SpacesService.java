package org.the_agora.server.social.services;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.the_agora.server.social.models.Space;
import org.the_agora.server.social.repositories.SpacesRepository;

import java.util.List;

@Service
public class SpacesService {
    private final SpacesRepository spacesRepository;

    public SpacesService(SpacesRepository spacesRepository) {
        this.spacesRepository = spacesRepository;
    }

    public List<Space> getSpaces() {
        return spacesRepository.getSpaces();
    }
}
