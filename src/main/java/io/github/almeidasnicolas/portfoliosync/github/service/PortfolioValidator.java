package io.github.almeidasnicolas.portfoliosync.github.service;

import io.github.almeidasnicolas.portfoliosync.github.dto.PortfolioFile;
import io.github.almeidasnicolas.portfoliosync.github.exception.InvalidPortfolioFileException;

import org.springframework.stereotype.Component;

@Component
public class PortfolioValidator {

    public void validate(PortfolioFile file){
        if (file.title() == null){
            throw new InvalidPortfolioFileException("Title is required");
        }
        if (file.title().es() == null || file.title().es().isBlank()){
            throw new InvalidPortfolioFileException("Title.es is required");
        }
        if (file.title().en() == null || file.title().en().isBlank()){
            throw new InvalidPortfolioFileException("Title.en is required");
        }
        if (file.description() == null){
            throw new InvalidPortfolioFileException("Description is required");
        }
        if (file.description().es() == null || file.description().es().isBlank()){
            throw new InvalidPortfolioFileException("Description.es is required");
        }
        if (file.description().en() == null || file.description().en().isBlank()){
            throw new InvalidPortfolioFileException("Description.en is required");
        }


    }


}
