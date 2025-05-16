package funtionaltests;

import com.petstore.PetEntity;
import com.petstore.PetStoreReader;
//import com.petstore.exceptions.AnimalType;
import com.petstore.animals.attributes.Breed;
import com.petstore.animals.attributes.Gender;
import com.petstore.animals.attributes.PetType;
import com.petstore.animals.attributes.Skin;
import com.petstoreservices.exceptions.PetDataStoreException;
import funtionaltests.BadRequestResponseBody;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.parsing.Parser;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class AddPetEntityTests {

    private static Headers headers;
    private List<PetEntity> expectedResults;

    @BeforeEach
    public void retrieveDataStore() throws PetDataStoreException {
        RestAssured.baseURI = "http://localhost:8080/";
        PetStoreReader psReader = new PetStoreReader();
        expectedResults = psReader.readJsonFromFile();
        Header contentType = new Header("Content-Type", ContentType.JSON.toString());
        Header accept = new Header("Accept", ContentType.JSON.toString());
        headers = new Headers(contentType, accept);
    }

    @TestFactory
    @DisplayName("Add New Pet Entity [Dog]")
    public Stream<DynamicTest> addNewPetEntityTest() throws PetDataStoreException {
        Random rand = new Random();
        int newPetId = 1000 + rand.nextInt(9000); // ensure new unique ID for each test run

        PetEntity newPet = new PetEntity(
                newPetId,
                PetType.DOG,
                new BigDecimal("249.99"),
                Gender.FEMALE,
                Skin.FUR,
                PetType.DOG,
                Breed.POODLE,
                25,
                4
        );

        PetEntity addedPet =
                given()
                        .headers(headers)
                        .body(newPet)
                        .when()
                        .post("inventory/add")
                        .then()
                        .log().all()
                        .assertThat().statusCode(201)
                        .assertThat().contentType("application/json")
                        .extract()
                        .jsonPath()
                        .getObject(".", PetEntity.class);

        PetStoreReader psReader = new PetStoreReader();
        List<PetEntity> actualResults = psReader.readJsonFromFile();

        List<DynamicTest> testResults = Arrays.asList(
                DynamicTest.dynamicTest("Pet list increased by one",
                        () -> assertEquals(expectedResults.size() + 1, actualResults.size())),
                DynamicTest.dynamicTest("Pet list contains the new pet",
                        () -> assertTrue(actualResults.contains(addedPet)))
        );

        return testResults.stream();
    }

    @TestFactory
    @DisplayName("Add Pet Entity with Missing Fields")
    public Stream<DynamicTest> addInvalidPetEntityTest() {
        RestAssured.registerParser("application/json", Parser.JSON);

        // Sending empty object to simulate bad request
        BadRequestResponseBody body =
                given()
                        .headers(headers)
                        .body("{}")
                        .when()
                        .post("inventory/add")
                        .then()
                        .log().all()
                        .assertThat().statusCode(400)
                        .extract()
                        .jsonPath().getObject(".", BadRequestResponseBody.class);

        return body.executeTests(
                "Bad Request",
                "Required fields are missing or invalid in request body.",
                "/inventory/add",
                400
        ).stream();
    }
}
