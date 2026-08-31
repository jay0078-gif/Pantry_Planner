package com.main.service;

import com.main.model.Recipe;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PhotoServiceTests {

    private static final String FALLBACK = "/images/food-fallback.png";

    @Test
    void missingKeyLeavesTheRecipeOnItsFallbackWithoutCallingPexels() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PhotoService service = new PhotoService(FALLBACK, "", restTemplate);
        Recipe recipe = recipeNamed("Tomato Pasta");

        PhotoService.ImageLookupResult result = service.ensureImage(recipe);

        assertThat(result).isEqualTo(PhotoService.ImageLookupResult.DISABLED);
        assertThat(recipe.getImageUrl()).isNull();
        server.verify();
    }

    @Test
    void configuredKeyStoresThePexelsImageAndItsAttribution() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PhotoService service = new PhotoService(FALLBACK, "test-pexels-key", restTemplate);
        Recipe recipe = recipeNamed("Tomato Pasta");

        server.expect(once(), requestTo(
                        "https://api.pexels.com/v1/search?query=Tomato%20Pasta&per_page=1&orientation=landscape"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "test-pexels-key"))
                .andRespond(withSuccess("""
                        {
                          "photos": [{
                            "url": "https://www.pexels.com/photo/pasta-123/",
                            "photographer": "Alex Cook",
                            "photographer_url": "https://www.pexels.com/@alex-cook/",
                            "src": {
                              "landscape": "https://images.pexels.com/photos/123/pasta.jpeg",
                              "large": "https://images.pexels.com/photos/123/pasta-large.jpeg"
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON)
                        .header("X-Ratelimit-Remaining", "25"));

        PhotoService.ImageLookupResult result = service.ensureImage(recipe);

        assertThat(result).isEqualTo(PhotoService.ImageLookupResult.UPDATED);
        assertThat(recipe.getImageUrl())
                .isEqualTo("https://images.pexels.com/photos/123/pasta.jpeg");
        assertThat(recipe.getImageSourceUrl())
                .isEqualTo("https://www.pexels.com/photo/pasta-123/");
        assertThat(recipe.getImagePhotographer()).isEqualTo("Alex Cook");
        assertThat(recipe.getImagePhotographerUrl())
                .isEqualTo("https://www.pexels.com/@alex-cook/");
        assertThat(service.shouldPauseBackfill()).isTrue();
        server.verify();
    }

    @Test
    void rateLimitStopsTheLookupWithoutOverwritingTheRecipe() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PhotoService service = new PhotoService(FALLBACK, "test-pexels-key", restTemplate);
        Recipe recipe = recipeNamed("Tomato Pasta");

        server.expect(once(), requestTo(
                        "https://api.pexels.com/v1/search?query=Tomato%20Pasta&per_page=1&orientation=landscape"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        PhotoService.ImageLookupResult result = service.ensureImage(recipe);

        assertThat(result).isEqualTo(PhotoService.ImageLookupResult.RATE_LIMITED);
        assertThat(recipe.getImageUrl()).isNull();
        server.verify();
    }

    @Test
    void rejectedKeyStopsTheLookupWithoutOverwritingTheRecipe() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PhotoService service = new PhotoService(FALLBACK, "test-pexels-key", restTemplate);
        Recipe recipe = recipeNamed("Tomato Pasta");

        server.expect(once(), requestTo(
                        "https://api.pexels.com/v1/search?query=Tomato%20Pasta&per_page=1&orientation=landscape"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        PhotoService.ImageLookupResult result = service.ensureImage(recipe);

        assertThat(result).isEqualTo(PhotoService.ImageLookupResult.AUTHENTICATION_FAILED);
        assertThat(recipe.getImageUrl()).isNull();
        assertThat(recipe.getImageSourceUrl()).isNull();
        server.verify();
    }

    @Test
    void emptySearchResultLeavesTheRecipeUnchanged() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PhotoService service = new PhotoService(FALLBACK, "test-pexels-key", restTemplate);
        Recipe recipe = recipeNamed("Unknown Dish");

        server.expect(once(), requestTo(
                        "https://api.pexels.com/v1/search?query=Unknown%20Dish&per_page=1&orientation=landscape"))
                .andRespond(withSuccess("{\"photos\":[]}", MediaType.APPLICATION_JSON));

        PhotoService.ImageLookupResult result = service.ensureImage(recipe);

        assertThat(result).isEqualTo(PhotoService.ImageLookupResult.NOT_FOUND);
        assertThat(recipe.getImageUrl()).isNull();
        server.verify();
    }

    @Test
    void validStoredImageDoesNotCallPexelsAgain() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PhotoService service = new PhotoService(FALLBACK, "test-pexels-key", restTemplate);
        Recipe recipe = recipeNamed("Tomato Pasta");
        recipe.setImageUrl("https://images.pexels.com/photos/123/pasta.jpeg");
        recipe.setImageSourceUrl("https://www.pexels.com/photo/pasta-123/");
        recipe.setImagePhotographer("Alex Cook");
        recipe.setImagePhotographerUrl("https://www.pexels.com/@alex-cook/");

        PhotoService.ImageLookupResult result = service.ensureImage(recipe);

        assertThat(result).isEqualTo(PhotoService.ImageLookupResult.EXISTING);
        assertThat(recipe.getImageUrl())
                .isEqualTo("https://images.pexels.com/photos/123/pasta.jpeg");
        server.verify();
    }

    @Test
    void legacyPexelsPlaceholderIsTreatedAsMissing() {
        PhotoService service = new PhotoService(FALLBACK, "", new RestTemplate());

        assertThat(service.isFallback(
                "https://images.pexels.com/photos/.../pexels-photo.jpeg"))
                .isTrue();
    }

    private static Recipe recipeNamed(String name) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        return recipe;
    }
}
