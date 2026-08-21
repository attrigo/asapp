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

package com.attrigo.asapp.users.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Tests that dependencies between the hexagonal layers point inward only.
 * <p>
 * Coverage:
 * <li>Rejects domain classes depending on the application or infrastructure layer</li>
 * <li>Rejects application classes depending on the infrastructure layer</li>
 * <li>Rejects the bootstrap class depending on the domain or application layer</li>
 * <li>Rejects any layer depending on the bootstrap class</li>
 * <li>Rejects classes placed outside the declared layers</li>
 */
@AnalyzeClasses(packages = "com.attrigo.asapp.users", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTests {

    @ArchTest
    static final ArchRule layersRespectDependencyDirection =
    // @formatter:off
            layeredArchitecture().consideringOnlyDependenciesInLayers()
                                 .layer("Bootstrap").definedBy("com.attrigo.asapp.users")
                                 .layer("Infrastructure").definedBy("..infrastructure..")
                                 .layer("Application").definedBy("..application..")
                                 .layer("Domain").definedBy("..domain..")
                                 .whereLayer("Bootstrap").mayNotBeAccessedByAnyLayer()
                                 .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Bootstrap")
                                 .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                                 .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                                 .ensureAllClassesAreContainedInArchitecture();
    // @formatter:on

}
