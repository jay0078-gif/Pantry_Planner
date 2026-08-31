package com.main.seed;

import com.main.model.Recipe;
import com.main.repository.RecipeRepository;
import com.main.service.PhotoService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhotoBackfillTests {

    @Test
    void disabledBackfillDoesNotReadRecipes() {
        RecipeRepository repository = mock(RecipeRepository.class);
        PhotoService photoService = mock(PhotoService.class);
        PhotoBackfill backfill = backfill(false, 100, repository, photoService);

        backfill.backfillMissingImages();

        verify(repository, never()).findAllByOrderByIdAsc();
        verify(photoService, never()).ensureImage(any());
    }

    @Test
    void enabledBackfillPersistsOnlyTheConfiguredBatch() {
        RecipeRepository repository = mock(RecipeRepository.class);
        PhotoService photoService = mock(PhotoService.class);
        List<Recipe> recipes = List.of(recipeNamed("One"), recipeNamed("Two"), recipeNamed("Three"));
        when(photoService.isEnabled()).thenReturn(true);
        when(photoService.needsImage(any())).thenReturn(true);
        when(repository.findAllByOrderByIdAsc()).thenReturn(recipes);
        doAnswer(invocation -> {
            Recipe recipe = invocation.getArgument(0);
            recipe.setImageUrl("https://images.pexels.com/photos/123/food.jpeg");
            return PhotoService.ImageLookupResult.UPDATED;
        }).when(photoService).ensureImage(any());

        PhotoBackfill backfill = backfill(true, 2, repository, photoService);
        backfill.backfillMissingImages();

        verify(photoService, times(2)).ensureImage(any());
        verify(repository).save(recipes.get(0));
        verify(repository).save(recipes.get(1));
        verify(repository, never()).save(recipes.get(2));
    }

    @Test
    void enabledBackfillWithoutAKeyDoesNotReadRecipes() {
        RecipeRepository repository = mock(RecipeRepository.class);
        PhotoService photoService = mock(PhotoService.class);
        when(photoService.isEnabled()).thenReturn(false);
        PhotoBackfill backfill = backfill(true, 100, repository, photoService);

        backfill.backfillMissingImages();

        verify(repository, never()).findAllByOrderByIdAsc();
        verify(photoService, never()).ensureImage(any());
    }

    @Test
    void validImagesDoNotConsumeTheBatch() {
        RecipeRepository repository = mock(RecipeRepository.class);
        PhotoService photoService = mock(PhotoService.class);
        Recipe valid = recipeNamed("Already Filled");
        valid.setImageUrl("https://images.pexels.com/photos/1/filled.jpeg");
        Recipe missing = recipeNamed("Missing");
        when(photoService.isEnabled()).thenReturn(true);
        when(photoService.needsImage(valid)).thenReturn(false);
        when(photoService.needsImage(missing)).thenReturn(true);
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(valid, missing));
        when(photoService.ensureImage(missing)).thenReturn(PhotoService.ImageLookupResult.UPDATED);

        PhotoBackfill backfill = backfill(true, 1, repository, photoService);
        backfill.backfillMissingImages();

        verify(photoService, never()).ensureImage(valid);
        verify(photoService).ensureImage(missing);
        verify(repository).save(missing);
    }

    @Test
    void notFoundRecipeIsDeferredSoTheNextBatchCanProgress() {
        RecipeRepository repository = mock(RecipeRepository.class);
        PhotoService photoService = mock(PhotoService.class);
        Recipe first = recipeNamed("No Result");
        Recipe second = recipeNamed("Later Recipe");
        when(photoService.isEnabled()).thenReturn(true);
        when(photoService.needsImage(any())).thenReturn(true);
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(first, second));
        when(photoService.ensureImage(first)).thenReturn(PhotoService.ImageLookupResult.NOT_FOUND);
        when(photoService.ensureImage(second)).thenReturn(PhotoService.ImageLookupResult.UPDATED);

        PhotoBackfill backfill = backfill(true, 1, repository, photoService);
        backfill.backfillMissingImages();
        backfill.backfillMissingImages();

        verify(photoService, times(1)).ensureImage(first);
        verify(photoService, times(1)).ensureImage(second);
        verify(repository).save(first);
        verify(repository).save(second);
    }

    @Test
    void rateLimitStopsTheCurrentBatchImmediately() {
        RecipeRepository repository = mock(RecipeRepository.class);
        PhotoService photoService = mock(PhotoService.class);
        when(photoService.isEnabled()).thenReturn(true);
        when(photoService.needsImage(any())).thenReturn(true);
        when(repository.findAllByOrderByIdAsc())
                .thenReturn(List.of(recipeNamed("One"), recipeNamed("Two")));
        when(photoService.ensureImage(any()))
                .thenReturn(PhotoService.ImageLookupResult.RATE_LIMITED);

        PhotoBackfill backfill = backfill(true, 100, repository, photoService);
        backfill.backfillMissingImages();

        verify(photoService, times(1)).ensureImage(any());
        verify(repository, never()).save(any());
    }

    private static Recipe recipeNamed(String name) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        return recipe;
    }

    private static PhotoBackfill backfill(
            boolean enabled,
            int batchSize,
            RecipeRepository repository,
            PhotoService photoService) {
        return new PhotoBackfill(
                enabled,
                batchSize,
                Duration.ofDays(30),
                Duration.ofHours(1),
                Duration.ofHours(24),
                Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC),
                repository,
                photoService);
    }
}
