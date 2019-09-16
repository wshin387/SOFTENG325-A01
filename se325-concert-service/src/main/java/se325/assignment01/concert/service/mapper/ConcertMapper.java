package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.service.domain.Concert;

public class ConcertMapper {

    static Concert toDomainModel(ConcertDTO dtoConcert) {
        Concert fullConcert = new Concert(dtoConcert.getId(),
                dtoConcert.getTitle(),
                dtoConcert.getImageName(),
                dtoConcert.getBlurb(),
                dtoConcert.getDates(),
                dtoConcert.getPerformers());

        return fullConcert;
    }

    static ConcertDTO toDTO(Concert concert) {
        ConcertDTO dtoConcert = new ConcertDTO(
                concert.getId(),
                concert.getTitle(),
                concert.getImageName(),
                concert.getBlurb()
        );

        return dtoConcert;
    }

}
