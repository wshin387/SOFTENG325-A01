package se325.assignment01.concert.service.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Seat {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String label;
	private boolean isBooked;
	private LocalDateTime date;
	private BigDecimal price;

	public Seat() {}

	public Seat(String label, boolean isBooked, LocalDateTime date, BigDecimal price) {
		this.label = label;
		this.isBooked = isBooked;
		this.date = date;
		this.price = price;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setBooked(boolean booked) {
		isBooked = booked;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public boolean isBooked() {
		return isBooked;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public BigDecimal getPrice() {
		return price;
	}

}
