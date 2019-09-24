package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.common.dto.ConcertSummaryDTO;
import se325.assignment01.concert.service.domain.Concert;
import sun.misc.Perf;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConcertMapper {
    public static ConcertDTO toDTO(Concert concert){
        return new ConcertDTO(concert.getId(),concert.getTitle(),concert.getImage(),concert.getBlurb(),
                new ArrayList<LocalDateTime>(concert.getDates()), PerformerMapper.setToDTO(concert.getPerformers()));
    }

    public static List<ConcertDTO> listToDTO(List<Concert> concerts) {
        List<ConcertDTO> dtoList = new ArrayList<>();
        for (Concert c : concerts) {
            dtoList.add(ConcertMapper.toDTO(c));
        }
        return dtoList;
    }

    public static ConcertSummaryDTO toSummaryDTO(Concert concert){
        return new ConcertSummaryDTO(concert.getId(),concert.getTitle(),concert.getImage());
    }

    public static List<ConcertSummaryDTO> listToSummaryDTO(List<Concert> concerts){
        List<ConcertSummaryDTO> dtoList = new ArrayList<>();
        for (Concert c: concerts){
            dtoList.add(ConcertMapper.toSummaryDTO(c));
        }
        return dtoList;
    }
}
