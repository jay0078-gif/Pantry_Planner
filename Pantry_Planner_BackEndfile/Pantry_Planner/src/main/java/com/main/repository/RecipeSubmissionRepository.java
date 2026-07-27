package com.main.repository;

import com.main.model.RecipeSubmission;
import com.main.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecipeSubmissionRepository extends JpaRepository<RecipeSubmission, Long> {

    /** 
     * 🔹 Returns all submissions that are not yet approved (for admin/owner review).
     */
    List<RecipeSubmission> findByApprovedFalse();

    /** 
     * 🔹 Returns all submissions created by the given user (for user dashboard/history).
     */
    List<RecipeSubmission> findBySubmittedBy(User user);
    
    List<RecipeSubmission> findByApprovedFalseAndRejectedFalse();
}