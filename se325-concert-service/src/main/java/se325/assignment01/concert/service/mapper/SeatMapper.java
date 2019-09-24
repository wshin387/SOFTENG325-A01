package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Seat;

public class SeatMapper {
    public static SeatDTO toDTO(Seat seat){
        return new SeatDTO(seat.getLabel(), seat.getPrice());
    }
}
