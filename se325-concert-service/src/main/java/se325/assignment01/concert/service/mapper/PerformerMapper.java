package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public static List<PerformerDTO> setToDTO(Set<Performer> performerSet) {
        ArrayList<Performer> performerList = new ArrayList<>(performerSet);
        return PerformerMapper.listToDTO(performerList);
    }
}