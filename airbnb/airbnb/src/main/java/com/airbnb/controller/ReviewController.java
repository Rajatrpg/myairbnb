package com.airbnb.controller;

import com.airbnb.entity.Property;
import com.airbnb.entity.PropertyUser;
import com.airbnb.entity.Review;
import com.airbnb.repository.PropertyRepository;
import com.airbnb.repository.PropertyUserRepository;
import com.airbnb.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private PropertyRepository propertyRepository;

    private ReviewRepository reviewRepository;
    private final PropertyUserRepository propertyUserRepository;

    public ReviewController(PropertyRepository propertyRepository, ReviewRepository reviewRepository,
                            PropertyUserRepository propertyUserRepository) {
        this.propertyRepository = propertyRepository;
        this.reviewRepository = reviewRepository;
        this.propertyUserRepository = propertyUserRepository;
    }


    @PostMapping("/addReview/{propertyId}")
    public ResponseEntity<String> addReview(
            @PathVariable long propertyId,
            @RequestBody Review review,
            @AuthenticationPrincipal PropertyUser propertyUser) {
        // Have to do it, The Review Part, It's incomplete--->>>


        Optional <Property> opProperty = propertyRepository.findById ( propertyId );
        Property property = opProperty.get ();

        Review r = reviewRepository.findReviewByUser ( property, propertyUser );

        if(r!=null){
            return new ResponseEntity <> ( "You have already added a review for this",HttpStatus.BAD_REQUEST );
        }

        review.setProperty ( property );
        review.setPropertyUser ( propertyUser );

        reviewRepository.save ( review );
        return new ResponseEntity <> ( "Review added successfully", HttpStatus.CREATED );
    }

    @GetMapping("/userRev")
    public ResponseEntity<List<Review>> getUserReviews(@AuthenticationPrincipal PropertyUser propertyUser){
        List<Review> reviews= reviewRepository.findByPropertyUser ( propertyUser );
        return new ResponseEntity<> (reviews,HttpStatus.OK  );

    }
}
