package com.alec.aitraining;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@Tag("arch")
@AnalyzeClasses(packages = "com.alec.aitraining", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule layerDependencies = layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Web").definedBy("com.alec.aitraining.web..")
		.layer("Service").definedBy("com.alec.aitraining.service..")
		.layer("Repository").definedBy("com.alec.aitraining.repository..")
		.layer("Domain").definedBy("com.alec.aitraining.domain..")
		.layer("DTO").definedBy("com.alec.aitraining.dto..")
		.whereLayer("Web").mayOnlyAccessLayers("Service", "DTO", "Domain")
		.whereLayer("Service").mayOnlyAccessLayers("Repository", "Domain", "DTO")
		.whereLayer("Repository").mayOnlyAccessLayers("Domain");

	@ArchTest
	static final ArchRule noFieldInjection = noFields()
		.should().beAnnotatedWith(Autowired.class)
		.because("use constructor injection via @RequiredArgsConstructor");

	@ArchTest
	static final ArchRule domainMustBeSpringFree = noClasses()
		.that().resideInAPackage("..domain..")
		.should().dependOnClassesThat()
		.resideInAPackage("org.springframework..")
		.because("domain model must not depend on Spring framework");
}
