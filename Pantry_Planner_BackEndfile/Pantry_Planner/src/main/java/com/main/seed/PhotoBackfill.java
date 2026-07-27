package com.main.seed;

import com.main.model.Recipe;
import com.main.repository.RecipeRepository;
import com.main.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PhotoBackfill {

  private static final Logger log = LoggerFactory.getLogger(PhotoBackfill.class);

  private final boolean runOnStart;
  private final RecipeRepository recipeRepo;
  private final PhotoService photoService;

  public PhotoBackfill(@Value("${photos.backfill-on-start:false}") boolean runOnStart,
                       RecipeRepository recipeRepo,
                       PhotoService photoService) {
    this.runOnStart = runOnStart;
    this.recipeRepo = recipeRepo;
    this.photoService = photoService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    if (!runOnStart) {
      log.info("PhotoBackfill: disabled (photos.backfill-on-start=false)");
      return;
    }
    log.info("PhotoBackfill: starting");

    List<Recipe> all = recipeRepo.findAll();
    int updated = 0, skippedValid = 0, tried = 0;

    for (Recipe r : all) {
      // If the current URL is already valid (not fallback/placeholder), skip
      if (!photoService.isFallback(r.getImageUrl())) {
        skippedValid++;
        continue;
      }

      tried++;
      String before = r.getImageUrl();

      // Try to find a real image; this only sets when we get a non-fallback
      photoService.ensureImage(r);

      String after = r.getImageUrl();
      if (after != null && !photoService.isFallback(after) && !after.equals(before)) {
        recipeRepo.save(r);
        updated++;
      }
    }

    log.info("PhotoBackfill: done. updated={}, skippedValid={}, tried={}", updated, skippedValid, tried);
  }
}