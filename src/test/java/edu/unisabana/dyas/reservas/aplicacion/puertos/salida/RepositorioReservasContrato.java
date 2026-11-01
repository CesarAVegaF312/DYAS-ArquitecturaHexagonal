package edu.unisabana.dyas.reservas.aplicacion.puertos.salida;

import edu.unisabana.dyas.reservas.dominio.EstadoReserva;
import edu.unisabana.dyas.reservas.dominio.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de contrato del puerto {@link RepositorioReservas}: lo que cualquier adaptador de persistencia
 * debe cumplir para que los casos de uso funcionen igual con él.
 *
 * <p>Cada adaptador tiene una subclase que solo dice cómo crear el adaptador
 * (ver {@code RepositorioReservasEnMemoriaTest}). Las pruebas son las mismas para todos.
 */
public abstract class RepositorioReservasContrato {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 6);
    private static final LocalDate MANANA = HOY.plusDays(1);

    private RepositorioReservas repositorio;

    /** Crea el adaptador que se va a probar, vacío. */
    protected abstract RepositorioReservas crearRepositorio();

    @BeforeEach
    void prepararRepositorio() {
        repositorio = crearRepositorio();
    }

    private static Reserva reserva(String sala, LocalDate fecha, String inicio, String fin) {
        return Reserva.nueva(sala, "ana.perez@unisabana.edu.co", fecha, LocalTime.parse(inicio),
                LocalTime.parse(fin), HOY);
    }

    @Test
    void guardaYRecuperaUnaReservaConTodosSusDatos() {
        Reserva original = reserva("B-201", MANANA, "10:00", "12:00");

        repositorio.guardar(original);

        assertThat(repositorio.buscarPorId(original.getId())).get().satisfies(r -> {
            assertThat(r.getId()).isEqualTo(original.getId());
            assertThat(r.getSala()).isEqualTo("B-201");
            assertThat(r.getResponsable()).isEqualTo("ana.perez@unisabana.edu.co");
            assertThat(r.getFecha()).isEqualTo(MANANA);
            assertThat(r.getInicio()).isEqualTo(LocalTime.of(10, 0));
            assertThat(r.getFin()).isEqualTo(LocalTime.of(12, 0));
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
        });
    }

    @Test
    void noEncuentraUnaReservaInexistente() {
        assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void guardarDeNuevoActualizaLaReservaSinDuplicarla() {
        Reserva reserva = reserva("B-201", MANANA, "10:00", "12:00");
        repositorio.guardar(reserva);

        reserva.cancelar();
        repositorio.guardar(reserva);

        assertThat(repositorio.buscarPorId(reserva.getId())).get()
                .extracting(Reserva::getEstado).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(repositorio.buscarPorFecha(MANANA)).hasSize(1);
    }

    @Test
    void buscaPorSalaYFechaIncluyendoLasCanceladas() {
        Reserva activa = reserva("B-201", MANANA, "10:00", "11:00");
        Reserva cancelada = reserva("B-201", MANANA, "12:00", "13:00");
        cancelada.cancelar();
        repositorio.guardar(activa);
        repositorio.guardar(cancelada);
        repositorio.guardar(reserva("B-202", MANANA, "10:00", "11:00"));
        repositorio.guardar(reserva("B-201", MANANA.plusDays(1), "10:00", "11:00"));

        assertThat(repositorio.buscarPorSalaYFecha("B-201", MANANA))
                .extracting(Reserva::getId)
                .containsExactlyInAnyOrder(activa.getId(), cancelada.getId());
    }

    @Test
    void buscaPorFechaOrdenadoPorSalaYHoraDeInicio() {
        Reserva c101Tarde = reserva("C-101", MANANA, "15:00", "16:00");
        Reserva b201Tarde = reserva("B-201", MANANA, "14:00", "15:00");
        Reserva b201Manana = reserva("B-201", MANANA, "08:00", "09:00");
        repositorio.guardar(c101Tarde);
        repositorio.guardar(b201Tarde);
        repositorio.guardar(b201Manana);
        repositorio.guardar(reserva("B-201", HOY, "10:00", "11:00"));

        assertThat(repositorio.buscarPorFecha(MANANA))
                .extracting(Reserva::getId)
                .containsExactly(b201Manana.getId(), b201Tarde.getId(), c101Tarde.getId());
    }
}
