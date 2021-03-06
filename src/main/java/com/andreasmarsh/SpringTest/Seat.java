package com.andreasmarsh.SpringTest;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booked_seats")
public class Seat {


    @Id
    //@GeneratedValue(strategy=GenerationType.AUTO)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long ticketID;

    private Long showID;

    @ManyToOne
    @JoinColumn(name = "seatListID")
    private SeatList seats;


    @Column(name = "seatID")
    private Long seatID;

    @Column(name = "reserved")
    private Long reserved;


    @Column(name = "bookingID")
    private Long bookingID;


    /**
    //The seats for a specific showing will have an associated booking
    //starts as null
    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "booking_bookingID", nullable = true, referencedColumnName = "bookingID")
    @NotFound(action = NotFoundAction.IGNORE)
    private Booking booking;
    */


    public Long getSeatId() {
        return seatID;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    public Seat(){
        showID=0L;
        seatID=0L;
        reserved=0L;
    }

    public boolean isReserved(){
        return UserServices.isReserved(seatID,showID);
    }

    public Long getReserved(){
        return reserved;
    }

    public void setReserved( Long reserved){
        this.reserved = reserved;
    }
    public void setSeatId(Long seatID) {
        this.seatID = seatID;
    }

    public Long getShowId() {
        return showID;
    }

    public void setShowId(Long showID) {
        this.showID = showID;
    }


    public Long getBookingID() {
        return bookingID;
    }

    public void setBookingID(Long bookingID) {
        this.bookingID = bookingID;
    }

    public Seat(Long seatID, Long showID, Long reserved, SeatList seats) {
        this.seatID = seatID;
        this.showID = showID;
        this.reserved= reserved;
        this.seats = seats;
    }



    /**
    //getter and setter for booking
    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }
*/


}



