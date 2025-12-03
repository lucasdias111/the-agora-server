package org.margin.server.social.services;

import org.springframework.stereotype.Service;
import org.margin.server.social.models.Space;
import org.margin.server.social.repositories.SpacesRepository;

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
