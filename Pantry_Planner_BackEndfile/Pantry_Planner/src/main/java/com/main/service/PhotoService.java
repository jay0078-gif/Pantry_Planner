package com.main.service;

import com.main.model.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PhotoService {

  private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

  private final String fallback;
  private final String pexelsApiKey;
  private final RestTemplate http;

  private final List<Pattern> placeholderPatterns;
  private final boolean treatNullStringAsNull = true;

  public PhotoService(
      @Value("${photos.fallback:/images/food-fallback.png}") String fallback,
      @Value("${pexels.apiKey:}") String pexelsApiKey,
      RestTemplateBuilder builder
  ) {
    if (fallback == null || fallback.trim().isEmpty()) {
      throw new IllegalArgumentException("fallback cannot be blank");
    }
    this.fallback = fallback.trim();
    this.pexelsApiKey = pexelsApiKey == null ? "" : pexelsApiKey.trim();

    this.http = builder
        .connectTimeout(Duration.ofSeconds(4))
        .readTimeout(Duration.ofSeconds(6))
        .build();

    List<Pattern> patterns = new ArrayList<>();
    patterns.add(Pattern.compile("^https?://picsum\\.photos/.+$", Pattern.CASE_INSENSITIVE));
    patterns.add(Pattern.compile("^https?://source\\.unsplash\\.com/.+$", Pattern.CASE_INSENSITIVE));
    patterns.add(Pattern.compile("^/images/food-fallback\\..+$", Pattern.CASE_INSENSITIVE));
    this.placeholderPatterns = Collections.unmodifiableList(patterns);

    log.info("PhotoService ready. Pexels enabled: {}", !this.pexelsApiKey.isEmpty());
  }

  // -------------------------------------------------------------
  //  Existing functionality (unchanged)
  // -------------------------------------------------------------

  public String getUrl(String imageUrl) {
    String url = normalize(imageUrl);
    if (url == null) return fallback;

    for (Pattern p : placeholderPatterns) {
      if (p.matcher(url).matches()) return fallback;
    }

    if (url.startsWith("/")) return url;

    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
        return fallback;

      if (uri.getHost() == null || uri.getHost().isEmpty())
        return fallback;

      return url;
    } catch (IllegalArgumentException ex) {
      return fallback;
    }
  }

  public String getUrlOrSearch(String currentUrl, String query) {
    String safe = getUrl(currentUrl);
    if (!safe.equals(fallback)) return safe;
    String q = normalize(query);
    if (q == null || pexelsApiKey.isEmpty()) return fallback;
    String fromPexels = searchPexels(q);
    return fromPexels != null ? fromPexels : fallback;
  }

  public boolean isFallback(String url) {
    return fallback.equals(getUrl(url));
  }

  public void ensureImage(Recipe recipe) {
    if (recipe == null) return;

    String safe = getUrl(recipe.getImageUrl());
    if (!safe.equals(fallback)) {
      if (!safe.equals(recipe.getImageUrl())) recipe.setImageUrl(safe);
      return;
    }

    String q = normalize(recipe.getName());
    if (q == null || pexelsApiKey.isEmpty()) return;

    String fromPexels = searchPexels(q);
    if (notBlank(fromPexels) && !isFallback(fromPexels)) {
      recipe.setImageUrl(fromPexels);
    }
  }

  private String searchPexels(String query) {
    try {
      URI uri = UriComponentsBuilder
          .fromUriString("https://api.pexels.com/v1/search")
          .queryParam("query", query)
          .queryParam("per_page", 1)
          .queryParam("orientation", "landscape")
          .queryParam("size", "medium")
          .build()
          .encode()
          .toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", pexelsApiKey);
      HttpEntity<Void> req = new HttpEntity<>(headers);

      ResponseEntity<PexelsResponse> resp =
          http.exchange(uri, HttpMethod.GET, req, PexelsResponse.class);

      if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
      PexelsResponse body = resp.getBody();
      if (body.photos == null || body.photos.isEmpty() || body.photos.get(0).src == null) return null;

      PexelsSrc src = body.photos.get(0).src;
      return firstNotBlank(src.landscape, src.large, src.large2x, src.medium, src.original);
    } catch (RestClientException | IllegalArgumentException ex) {
      log.warn("Pexels search failed for '{}': {}", query, ex.getMessage());
      return null;
    }
  }

  private static String firstNotBlank(String... v) {
    if (v == null) return null;
    for (String s : v) if (s != null && !s.trim().isEmpty()) return s;
    return null;
  }

  private static boolean notBlank(String s) {
    return s != null && !s.trim().isEmpty();
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    if (trimmed.isEmpty()) return null;
    if (treatNullStringAsNull && "null".equalsIgnoreCase(trimmed)) return null;
    return trimmed;
  }

  // Minimal DTOs for Pexels JSON
  public static class PexelsResponse {
    public List<PexelsPhoto> photos;
  }

  public static class PexelsPhoto {
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
