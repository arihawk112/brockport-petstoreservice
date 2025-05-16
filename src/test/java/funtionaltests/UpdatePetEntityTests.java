package funtionaltests;

import com.petstore.PetEntity;
import com.petstore.PetStoreReader;
import com.petstore.animals.attributes.PetType;
import com.petstoreservices.exceptions.PetDataStoreException;
import funtionaltests.BadRequestResponseBody;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.stream.Stream;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for updating pet entities via REST API
 */
public class UpdatePetEntityTests {
    private static Headers headers;
    private List<PetEntity> originalPets;

    @BeforeEach
    public void setup() throws PetDataStoreException {
        RestAssured.baseURI = "http://localhost:8080/";
        Header contentType = new Header("Content-Type", ContentType.JSON.toString());
        Header accept = new Header("Accept", ContentType.JSON.toString());
        headers = new Headers(contentType, accept);

        // Load test data
        PetStoreReader reader = new PetStoreReader();
        originalPets = reader.readJsonFromFile();
    }

    @TestFactory
    @DisplayName("Update Dog Price - Happy Path")
    public Stream<DynamicTest> updateDogPriceSuccessTest() {
        // Find first available dog
        PetEntity targetDog = originalPets.stream()
                .filter(p -> p.getPetType() == PetType.DOG)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No dogs available for testing"));

        // New price
        double newPrice = targetDog.getCost().doubleValue() + 50.0;
        String requestBody = String.format("{\"price\": %.2f}", newPrice);

        // Execute update
        PetEntity updatedPet = given()
                .headers(headers)
                .body(requestBody)
                .when()
                .patch("inventory/dog/" + targetDog.getPetId())
                .then()
                .log().all()
                .assertThat().statusCode(200)
                .assertThat().contentType(ContentType.JSON)
                .extract()
                .as(PetEntity.class);

        // Verify updates
        return Stream.of(
                DynamicTest.dynamicTest("Price updated correctly",
                        () -> assertEquals(newPrice, updatedPet.getCost().doubleValue(), 0.01)),
                DynamicTest.dynamicTest("Other fields unchanged",
                        () -> {
                            assertEquals(targetDog.getPetType(), updatedPet.getPetType());
                            assertEquals(targetDog.getBreed(), updatedPet.getBreed());
                        })
        );
    }

    @TestFactory
    @DisplayName("Update Non-Existent Pet")
    public Stream<DynamicTest> updateNonExistentPetTest() {
        int invalidId = originalPets.stream()
                .mapToInt(PetEntity::getPetId)
                .max()
                .orElse(0) + 100; // Guaranteed non-existent ID

        String requestBody = "{\"price\": 100.00}";

        BadRequestResponseBody response = given()
                .headers(headers)
                .body(requestBody)
                .when()
                .patch("inventory/dog/" + invalidId)
                .then()
                .log().all()
                .assertThat().statusCode(404)
                .extract()
                .as(BadRequestResponseBody.class);

        return response.executeTests(
                "Not Found",
                "Pet with ID " + invalidId + " not found",
                "/inventory/dog/" + invalidId,
                404
        ).stream();
    }

    @TestFactory
    @DisplayName("Invalid Update - Negative Price")
    public Stream<DynamicTest> updateWithInvalidPriceTest() {
        PetEntity targetDog = originalPets.stream()
                .filter(p -> p.getPetType() == PetType.DOG)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No dogs available"));

        String invalidPriceBody = "{\"price\": -10.00}";

        BadRequestResponseBody response = given()
                .headers(headers)
                .body(invalidPriceBody)
                .when()
                .patch("inventory/dog/" + targetDog.getPetId())
                .then()
                .log().all()
                .assertThat().statusCode(400)
                .extract()
                .as(BadRequestResponseBody.class);

        return response.executeTests(
                "Bad Request",
                "Price must be positive",
                "/inventory/dog/" + targetDog.getPetId(),
                400
        ).stream();
    }
}