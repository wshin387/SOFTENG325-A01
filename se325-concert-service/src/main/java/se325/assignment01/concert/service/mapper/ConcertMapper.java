package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.common.dto.ConcertSummaryDTO;
import se325.assignment01.concert.service.domain.Concert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConcertMapper {
    public static ConcertDTO toDTO(Concert concert){
        ConcertDTO result = new ConcertDTO(concert.getId(),concert.getTitle(),concert.getImage(),concert.getBlurb());
        result.setDates(new ArrayList<LocalDateTime>(concert.getDates()));
        result.setPerformers(PerformerMapper.setToDTO(concert.getPerformers()));
        return result;
    }

    public static List<ConcertDTO> listToDTO(List<Concert> concertList) {
        List<ConcertDTO> dtoList = new ArrayList<>();
        for (Concert c : concertList) {
            dtoList.add(ConcertMapper.toDTO(c));
        }
        return dtoList;
    }

    public static ConcertSummaryDTO toSummaryDTO(Concert concert){
        return new ConcertSummaryDTO(concert.getId(),concert.getTitle(),concert.getImage());
    }

    public static List<ConcertSummaryDTO> listToSummaryDTO(List<Concert> concerts){
        List<ConcertSummaryDTO> result = new ArrayList<>();
        for (Concert c: concerts){
            result.add(ConcertMapper.toSummaryDTO(c));
        }
        return result;
    }
}
