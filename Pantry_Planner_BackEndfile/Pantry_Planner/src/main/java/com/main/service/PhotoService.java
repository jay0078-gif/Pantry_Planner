package com.main.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.main.model.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
public class PhotoService {

  public enum ImageLookupResult {
    EXISTING,
    UPDATED,
    DISABLED,
    NOT_FOUND,
    RATE_LIMITED,
    AUTHENTICATION_FAILED,
    FAILED
  }

  private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

  private final String fallback;
  private final String pexelsApiKey;
  private final int backfillReserve;
  private final RestTemplate http;
  private final List<Pattern> placeholderPatterns;
  private final AtomicLong remainingRequests = new AtomicLong(Long.MAX_VALUE);

  @Autowired
  public PhotoService(
      @Value("${photos.fallback:/images/food-fallback.png}") String fallback,
      @Value("${pexels.apiKey:}") String pexelsApiKey,
      @Value("${pexels.backfill-reserve:25}") int backfillReserve,
      RestTemplateBuilder builder
  ) {
    this(
        fallback,
        pexelsApiKey,
        backfillReserve,
        builder
            .connectTimeout(Duration.ofSeconds(4))
            .readTimeout(Duration.ofSeconds(6))
            .build()
    );
  }

  PhotoService(String fallback, String pexelsApiKey, RestTemplate http) {
    this(fallback, pexelsApiKey, 25, http);
  }

  PhotoService(String fallback, String pexelsApiKey, int backfillReserve, RestTemplate http) {
    if (fallback == null || fallback.trim().isEmpty()) {
      throw new IllegalArgumentException("fallback cannot be blank");
    }
    this.fallback = fallback.trim();
    this.pexelsApiKey = pexelsApiKey == null ? "" : pexelsApiKey.trim();
    this.backfillReserve = Math.max(0, backfillReserve);
    this.http = Objects.requireNonNull(http);

    List<Pattern> patterns = new ArrayList<>();
    patterns.add(Pattern.compile("^https?://picsum\\.photos/.+$", Pattern.CASE_INSENSITIVE));
    patterns.add(Pattern.compile("^https?://source\\.unsplash\\.com/.+$", Pattern.CASE_INSENSITIVE));
    patterns.add(Pattern.compile(
        "^https?://images\\.pexels\\.com/photos/\\.\\.\\./.+$",
        Pattern.CASE_INSENSITIVE
    ));
    patterns.add(Pattern.compile("^/images/food-fallback\\..+$", Pattern.CASE_INSENSITIVE));
    this.placeholderPatterns = Collections.unmodifiableList(patterns);

    log.info("PhotoService ready. Pexels enabled: {}", isEnabled());
  }

  public boolean isEnabled() {
    return !pexelsApiKey.isEmpty();
  }

  public boolean shouldPauseBackfill() {
    return remainingRequests.get() <= backfillReserve;
  }

