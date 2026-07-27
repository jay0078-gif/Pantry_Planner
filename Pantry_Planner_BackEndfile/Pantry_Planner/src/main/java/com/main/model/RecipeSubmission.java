//package com.main.model;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Table(name = "recipe_submission")
//public class RecipeSubmission {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String title;
//
//    @Column(length = 5000)
//    private String instructions;
//
//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(
//            name = "recipe_submission_ingredients",
//            joinColumns = @JoinColumn(name = "submission_id"))
//    @Column(name = "ingredient")
//    private List<String> ingredients = new ArrayList<>();
//
//    private boolean approved = false;
//
//    @Column(updatable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    private LocalDateTime approvedAt;
//
//    @ManyToOne
//    @JoinColumn(name = "submitted_by_id")
//    private User submittedBy;
//
//    // 🆕 Optional: record who approved for traceability
//    @ManyToOne
//    @JoinColumn(name = "approved_by_id")
//    private User approvedBy;
//
//    // ---------- getters & setters ----------
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getTitle() { return title; }
//    public void setTitle(String title) { this.title = title; }
//
//    public String getInstructions() { return instructions; }
//    public void setInstructions(String instructions) { this.instructions = instructions; }
//
//    public List<String> getIngredients() { return ingredients; }
//    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
//
//    public boolean isApproved() { return approved; }
//    public void setApproved(boolean approved) { this.approved = approved; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//
//    public LocalDateTime getApprovedAt() { return approvedAt; }
//    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
//
//    public User getSubmittedBy() { return submittedBy; }
//    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }
//
//    public User getApprovedBy() { return approvedBy; }
//    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
//}


package com.main.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe_submission")
public class RecipeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String instructions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "recipe_submission_ingredients",
            joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "ingredient")
    private List<String> ingredients = new ArrayList<>();

    /* ---------- Status Flags ---------- */
    private boolean approved = false;
    private boolean rejected = false;

    /* ---------- Timestamps ---------- */
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;

    /* ---------- Relationships ---------- */

    // the user who submitted
    @ManyToOne
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    // user who approved (owner / admin)
    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    // user who rejected (owner / admin)
    @ManyToOne
    @JoinColumn(name = "rejected_by_id")
    private User rejectedBy;

    /* ---------- Getters & Setters ---------- */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean isRejected() { return rejected; }
    public void setRejected(boolean rejected) { this.rejected = rejected; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public User getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(User rejectedBy) { this.rejectedBy = rejectedBy; }
}