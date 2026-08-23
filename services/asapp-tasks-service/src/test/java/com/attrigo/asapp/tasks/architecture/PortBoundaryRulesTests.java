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

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Tests that ports are interfaces, are implemented only where the architecture allows, and are reached only through the declared boundary.
 * <p>
 * Coverage:
 * <li>Rejects an input or output port declared as anything but an interface</li>
 * <li>Rejects an input port implemented outside the application service package</li>
 * <li>Rejects an application service implementing no input port</li>
 * <li>Rejects an output port implemented outside a driven adapter package</li>
 * <li>Rejects an input port referenced outside the application layer and the driving adapters</li>
 * <li>Rejects an application service referenced beyond the application layer</li>
 * <li>Rejects a class outside the application layer referencing an output port it does not implement</li>
 */
@AnalyzeClasses(packages = "com.attrigo.asapp.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class PortBoundaryRulesTests {

    private static final DescribedPredicate<JavaClass> APPLICATION_LAYER = resideInAPackage("..application..");

    @ArchTest
    static final ArchRule portsAreInterfaces =
    // @formatter:off
            classes().that()
                     .resideInAnyPackage("..application..in", "..application..out")
                     .should()
                     .beInterfaces();
    // @formatter:on

    @ArchTest
    static final ArchRule inputPortsAreImplementedOnlyByApplicationServices =
    // @formatter:off
            classes().that()
                     .implement(resideInAPackage("..application..in"))
                     .should()
                     .resideInAPackage("..application..in.service..");
    // @formatter:on

    @ArchTest
    static final ArchRule applicationServicesImplementInputPorts =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..application..in.service..")
                     .should()
                     .implement(resideInAPackage("..application..in"));
    // @formatter:on

    @ArchTest
    static final ArchRule outputPortsAreImplementedOnlyInDrivenAdapterPackages =
    // @formatter:off
            classes().that()
                     .implement(resideInAPackage("..application..out"))
                     .should()
                     .resideInAPackage("..infrastructure..out");
    // @formatter:on

    @ArchTest
    static final ArchRule inputPortsAreReferencedOnlyByTheApplicationOrDrivingAdapters =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..application..in")
                     .should()
                     .onlyHaveDependentClassesThat()
                     .resideInAnyPackage("..application..", "..infrastructure..in..");
    // @formatter:on

    @ArchTest
    static final ArchRule applicationServicesAreReferencedOnlyByTheApplication =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..application..in.service..")
                     .should()
                     .onlyHaveDependentClassesThat()
                     .resideInAPackage("..application..");
    // @formatter:on

    @ArchTest
    static final ArchRule outputPortsAreReferencedOnlyByTheApplicationOrTheirImplementations =
    // @formatter:off
            classes().that()
                     .resideInAPackage("..application..out")
                     .should(beReferencedOnlyByTheApplicationOrTheirImplementations());
    // @formatter:on

    private static ArchCondition<JavaClass> beReferencedOnlyByTheApplicationOrTheirImplementations() {
        return new ArchCondition<>("be referenced only by the application layer or their implementations") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getDirectDependenciesToSelf()
                         .stream()
                         .filter(dependency -> isNotInApplicationLayerNorImplementationOf(dependency.getOriginClass(), javaClass))
                         .forEach(dependency -> events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }

        };
    }

    private static boolean isNotInApplicationLayerNorImplementationOf(JavaClass origin, JavaClass javaClass) {
        return !APPLICATION_LAYER.test(origin) && !origin.isAssignableTo(javaClass.getName());
    }

}
