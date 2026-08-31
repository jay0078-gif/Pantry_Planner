package com.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.Purchase;
import com.main.model.Role;
import com.main.model.User;
import com.main.repository.IngredientRepository;
import com.main.repository.PurchaseRepository;
import com.main.repository.RecipeRepository;
import com.main.repository.RecipeSubmissionRepository;
import com.main.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PantryPlannerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeSubmissionRepository recipeSubmissionRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private Clock clock;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void recipeSubmissionIngredientsTableHasAPrimaryKey() throws Exception {
        List<String> primaryKeyColumns = new ArrayList<>();
        try (var connection = dataSource.getConnection();
             ResultSet keys = connection.getMetaData().getPrimaryKeys(
                     null, null, "recipe_submission_ingredients")) {
            while (keys.next()) {
                primaryKeyColumns.add(keys.getString("COLUMN_NAME"));
            }
        }

        assertThat(primaryKeyColumns)
                .containsExactlyInAnyOrder("submission_id", "ingredient_position");
    }

    @Test
    void seededRecipesDoNotPersistBrokenImageUrls() {
        assertThat(recipeRepository.findAll())
                .hasSize(250)
                .noneMatch(recipe -> {
                    String imageUrl = recipe.getImageUrl();
                    return imageUrl != null
                            && (imageUrl.contains("source.unsplash.com") || imageUrl.contains("/.../"));
                });
    }

    @Test
    @Transactional
    void registrationCannotOverwriteAnExistingUser() throws Exception {
        User owner = userRepository.findByUsername("owner").orElseThrow();
        Long ownerId = owner.getId();
        String ownerPasswordHash = owner.getPassword();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"OWNER","password":"replacement-password"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":%d,"username":"new-user","password":"new-user-password","role":"ROLE_ADMIN"}
                                """.formatted(ownerId)))
                .andExpect(status().isCreated());

        User unchangedOwner = userRepository.findById(ownerId).orElseThrow();
        User newUser = userRepository.findByUsername("new-user").orElseThrow();
        assertThat(unchangedOwner.getUsername()).isEqualTo("owner");
        assertThat(unchangedOwner.getPassword()).isEqualTo(ownerPasswordHash);
        assertThat(newUser.getId()).isNotEqualTo(ownerId);
        assertThat(newUser.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(passwordEncoder.matches("new-user-password", newUser.getPassword())).isTrue();
    }

    @Test
    void loginIssuesATokenThatAuthenticatesCurrentUser() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"owner","password":"test-owner-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).path("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void invalidUsernameAndPasswordReturnTheSameError() throws Exception {
        String unknownUser = failedLoginBody("missing-user", "wrong-password");
        String wrongPassword = failedLoginBody("owner", "wrong-password");

        assertThat(unknownUser)
                .isEqualTo(wrongPassword)
                .contains("Invalid username or password");
    }

    @Test
    void protectedRoutesRejectInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isUnauthorized());

        String validToken = loginToken("owner", "test-owner-password");
        mockMvc.perform(get("/api/auth/current")
                        .header("Authorization", "Bearer " + tamper(validToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/current")
                        .header("Authorization", "Bearer " + customToken(
                                "wrong-issuer", "pantry-planner-web", clock.instant().plusSeconds(300))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/current")
                        .header("Authorization", "Bearer " + customToken(
                                "pantry-planner", "wrong-audience", clock.instant().plusSeconds(300))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/current")
                        .header("Authorization", "Bearer " + customToken(
                                "pantry-planner", "pantry-planner-web", clock.instant().minusSeconds(300))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void purchaseReceiptsAreVisibleOnlyToTheirOwner() throws Exception {
        User owner = userRepository.findByUsername("owner").orElseThrow();
        User otherUser = new User();
        otherUser.setUsername("purchase-viewer");
        otherUser.setPassword(passwordEncoder.encode("purchase-viewer-password"));
        otherUser.setRole(Role.ROLE_USER);
        userRepository.save(otherUser);

        Purchase purchase = new Purchase(
                owner,
                ingredientRepository.findAll().get(0),
                new BigDecimal("2.5"),
                "kg",
                new BigDecimal("12.50"));
        purchaseRepository.saveAndFlush(purchase);

        mockMvc.perform(get("/api/purchases/" + purchase.getId())
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchase.getId()))
                .andExpect(jsonPath("$.quantity").value(2.5))
                .andExpect(jsonPath("$.price").value(12.50));

        mockMvc.perform(get("/api/purchases/" + purchase.getId())
                        .header("Authorization", "Bearer " + loginToken(
                                "purchase-viewer", "purchase-viewer-password")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/purchases/" + Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void pantryWritesCannotCreateGlobalIngredients() throws Exception {
        long ingredientCount = ingredientRepository.count();
        String existingIngredient = ingredientRepository.findAll().get(0).getName();

        mockMvc.perform(post("/api/pantry")
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientName":"unreviewed-test-ingredient"}
                                """))
                .andExpect(status().isNotFound());

        assertThat(ingredientRepository.count()).isEqualTo(ingredientCount);
        assertThat(ingredientRepository.findByName("unreviewed-test-ingredient")).isEmpty();

        mockMvc.perform(post("/api/pantry")
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("ingredientName", existingIngredient))))
                .andExpect(status().isOk());

        assertThat(ingredientRepository.count()).isEqualTo(ingredientCount);
    }

    @Test
    @Transactional
    void recipeResponsesDoNotExposeCreatorAccounts() throws Exception {
        var recipe = recipeRepository.findAll().get(0);
        recipe.setImageUrl("https://images.pexels.com/photos/123/food.jpeg");
        recipe.setImageSourceUrl("https://www.pexels.com/photo/food-123/");
        recipe.setImagePhotographer("Test Photographer");
        recipe.setImagePhotographerUrl("https://www.pexels.com/@test-photographer/");
        recipeRepository.save(recipe);

        mockMvc.perform(get("/api/recipes/" + recipe.getId())
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.imageUrl")
                        .value("https://images.pexels.com/photos/123/food.jpeg"))
                .andExpect(jsonPath("$.imageSourceUrl")
                        .value("https://www.pexels.com/photo/food-123/"))
                .andExpect(jsonPath("$.imagePhotographer").value("Test Photographer"))
                .andExpect(jsonPath("$.imagePhotographerUrl")
                        .value("https://www.pexels.com/@test-photographer/"))
                .andExpect(jsonPath("$.imageLookupAttemptedAt").doesNotExist())
                .andExpect(jsonPath("$.imageLookupRetryAt").doesNotExist())
                .andExpect(jsonPath("$.imageLookupFailures").doesNotExist());
    }

    @Test
    @Transactional
    void jwtPrincipalAndRolesWorkAcrossApplicationRoutes() throws Exception {
        User user = new User();
        user.setUsername("jwt-user");
        user.setPassword(passwordEncoder.encode("jwt-user-password"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        String userToken = loginToken("jwt-user", "jwt-user-password");
        mockMvc.perform(post("/api/user/recipes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":999999,"title":"Token Test Recipe","instructions":"Mix and serve.",
                                 "ingredients":["beans"],"approved":true,"rejected":true}
                                """))
                .andExpect(status().isCreated());
        var submission = recipeSubmissionRepository.findAll().stream()
                .filter(item -> item.getTitle().equals("Token Test Recipe"))
                .findFirst()
                .orElseThrow();
        assertThat(submission.getId()).isNotEqualTo(999999L);
        assertThat(submission.isApproved()).isFalse();
        assertThat(submission.isRejected()).isFalse();
        assertThat(submission.getSubmittedBy().getUsername()).isEqualTo("jwt-user");

        mockMvc.perform(get("/api/admin/recipes/pending")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pantry")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/shopping-list")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/suggestions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/recipes/pending")
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password")))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void userRegistrationIsPublicAndAdminRegistrationStaysProtected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"public-user","password":"public-password"}
                                """))
                .andExpect(status().isCreated());

        User publicUser = userRepository.findByUsername("public-user").orElseThrow();
        assertThat(publicUser.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(passwordEncoder.matches("public-password", publicUser.getPassword())).isTrue();

        String userToken = loginToken("public-user", "public-password");
        mockMvc.perform(post("/api/auth/register-admin")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"forbidden-admin","password":"another-password"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"another-admin","password":"another-password"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register-admin")
                        .header("Authorization", "Bearer " + loginToken("owner", "test-owner-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"created-admin","password":"another-password"}
                                """))
                .andExpect(status().isCreated());

        User createdAdmin = userRepository.findByUsername("created-admin").orElseThrow();
        assertThat(createdAdmin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(passwordEncoder.matches("another-password", createdAdmin.getPassword())).isTrue();
    }

    @Test
    void healthCheckIncludesTheDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void corsAllowsOnlyTheGitHubPagesOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", "https://jay0078-gif.github.io")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin", "https://jay0078-gif.github.io"));

        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private String failedLoginBody(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(username, password))))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String customToken(String issuer, String audience, Instant expiresAt) {
        Instant issuedAt = expiresAt.isBefore(clock.instant())
                ? expiresAt.minusSeconds(60)
                : clock.instant().minusSeconds(5);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject("owner")
                .audience(List.of(audience))
                .claim("roles", List.of("ROLE_ADMIN"))
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private static String tamper(String token) {
        String[] segments = token.split("\\.");
        int index = segments[2].length() / 2;
        char replacement = segments[2].charAt(index) == 'a' ? 'b' : 'a';
        segments[2] = segments[2].substring(0, index) + replacement + segments[2].substring(index + 1);
        return String.join(".", segments);
    }

    private record LoginBody(String username, String password) {
    }
}
