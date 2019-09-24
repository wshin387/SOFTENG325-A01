package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Concert;

public class BookingMapper {

    static Booking toDomainModel(BookingDTO dtoBooking) {
        Booking fullBooking = new Booking();
        return fullBooking;
    }

    public static BookingDTO toDTO(Concert concert){
        return new ConcertDTO(concert.getId(),concert.getTitle(),concert.getImage(),concert.getBlurb());
    }

}
