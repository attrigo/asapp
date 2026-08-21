/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.attrigo.asapp.tasks.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Tests that each hexagonal layer depends only on what its position allows, and is depended on only by the layers outside it.
 * <p>
 * Coverage:
 * <li>Rejects domain classes depending on anything but the JDK and other domain classes</li>
 * <li>Rejects application classes depending on anything but the domain, the JDK, the logging facade and the transaction annotations</li>
 * <li>Rejects the bootstrap class depending on the domain or application layer</li>
 * <li>Rejects a class outside the application and infrastructure layers depending on the domain</li>
 * <li>Rejects a class outside the infrastructure layer depending on the application</li>
 * <li>Rejects a class outside the bootstrap depending on the infrastructure</li>
 * <li>Rejects any layer depending on the bootstrap class</li>
 */
@AnalyzeClasses(packages = "com.attrigo.asapp.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyRulesTests {

    @ArchTest
    static final ArchRule domainDependsOnlyOnTheJdk =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..domain..")
                     .should()
                     .onlyDependOnClassesThat()
                     .resideInAnyPackage("..domain..", "java..");
    // @formatter:on

    @ArchTest
    static final ArchRule applicationDependsOnlyOnTheDomainJdkLoggingAndTransactions =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..application..")
                     .should()
                     .onlyDependOnClassesThat()
                     .resideInAnyPackage("..domain..", "..application..", "java..", "org.slf4j..", "org.springframework.transaction.annotation..");
    // @formatter:on

    @ArchTest
    static final ArchRule bootstrapDependsOnNeitherTheDomainNorTheApplication =
    // @formatter:off
            noClasses().that()
                       .resideInAPackage("com.attrigo.asapp.tasks")
                       .should()
                       .dependOnClassesThat()
                       .resideInAnyPackage("..domain..", "..application..");
    // @formatter:on

    @ArchTest
    static final ArchRule domainIsDependedOnOnlyByTheApplicationAndTheInfrastructure =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..domain..")
                     .should()
                     .onlyHaveDependentClassesThat()
                     .resideInAnyPackage("..domain..", "..application..", "..infrastructure..");
    // @formatter:on

    @ArchTest
    static final ArchRule applicationIsDependedOnOnlyByTheInfrastructure =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..application..")
                     .should()
                     .onlyHaveDependentClassesThat()
                     .resideInAnyPackage("..application..", "..infrastructure..");
    // @formatter:on

    @ArchTest
    static final ArchRule infrastructureIsDependedOnOnlyByTheBootstrap =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..infrastructure..")
                     .should()
                     .onlyHaveDependentClassesThat()
                     .resideInAnyPackage("..infrastructure..", "com.attrigo.asapp.tasks");
    // @formatter:on

    @ArchTest
    static final ArchRule bootstrapIsNotDependedOnByAnyLayer =
    // @formatter:off
            noClasses().that()
                       .resideInAnyPackage("..domain..", "..application..", "..infrastructure..")
                       .should()
                       .dependOnClassesThat()
                       .resideInAPackage("com.attrigo.asapp.tasks");
    // @formatter:on

}
