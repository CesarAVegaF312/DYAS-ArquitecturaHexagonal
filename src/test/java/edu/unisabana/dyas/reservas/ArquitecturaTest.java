package edu.unisabana.dyas.reservas;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Reglas de la arquitectura hexagonal escritas como pruebas (funciones de aptitud).
 * Corren con ./mvnw test: si alguien rompe una frontera, la prueba falla y dice cuál.
 *
 * NO DEBILITE ESTAS REGLAS para que el código pase: corrija el código.
 */
@AnalyzeClasses(packages = "edu.unisabana.dyas.reservas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** Regla 1: el dominio no conoce las capas externas. */
    @ArchTest
    static final ArchRule dominioNoDependeDeCapasExternas = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..aplicacion..", "..adaptadores..", "..configuracion..")
            .because("el dominio es el centro del hexágono y no debe cambiar cuando cambian las capas externas");

    /** Regla 2: el dominio es Java puro, sin frameworks. */
    @ArchTest
    static final ArchRule dominioNoDependeDeFrameworks = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "java.sql..", "javax.sql..",
                    "tools.jackson..", "com.fasterxml.jackson..")
            .because("las reglas de negocio deben poder probarse y reutilizarse sin Spring ni base de datos");

    /** Regla 3: la aplicación solo usa el dominio, sus propios puertos y Java estándar (sin JDBC). */
    @ArchTest
    static final ArchRule aplicacionSoloDependeDelDominioYDeSusPuertos = classes()
            .that().resideInAPackage("..aplicacion..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..dominio..", "..aplicacion..", "java.lang..", "java.util..", "java.time..")
            .because("los casos de uso hablan con el exterior solo a través de puertos");

    /**
     * Regla 4: los adaptadores dependen hacia adentro y no se conocen entre sí.
     * El paquete configuracion es la raíz de composición, por eso se le permite conocer todas las capas.
     */
    @ArchTest
    static final ArchRule adaptadoresAisladosEntreSi = onionArchitecture()
            .domainModels("..dominio..")
            .applicationServices("..aplicacion..")
            .adapter("web", "..adaptadores.entrada.web..")
            .adapter("memoria", "..adaptadores.salida.memoria..")
            .adapter("postgres", "..adaptadores.salida.postgres..")
            .adapter("notificaciones", "..adaptadores.salida.notificaciones..")
            .withOptionalLayers(true)
            .ignoreDependency(resideInAPackage("..configuracion.."), alwaysTrue());
}
