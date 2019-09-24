package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;

import java.util.ArrayList;
import java.util.List;

public class PerformerMapper {
    public static PerformerDTO toDTO(Performer performer){
        return new PerformerDTO(performer.getId(), performer.getName(), performer.getImage(), performer.getGenre(), performer.getBlurb());
    }

    public static List<PerformerDTO> listToDTO(List<Performer> performers) {
        List<PerformerDTO> dtoList = new ArrayList<>();

        for (Performer p: performers){
            dtoList.add(PerformerMapper.toDTO(p));
        }
        return dtoList;
    }
}