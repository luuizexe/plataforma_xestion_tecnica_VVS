package gal.udc.fic.prperez.pleste.service.integration;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import gal.udc.fic.prperez.pleste.service.exceptions.RESTException;
import jakarta.persistence.Entity;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@DisplayName("Server Architecture Structural Integration Tests (ArchUnit)")
public class ServerArchitectureIT {

	private static JavaClasses importedClasses;

	@BeforeAll
	static void setUp() {
		importedClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("gal.udc.fic.prperez.pleste");
	}

	@Test
	@DisplayName("DAOs and entities must not depend on REST Resources")
	void testDaosMustNotDependOnResources() {
		noClasses().that().resideInAPackage("..dao..")
				.should().dependOnClassesThat().haveSimpleNameEndingWith("Resource")
				.as("Data access layer classes must never depend on REST Resource controllers")
				.check(importedClasses);
	}

	@Test
	@DisplayName("All custom business exceptions must inherit from RESTException")
	void testCustomExceptionsMustInheritFromRestException() {
		classes().that().resideInAPackage("..exceptions..")
				.and().haveSimpleNameEndingWith("Exception")
				.should().beAssignableTo(RESTException.class)
				.as("All business domain exceptions must inherit from RESTException")
				.check(importedClasses);
	}

	@Test
	@DisplayName("All Resource classes must be annotated with JAX-RS @Path")
	void testResourcesMustBeAnnotatedWithPath() {
		classes().that().haveSimpleNameEndingWith("Resource")
				.should().beAnnotatedWith(Path.class)
				.as("Classes ending with 'Resource' must have JAX-RS @Path annotation")
				.check(importedClasses);
	}

	@Test
	@DisplayName("All JpaRepositories must be interfaces ending in Dao residing in dao package")
	void testJpaRepositoriesNamingAndLocation() {
		classes().that().areAssignableTo(JpaRepository.class)
				.should().beInterfaces()
				.andShould().haveSimpleNameEndingWith("Dao")
				.andShould().resideInAPackage("..dao..")
				.as("All Spring Data JPA repositories must be interfaces named *Dao in a dao package")
				.check(importedClasses);
	}

	@Test
	@DisplayName("All JPA @Entity classes must reside in dao subpackages")
	void testJpaEntitiesLocation() {
		classes().that().areAnnotatedWith(Entity.class)
				.should().resideInAPackage("..dao..")
				.as("All JPA entities must be located in dao packages")
				.check(importedClasses);
	}

	@Test
	@DisplayName("Resource fields must not be public to preserve encapsulation")
	void testResourceFieldsEncapsulation() {
		fields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Resource")
				.should().notBePublic()
				.as("Resource fields must be private or protected, never public")
				.check(importedClasses);
	}

}