  public String getUrl(String imageUrl) {
    String url = normalize(imageUrl);
    if (url == null) return fallback;

    for (Pattern pattern : placeholderPatterns) {
      if (pattern.matcher(url).matches()) return fallback;
    }

    if (url.startsWith("/")) return url;

    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        return fallback;
      }
      if (uri.getHost() == null || uri.getHost().isEmpty()) return fallback;
      return url;
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }

  public boolean isFallback(String url) {
    return fallback.equals(getUrl(url));
  }

  public boolean needsImage(Recipe recipe) {
    if (recipe == null || isFallback(recipe.getImageUrl())) return true;
    return isPexelsImage(recipe.getImageUrl()) && !hasCompleteAttribution(recipe);
  }

  public ImageLookupResult ensureImage(Recipe recipe) {
    if (recipe == null) return ImageLookupResult.NOT_FOUND;

    String safe = getUrl(recipe.getImageUrl());
    if (!needsImage(recipe)) {
      if (!safe.equals(recipe.getImageUrl())) recipe.setImageUrl(safe);
      return ImageLookupResult.EXISTING;
    }

    String query = normalize(recipe.getName());
    if (query == null) return ImageLookupResult.NOT_FOUND;
    if (!isEnabled()) return ImageLookupResult.DISABLED;

    SearchResult result = searchPexels(query);
    if (result.photo() == null) return result.status();

    PexelsPhoto photo = result.photo();
    String imageUrl = firstNotBlank(
        photo.src.landscape,
        photo.src.large,
        photo.src.large2x,
        photo.src.medium,
        photo.src.original
    );
    if (!notBlank(imageUrl) || isFallback(imageUrl)) return ImageLookupResult.NOT_FOUND;

    String sourceUrl = normalize(photo.url);
    String photographer = normalize(photo.photographer);
    String photographerUrl = normalize(photo.photographerUrl);
    if (sourceUrl == null || photographer == null || photographerUrl == null) {
      log.warn("Pexels returned incomplete attribution for '{}'", query);
      return ImageLookupResult.FAILED;
    }

    recipe.setImageUrl(imageUrl);
    recipe.setImageSourceUrl(sourceUrl);
    recipe.setImagePhotographer(photographer);
    recipe.setImagePhotographerUrl(photographerUrl);
    return ImageLookupResult.UPDATED;
  }

  private SearchResult searchPexels(String query) {
    try {
      URI uri = UriComponentsBuilder
          .fromUriString("https://api.pexels.com/v1/search")
          .queryParam("query", query)
          .queryParam("per_page", 1)
          .queryParam("orientation", "landscape")
          .build()
          .encode()
          .toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", pexelsApiKey);
      HttpEntity<Void> request = new HttpEntity<>(headers);
      ResponseEntity<PexelsResponse> response =
          http.exchange(uri, HttpMethod.GET, request, PexelsResponse.class);

      updateRemainingRequests(response.getHeaders().getFirst("X-Ratelimit-Remaining"));

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        return SearchResult.failed(ImageLookupResult.FAILED);
      }
      PexelsResponse body = response.getBody();
      if (body.photos == null || body.photos.isEmpty() || body.photos.get(0).src == null) {
        return SearchResult.failed(ImageLookupResult.NOT_FOUND);
      }
      return SearchResult.found(body.photos.get(0));
    } catch (HttpStatusCodeException exception) {
      if (exception.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
        log.warn("Pexels rate limit reached while searching for '{}'", query);
        return SearchResult.failed(ImageLookupResult.RATE_LIMITED);
      }
      if (exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
          || exception.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
        log.error("Pexels rejected the configured API key");
        return SearchResult.failed(ImageLookupResult.AUTHENTICATION_FAILED);
      }
      log.warn("Pexels search failed for '{}': {}", query, exception.getStatusCode());
      return SearchResult.failed(ImageLookupResult.FAILED);
    } catch (RestClientException | IllegalArgumentException exception) {
      log.warn("Pexels search failed for '{}': {}", query, exception.getMessage());
      return SearchResult.failed(ImageLookupResult.FAILED);
    }
  }

  private static String firstNotBlank(String... values) {
    if (values == null) return null;
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) return value;
    }
    return null;
  }

  private static boolean notBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private boolean hasCompleteAttribution(Recipe recipe) {
    return normalize(recipe.getImageSourceUrl()) != null
        && normalize(recipe.getImagePhotographer()) != null
        && normalize(recipe.getImagePhotographerUrl()) != null;
  }

  private boolean isPexelsImage(String value) {
    String normalized = normalize(value);
    if (normalized == null) return false;
    try {
      URI uri = URI.create(normalized);
      return "https".equalsIgnoreCase(uri.getScheme())
          && "images.pexels.com".equalsIgnoreCase(uri.getHost());
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private void updateRemainingRequests(String value) {
    if (value == null || value.isBlank()) return;
    try {
      remainingRequests.set(Long.parseLong(value));
    } catch (NumberFormatException exception) {
      log.warn("Pexels returned an invalid remaining-request count");
    }
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) return null;
    return trimmed;
  }

  private record SearchResult(PexelsPhoto photo, ImageLookupResult status) {
    private static SearchResult found(PexelsPhoto photo) {
      return new SearchResult(photo, ImageLookupResult.UPDATED);
    }

    private static SearchResult failed(ImageLookupResult status) {
      return new SearchResult(null, status);
    }
  }

  public static class PexelsResponse {
    public List<PexelsPhoto> photos;
  }

  public static class PexelsPhoto {
    public String url;
    public String photographer;

    @JsonProperty("photographer_url")
    public String photographerUrl;

    public PexelsSrc src;
  }

  public static class PexelsSrc {
    public String original;
    public String large2x;
    public String large;
    public String medium;
    public String landscape;
  }
}
