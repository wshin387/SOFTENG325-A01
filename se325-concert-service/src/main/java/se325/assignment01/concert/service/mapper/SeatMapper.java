package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Seat;

import java.util.ArrayList;
import java.util.List;

public class SeatMapper {
    public static SeatDTO toDTO(Seat seat){
        return new SeatDTO(seat.getLabel(), seat.getPrice());
    }

    public static List<SeatDTO> listToDTO(List<Seat> seatList) {
        List<SeatDTO> result = new ArrayList<>();

        for (Seat s: seatList) {
            result.add(SeatMapper.toDTO(s));
        }

        return result;
    }
}
