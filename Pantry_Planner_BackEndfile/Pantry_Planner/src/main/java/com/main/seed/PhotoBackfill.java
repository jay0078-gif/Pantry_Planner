package com.main.seed;

import com.main.model.Recipe;
import com.main.repository.RecipeRepository;
import com.main.service.PhotoService;
import com.main.service.PhotoService.ImageLookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class PhotoBackfill {

  private static final Logger log = LoggerFactory.getLogger(PhotoBackfill.class);

  private final boolean enabled;
  private final int batchSize;
  private final Duration notFoundRetry;
  private final Duration failureRetry;
  private final Duration maxFailureRetry;
  private final Clock clock;
  private final RecipeRepository recipeRepository;
  private final PhotoService photoService;

  public PhotoBackfill(
      @Value("${photos.backfill.enabled:false}") boolean enabled,
      @Value("${photos.backfill.batch-size:100}") int batchSize,
      @Value("${photos.backfill.not-found-retry:PT720H}") Duration notFoundRetry,
      @Value("${photos.backfill.failure-retry:PT1H}") Duration failureRetry,
      @Value("${photos.backfill.max-failure-retry:PT24H}") Duration maxFailureRetry,
      Clock clock,
      RecipeRepository recipeRepository,
      PhotoService photoService
  ) {
    this.enabled = enabled;
    this.batchSize = Math.max(1, batchSize);
    this.notFoundRetry = notFoundRetry;
    this.failureRetry = failureRetry;
    this.maxFailureRetry = maxFailureRetry;
    this.clock = clock;
    this.recipeRepository = recipeRepository;
    this.photoService = photoService;
  }

  @Scheduled(
      initialDelayString = "${photos.backfill.initial-delay:PT10S}",
      fixedDelayString = "${photos.backfill.interval:PT1H}"
  )
  public void backfillMissingImages() {
    if (!enabled) return;
    if (!photoService.isEnabled()) {
      log.warn("Photo backfill is enabled, but PEXELS_API_KEY is missing");
      return;
    }

    List<Recipe> recipes = recipeRepository.findAllByOrderByIdAsc();
    int attempted = 0;
    int updated = 0;
    int skipped = 0;
    int deferred = 0;
    int consecutiveFailures = 0;

    for (Recipe recipe : recipes) {
      if (!photoService.needsImage(recipe)) {
        skipped++;
        continue;
      }

      Instant now = clock.instant();
      if (recipe.getImageLookupRetryAt() != null
          && recipe.getImageLookupRetryAt().isAfter(now)) {
        deferred++;
        continue;
      }
      if (attempted >= batchSize || photoService.shouldPauseBackfill()) break;

      attempted++;
      ImageLookupResult result = photoService.ensureImage(recipe);
      recipe.setImageLookupAttemptedAt(now);

      if (result == ImageLookupResult.UPDATED) {
        clearRetry(recipe);
        recipeRepository.save(recipe);
        updated++;
        consecutiveFailures = 0;
        if (photoService.shouldPauseBackfill()) break;
        continue;
      }
      if (result == ImageLookupResult.NOT_FOUND) {
        recipe.setImageLookupFailures(0);
        recipe.setImageLookupRetryAt(now.plus(notFoundRetry));
        recipeRepository.save(recipe);
        deferred++;
        consecutiveFailures = 0;
        continue;
      }
      if (result == ImageLookupResult.FAILED) {
        int failures = recipe.getImageLookupFailures() + 1;
        recipe.setImageLookupFailures(failures);
        recipe.setImageLookupRetryAt(now.plus(failureDelay(failures)));
        recipeRepository.save(recipe);
        deferred++;
        consecutiveFailures++;
        if (consecutiveFailures >= 3) break;
        continue;
      }
      if (result == ImageLookupResult.RATE_LIMITED
          || result == ImageLookupResult.AUTHENTICATION_FAILED
          || result == ImageLookupResult.DISABLED) {
        break;
      }
    }

    log.info(
        "Photo backfill finished. attempted={}, updated={}, skipped={}, deferred={}, batchSize={}",
        attempted,
        updated,
        skipped,
        deferred,
        batchSize
    );
  }

  private Duration failureDelay(int failures) {
    int exponent = Math.min(Math.max(0, failures - 1), 10);
    Duration delay = failureRetry.multipliedBy(1L << exponent);
    return delay.compareTo(maxFailureRetry) > 0 ? maxFailureRetry : delay;
  }

  private void clearRetry(Recipe recipe) {
    recipe.setImageLookupFailures(0);
    recipe.setImageLookupRetryAt(null);
  }
}
