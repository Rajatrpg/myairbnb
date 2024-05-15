package com.airbnb.controller;

import com.airbnb.dto.BookingDto;
import com.airbnb.entity.Booking;
import com.airbnb.entity.Property;
import com.airbnb.entity.PropertyUser;
import com.airbnb.repository.BookingRepository;
import com.airbnb.repository.PropertyRepository;
import com.airbnb.service.BucketService;
import com.airbnb.service.PDFService;
import com.airbnb.service.SmsService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {

    private BookingRepository bookingRepository;
    private PropertyRepository propertyRepository;
    private PDFService pdfService;

    private BucketService bucketService;

    private SmsService smsService;

    public BookingController(BookingRepository bookingRepository, PropertyRepository propertyRepository, PDFService pdfService, BucketService bucketService, SmsService smsService) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.pdfService = pdfService;
        this.bucketService = bucketService;
        this.smsService = smsService;
    }

    @PostMapping("/createBooking/{propertyId}")
    public ResponseEntity<?> createBooking(@RequestBody Booking booking, @AuthenticationPrincipal PropertyUser propertyUser,
                                                 @PathVariable long propertyId) throws IOException {

        booking.setPropertyUser ( propertyUser );
//        Property property = booking.getProperty ();
//        Long propertyId = property.getId ();
//
//        Property completePropertyInfo = propertyRepository.findById ( propertyId ).get ();
//
//        Booking createdBooking = bookingRepository.save ( booking );
        Property property = propertyRepository.findById ( propertyId ).get ();

        int propertyPrice = property.getNightlyPrice ();

        int totalNights = booking.getTotalNights ();

        int totalPrice = propertyPrice * totalNights;

        booking.setProperty ( property );

        booking.setTotalPrice ( totalPrice );

        Booking createdBooking = bookingRepository.save ( booking );

        BookingDto dto = new BookingDto ();
        dto.setBookingId ( createdBooking.getId () );
        dto.setGuestName ( createdBooking.getGuestName () );
        dto.setPrice(propertyPrice);
        dto.setTotalPrice ( createdBooking.getTotalPrice () );


        // CREATE A PDF WITH BOOKING CONFIRMATION ---->>>
        boolean b = pdfService.generatePDF ( "D://java//" + "booking-confirmation-id" + createdBooking.getId () + ".pdf", dto );
        if(b){
            MultipartFile file= BookingController.convert ( "D://java//" + "booking-confirmation-id" + createdBooking.getId () + ".pdf");
            // Upload your file into bucket
            String uploadedFileUrl = bucketService.uploadFile ( file, "rajatairbnb1" );

            smsService.sendSms ( "+917703926016","Your Booking is Confirm. Click for more information "+uploadedFileUrl );
        }else{
            return  new ResponseEntity <> ("Something went Wrong", HttpStatus.INTERNAL_SERVER_ERROR );

        }


        return  new ResponseEntity <> ( createdBooking, HttpStatus.CREATED );
    }

    public static MultipartFile convert(String filePath) throws IOException {
        File file = new File ( filePath );
        // Read the file into a byte array
        byte[] fileContent = Files.readAllBytes ( file.toPath () );

        // Create a Resource object from the byte array
        Resource resource = new ByteArrayResource ( fileContent );

        // Convert MultipartFile from Resource
        MultipartFile multipartFile = new MultipartFile () {
            @Override
            public String getName() {
                return file.getName ();
            }

            @Override
            public String getOriginalFilename() {
                return file.getName ();
            }

            @Override
            public String getContentType() {
                return null;
            }


            @Override
            public boolean isEmpty() {
                return fileContent.length==0;
            }

            @Override
            public long getSize() {
                return fileContent.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return fileContent;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return resource.getInputStream ();
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                Files.write ( dest.toPath (),fileContent );
            }
        };
        return multipartFile;
    }
}
