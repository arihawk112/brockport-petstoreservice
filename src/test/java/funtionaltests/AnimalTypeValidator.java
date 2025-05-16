package funtionaltests;

import com.petstore.PetEntity;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validator class focused on comparing AnimalType values
 */
public abstract class AnimalTypeValidator {

    /**
     * Validates that each actual PetEntity has the same AnimalType as expected.
     *
     * @param expectedResults list of expected pets
     * @param actualResults   list of actual pets returned from the endpoint
     * @return a container of dynamic tests for AnimalType validation
     */
    public static DynamicContainer addAnimalTypeBodyTests(List<PetEntity> expectedResults, List<PetEntity> actualResults) {
        List<DynamicNode> testNodes = new ArrayList<>();

        for (PetEntity expected : expectedResults) {
            int petId = expected.getPetId();

            PetEntity actual = actualResults.stream()
                    .filter(p -> p.getPetId() == petId)
                    .findFirst()
                    .orElse(null);

            if (actual != null) {
                testNodes.add(DynamicTest.dynamicTest("AnimalType match for PetId[" + petId + "]",
                        () -> assertEquals(expected.getAnimalType(), actual.getAnimalType())));
            } else {
                testNodes.add(DynamicTest.dynamicTest("Missing petId[" + petId + "] in actual results",
                        () -> assertEquals("Pet missing", "Found")));
            }
        }

        return DynamicContainer.dynamicContainer("AnimalType Validation", testNodes);
    }
}
