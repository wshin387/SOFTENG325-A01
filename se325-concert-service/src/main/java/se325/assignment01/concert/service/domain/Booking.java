package se325.assignment01.concert.service.domain;

import javax.persistence.*;
import java.util.List;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private List<Seat> seatList;

    @ElementCollection
    private List<String> userIdList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Seat> getSeats() {
        return seatList;
    }

    public void setSeats(List<Seat> seatList) {
        this.seatList = seatList;
    }

    public List<Seat> getSeatList() {
        return seatList;
    }

    public void setSeatList(List<Seat> seatList) {
        this.seatList = seatList;
    }

    public List<String> getUserIdList() {
        return userIdList;
    }

    public void setUserIdList(List<String> userIdList) {
        this.userIdList = userIdList;
    }

}
